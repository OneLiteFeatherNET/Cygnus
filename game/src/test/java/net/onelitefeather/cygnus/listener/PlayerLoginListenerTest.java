package net.onelitefeather.cygnus.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.RestartPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.resourcepack.ResourcePackService;
import net.onelitefeather.cygnus.spectator.LateJoinSlots;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.onelitefeather.cygnus.camera.WakeUpTransition;
import net.onelitefeather.cygnus.telemetry.TickSections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLoginListenerTest extends CygnusPlayerTestBase {

    private static GameConfig.Round lobbyConfig() {
        return new GameConfig.Round(2, 10, 30, 600, 8);
    }

    private static GamePhase gamePhase() {
        return new GamePhase(new GameViewImpl(), 600, new JumpScareManager(TickSections.NONE), (_, _) -> 0.0D);
    }

    private Instance instance;
    private Player player;

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
        this.player = env.createPlayer(this.instance);
    }

    @AfterEach
    void tearDown(Env env) {
        env.destroyInstance(this.instance, true);
    }

    @Test
    void testKicksWhenServerIsFull() {
        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 0, () -> new LobbyPhase(lobbyConfig()), Optional.empty(), new LateJoinSlots(0));
        listener.accept(new AsyncPlayerConfigurationEvent(player, true));

        assertFalse(player.isOnline());
    }

    @Test
    void testKicksWhenPhaseIsNotLobbyAndLateJoinIsOff() {
        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 10, () -> gamePhase(), Optional.empty(), new LateJoinSlots(0));
        listener.accept(new AsyncPlayerConfigurationEvent(player, true));

        assertFalse(player.isOnline());
    }

    @Test
    void testLateJoinerDuringGamePhaseIsAdmittedWhenPlaceIsFree() {
        LateJoinSlots slots = new LateJoinSlots(1);
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        new PlayerLoginListener(() -> instance, 10, () -> gamePhase(), Optional.empty(), slots).accept(event);

        assertSame(instance, event.getSpawningInstance(), "a late joiner must be sent into the running map");
        assertTrue(slots.holds(player.getUuid()), "the admitted late joiner must hold the free place");
    }

    @Test
    void testLateJoinerDuringWaitingPhaseIsAdmittedWhenPlaceIsFree() {
        LateJoinSlots slots = new LateJoinSlots(1);
        WaitingPhase waitingPhase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> {}, () -> instance, new WakeUpTransition());
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        new PlayerLoginListener(() -> instance, 10, () -> waitingPhase, Optional.empty(), slots).accept(event);

        assertSame(instance, event.getSpawningInstance(), "a late joiner must be sent into the running map");
        assertTrue(slots.holds(player.getUuid()), "the admitted late joiner must hold the free place");
    }

    @Test
    void testLateJoinerAfterRoundIsAdmittedWhenPlaceIsFree() {
        LateJoinSlots slots = new LateJoinSlots(1);
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        new PlayerLoginListener(() -> instance, 10, RestartPhase::new, Optional.empty(), slots).accept(event);

        assertSame(instance, event.getSpawningInstance(), "a joiner after the round must be sent into the map");
        assertTrue(slots.holds(player.getUuid()), "the admitted joiner must hold the free place");
    }

    @Test
    void testLateJoinerGetsTheResourcePack() {
        GameConfig.ResourcePack packConfig = new GameConfig.ResourcePack(URI.create("https://example.com/pack.zip"), "a".repeat(40));
        Optional<ResourcePackService> resourcePackService = ResourcePackService.create(packConfig);
        assertTrue(resourcePackService.isPresent());

        new PlayerLoginListener(() -> instance, 10, () -> gamePhase(), resourcePackService, new LateJoinSlots(1))
                .accept(new AsyncPlayerConfigurationEvent(player, true));

        assertNotNull(player.getResourcePackFuture(), "a late joiner must receive the resource pack like any other player");
    }

    @Test
    void testLateJoinerAboveMaxPlayersIsAdmittedDuringRunningRound() {
        LateJoinSlots slots = new LateJoinSlots(1);
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        // The player is already online, so the server counts one more than maxPlayers allows.
        new PlayerLoginListener(() -> instance, 1, () -> gamePhase(), Optional.empty(), slots).accept(event);

        assertSame(instance, event.getSpawningInstance(), "the lobby limit must not refuse a late joiner");
        assertTrue(player.isOnline(), "a late joiner above maxPlayers must not be kicked as a full server");
        assertTrue(slots.holds(player.getUuid()), "the admitted late joiner must hold the free place");
    }

    @Test
    void testLobbyJoinAboveMaxPlayersIsStillKicked() {
        LateJoinSlots slots = new LateJoinSlots(1);
        new PlayerLoginListener(() -> instance, 1, () -> new LobbyPhase(lobbyConfig()), Optional.empty(), slots)
                .accept(new AsyncPlayerConfigurationEvent(player, true));

        assertFalse(player.isOnline(), "a lobby join above maxPlayers must still be refused as a full server");
        assertFalse(slots.holds(player.getUuid()), "a lobby join must never take a late place");
    }

    @Test
    void testSuccessfulLoginSetsSpawningInstanceAndSkipsResourcePackWhenAbsent() {
        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 10, () -> new LobbyPhase(lobbyConfig()), Optional.empty(), new LateJoinSlots(0));
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        listener.accept(event);

        assertSame(instance, event.getSpawningInstance());
        assertNull(player.getResourcePackFuture());
    }

    @Test
    void testSuccessfulLoginDisablesRespawnScreen() {
        player.setEnableRespawnScreen(true);

        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 10, () -> new LobbyPhase(lobbyConfig()), Optional.empty(), new LateJoinSlots(0));
        listener.accept(new AsyncPlayerConfigurationEvent(player, true));
    }

    @Test
    void testSuccessfulLoginSendsResourcePackWhenPresent() {
        GameConfig.ResourcePack packConfig = new GameConfig.ResourcePack(URI.create("https://example.com/pack.zip"), "a".repeat(40));
        Optional<ResourcePackService> resourcePackService = ResourcePackService.create(packConfig);
        assertTrue(resourcePackService.isPresent());

        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 10, () -> new LobbyPhase(lobbyConfig()), resourcePackService, new LateJoinSlots(0));
        listener.accept(new AsyncPlayerConfigurationEvent(player, true));

        assertNotNull(player.getResourcePackFuture());
    }
}
