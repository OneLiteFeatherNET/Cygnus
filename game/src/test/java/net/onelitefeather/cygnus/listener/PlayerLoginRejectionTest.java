package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.DisconnectPacket;
import net.minestom.server.network.packet.server.common.ResourcePackPushPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.TeamService;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.listener.game.SlenderTakeover;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.resourcepack.ResourcePackService;
import net.onelitefeather.cygnus.spectator.LateJoinSlots;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers how {@link PlayerLoginListener} turns players away while they are still configuring.
 *
 * <p>Behind a proxy a disconnect during configuration makes the proxy move the player to the
 * fallback server inside the same configuration phase, which crashes the client with a duplicate
 * registry key. A rejected player therefore has to stay connected until the play state and be
 * kicked there.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.2
 */
@ExtendWith(MicrotusExtension.class)
class PlayerLoginRejectionTest {

    private static final GameConfig.Round ROUND = new GameConfig.Round(2, 10, 30, 600, 8);

    private ResourcePackService service;
    private RecordingConnection connection;
    private CygnusPlayer player;
    private Instance instance;

    @BeforeEach
    void setUp(@NotNull Env env) {
        this.service = ResourcePackService.create(
                new GameConfig.ResourcePack(URI.create("https://example.com/pack.zip"), "a".repeat(40))).orElseThrow();
        this.instance = env.createFlatInstance();
        this.connection = new RecordingConnection();
        this.connection.setServerState(ConnectionState.CONFIGURATION);
        this.player = new CygnusPlayer(connection, new GameProfile(UUID.randomUUID(), "Configuring"), service.packId());
    }

    @Test
    void testFullServerDoesNotDisconnectDuringConfiguration() {
        fullServerListener().accept(new AsyncPlayerConfigurationEvent(player, true));

        assertTrue(connection.disconnects().isEmpty(),
                "a disconnect during configuration makes the proxy break the client");
        assertTrue(connection.isOnline(), "the player has to stay connected until the play state");
    }

    @Test
    void testFullServerKicksOnceWithItsMessageInPlay() {
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        fullServerListener().accept(event);
        connection.setServerState(ConnectionState.PLAY);

        player.setInstance(event.getSpawningInstance());

        assertEquals(1, connection.disconnects().size(), "the player has to be kicked exactly once");
        assertEquals(plain(Messages.SERVER_FULL), plain(connection.disconnects().getFirst().message()));
        assertTrue(player.isRejectedBeforeJoin(), "the rejected player never took part in the game");
    }

    @Test
    void testStartedGameDoesNotDisconnectDuringConfiguration() {
        startedGameListener().accept(new AsyncPlayerConfigurationEvent(player, true));

        assertTrue(connection.disconnects().isEmpty(),
                "a disconnect during configuration makes the proxy break the client");
        assertTrue(connection.isOnline(), "the player has to stay connected until the play state");
    }

    @Test
    void testStartedGameKicksOnceWithItsMessageInPlay() {
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        startedGameListener().accept(event);
        connection.setServerState(ConnectionState.PLAY);

        player.setInstance(event.getSpawningInstance());

        assertEquals(1, connection.disconnects().size(), "the player has to be kicked exactly once");
        assertEquals(plain(Messages.GAME_ALREADY_STARTED), plain(connection.disconnects().getFirst().message()));
        assertTrue(player.isRejectedBeforeJoin(), "the rejected player never took part in the game");
    }

    @Test
    void testFullLatePlacesDoNotDisconnectDuringConfiguration() {
        lateJoinListener(fullLateJoinSlots(UUID.randomUUID())).accept(new AsyncPlayerConfigurationEvent(player, true));

        assertTrue(connection.disconnects().isEmpty(),
                "a disconnect during configuration makes the proxy break the client");
        assertTrue(connection.isOnline(), "the player has to stay connected until the play state");
    }

    @Test
    void testFullLatePlacesKickOnceWithSpectatorPlacesFullMessageInPlay() {
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        lateJoinListener(fullLateJoinSlots(UUID.randomUUID())).accept(event);
        connection.setServerState(ConnectionState.PLAY);

        player.setInstance(event.getSpawningInstance());

        assertEquals(1, connection.disconnects().size(), "the player has to be kicked exactly once");
        assertEquals(plain(Messages.SPECTATOR_PLACES_FULL), plain(connection.disconnects().getFirst().message()));
        assertTrue(player.isRejectedBeforeJoin(), "the refused late joiner never took part in the game");
    }

    @Test
    void testRefusedLateJoinerDoesNotHoldAPlace() {
        LateJoinSlots slots = fullLateJoinSlots(UUID.randomUUID());
        lateJoinListener(slots).accept(new AsyncPlayerConfigurationEvent(player, true));

        assertFalse(slots.holds(player.getUuid()), "a refused joiner must not take a place");
    }

    @Test
    void testRefusedLateJoinerDisconnectReleasesNothingAndRunsNoRoundLogic() {
        UUID holder = UUID.randomUUID();
        LateJoinSlots slots = fullLateJoinSlots(holder);
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        lateJoinListener(slots).accept(event);
        connection.setServerState(ConnectionState.PLAY);
        player.setInstance(event.getSpawningInstance());

        AtomicBoolean phaseRead = new AtomicBoolean(false);
        quitListener(slots, () -> {
            phaseRead.set(true);
            return new LobbyPhase(ROUND);
        }).accept(new PlayerDisconnectEvent(player));

        assertFalse(phaseRead.get(), "a refused joiner disconnecting must not run any round logic");
        assertFalse(slots.holds(player.getUuid()), "the refused joiner must hold no place after the disconnect");
        assertTrue(slots.holds(holder), "a refused joiner must not free somebody else's place");
    }

    @Test
    void testRejectedPlayerGetsNoResourcePackRequest() {
        fullServerListener().accept(new AsyncPlayerConfigurationEvent(player, true));

        assertTrue(connection.packets().stream().noneMatch(ResourcePackPushPacket.class::isInstance),
                "a player that is turned away has no use for the pack");
        assertNull(player.getResourcePackFuture(), "no pack download may be tracked for a rejected player");
    }

    @Test
    void testRejectedPlayerStillGetsASpawningInstance() {
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        fullServerListener().accept(event);

        assertNotNull(event.getSpawningInstance(),
                "without one Minestom aborts the configuration and the deferred kick is never delivered");
    }

    @Test
    void testAcceptedPlayerIsNotKickedAndGetsTheResourcePack() {
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        new PlayerLoginListener(() -> instance, 10, () -> new LobbyPhase(ROUND), Optional.of(service), new LateJoinSlots(0)).accept(event);
        connection.setServerState(ConnectionState.PLAY);

        assertSame(instance, event.getSpawningInstance(), "a normal login joins the game instance");
        assertTrue(connection.packets().stream().anyMatch(ResourcePackPushPacket.class::isInstance),
                "a normal login receives the pack request");
        assertTrue(connection.disconnects().isEmpty(), "a normal login is not kicked");
        assertFalse(player.isRejectedBeforeJoin(), "a normal login is not marked as rejected");
    }

    private PlayerLoginListener fullServerListener() {
        return new PlayerLoginListener(() -> instance, 0, () -> new LobbyPhase(ROUND), Optional.of(service), new LateJoinSlots(0));
    }

    private PlayerLoginListener startedGameListener() {
        // Late joining is off, so any phase that is not the lobby counts as a started game.
        return new PlayerLoginListener(() -> instance, 10, () -> null, Optional.of(service), new LateJoinSlots(0));
    }

    private PlayerLoginListener lateJoinListener(LateJoinSlots slots) {
        return new PlayerLoginListener(() -> instance, 10, () -> new GamePhase(new GameViewImpl(), 600,
                new JumpScareManager(TickSections.NONE), (_, _) -> 0.0D), Optional.of(service), slots);
    }

    /** A limit of one place, already taken by {@code holder}, so the joiner under test is refused. */
    private static LateJoinSlots fullLateJoinSlots(UUID holder) {
        LateJoinSlots slots = new LateJoinSlots(1);
        slots.tryReserve(holder);
        return slots;
    }

    private PlayerQuitListener quitListener(LateJoinSlots slots, Supplier<Phase> phase) {
        TeamService teamService = TeamService.of();
        return new PlayerQuitListener(phase, teamService, new StaminaService(), () -> {},
                new SlenderTakeover(teamService, phase, TickSections.NONE), slots);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** A connection that records what was sent and lets the test pick the server state. */
    private static final class RecordingConnection extends PlayerConnection {

        private final List<ServerPacket> packets = new ArrayList<>();

        @Override
        public void sendPacket(SendablePacket packet) {
            if (packet instanceof ServerPacket serverPacket) {
                packets.add(serverPacket);
            }
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return new InetSocketAddress("localhost", 25565);
        }

        List<ServerPacket> packets() {
            return packets;
        }

        List<DisconnectPacket> disconnects() {
            return packets.stream().filter(DisconnectPacket.class::isInstance).map(DisconnectPacket.class::cast).toList();
        }
    }
}
