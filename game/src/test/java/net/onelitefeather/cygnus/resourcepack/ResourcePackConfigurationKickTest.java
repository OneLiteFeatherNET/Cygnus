package net.onelitefeather.cygnus.resourcepack;

import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.listener.common.ResourcePackListener;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.client.common.ClientResourcePackStatusPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.DisconnectPacket;
import net.minestom.server.network.packet.server.common.ResourcePackPopPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.listener.PlayerQuitListener;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the kick of a player whose ResourcePack fails while the player is still configuring.
 *
 * <p>Behind a proxy a disconnect during configuration makes the proxy move the player to the
 * fallback server inside the same configuration phase, which crashes the client with a duplicate
 * registry key. The kick therefore has to wait for the play state.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.2
 */
@ExtendWith(MicrotusExtension.class)
class ResourcePackConfigurationKickTest {

    private ResourcePackService service;
    private RecordingConnection connection;
    private CygnusPlayer player;
    private Instance instance;

    @BeforeEach
    void setUp(@NotNull Env env) {
        this.service = ResourcePackService.create(
                new GameConfig.ResourcePack(URI.create("https://example.com/pack.zip"), "a".repeat(40))).orElseThrow();
        env.process().eventHandler().addChild(service.node());
        this.instance = env.createFlatInstance();
        this.connection = new RecordingConnection();
        this.connection.setServerState(ConnectionState.CONFIGURATION);
        this.player = new CygnusPlayer(connection, new GameProfile(UUID.randomUUID(), "Configuring"), service.packId());
        this.service.sendTo(player);
    }

    @Test
    void testFailedPackDoesNotDisconnectDuringConfiguration() {
        report(ResourcePackStatus.FAILED_RELOAD);

        assertTrue(connection.disconnects().isEmpty(),
                "a disconnect during configuration makes the proxy break the client");
        assertTrue(connection.isOnline(), "the player has to stay connected until the play state");
    }

    @Test
    void testFailedPackKicksOnceWithTheVersionMessageInPlay() {
        report(ResourcePackStatus.FAILED_RELOAD);
        connection.setServerState(ConnectionState.PLAY);

        player.setInstance(instance);

        assertEquals(1, connection.disconnects().size(), "the player has to be kicked exactly once");
        String message = PlainTextComponentSerializer.plainText().serialize(connection.disconnects().getFirst().message());
        assertTrue(message.contains("could not be loaded") && message.contains("version"),
                "the message has to name the failed pack and the possibly unsupported version, was: " + message);
        assertFalse(connection.isOnline(), "the connection has to be closed after the kick");
    }

    @Test
    void testDeclinedPackKeepsTheAcceptMessage() {
        report(ResourcePackStatus.DECLINED);
        connection.setServerState(ConnectionState.PLAY);

        player.setInstance(instance);

        assertEquals(1, connection.disconnects().size(), "the player has to be kicked exactly once");
        String message = PlainTextComponentSerializer.plainText().serialize(connection.disconnects().getFirst().message());
        assertEquals("You must accept the ResourcePack to play on this server!", message);
    }

    @Test
    void testDeferredKickDoesNotSpawnThePlayer(@NotNull Env env) {
        AtomicInteger spawns = new AtomicInteger();
        EventNode<Event> node = EventNode.all("spawn-count");
        node.addListener(PlayerSpawnEvent.class, _ -> spawns.incrementAndGet());
        env.process().eventHandler().addChild(node);
        report(ResourcePackStatus.FAILED_RELOAD);
        connection.setServerState(ConnectionState.PLAY);

        player.setInstance(instance);

        assertEquals(0, spawns.get(), "no spawn listener may see a player that is about to be kicked");
        assertTrue(player.isRejectedBeforeJoin(), "the player has to be marked as never joined");
        assertNull(player.getInstance(), "the player must not enter the instance");
        env.process().eventHandler().removeChild(node);
    }

    @Test
    void testDeferredKickRemovesThePlayerOnce(@NotNull Env env) {
        AtomicInteger disconnects = new AtomicInteger();
        EventNode<Event> node = EventNode.all("disconnect-count");
        node.addListener(PlayerDisconnectEvent.class, _ -> disconnects.incrementAndGet());
        env.process().eventHandler().addChild(node);
        report(ResourcePackStatus.FAILED_RELOAD);
        connection.setServerState(ConnectionState.PLAY);

        player.setInstance(instance);

        assertEquals(1, disconnects.get(), "nothing ticks a player outside an instance, so it has to be removed at once");
        env.process().eventHandler().removeChild(node);
    }

    @Test
    void testRejectedPlayerLeavingIsNeitherAnnouncedNorCounted() {
        report(ResourcePackStatus.FAILED_RELOAD);
        connection.setServerState(ConnectionState.PLAY);
        player.setInstance(instance);
        PlayerQuitListener listener = new PlayerQuitListener(
                () -> {
                    throw new AssertionError("the phase must not be consulted for a player that never joined");
                },
                TeamService.of(), new StaminaService(), () -> { }, null);

        listener.accept(new PlayerDisconnectEvent(player));
    }

    @Test
    void testFailedPackInPlayStillPopsBeforeDisconnecting() {
        connection.setServerState(ConnectionState.PLAY);

        report(ResourcePackStatus.FAILED_RELOAD);

        int pop = indexOf(ResourcePackPopPacket.class);
        int disconnect = indexOf(DisconnectPacket.class);
        assertTrue(pop >= 0, "a failure in play keeps the pop of the pack");
        assertTrue(disconnect > pop, "the pop has to be sent before the disconnect");
        assertEquals(1, connection.disconnects().size(), "the player has to be kicked exactly once");
    }

    private int indexOf(Class<? extends ServerPacket> type) {
        List<ServerPacket> sent = connection.packets();
        for (int i = 0; i < sent.size(); i++) {
            if (type.isInstance(sent.get(i))) return i;
        }
        return -1;
    }

    /** Delivers a status the way the client's packet does: the event first, then the player. */
    private void report(ResourcePackStatus status) {
        ResourcePackListener.listener(new ClientResourcePackStatusPacket(service.packId(), status), player);
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
