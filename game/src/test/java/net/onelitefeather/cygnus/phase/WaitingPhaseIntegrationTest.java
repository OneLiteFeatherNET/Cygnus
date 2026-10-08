package net.onelitefeather.cygnus.phase;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.camera.WakeUpTransition;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.map.event.GamePrepareEvent;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitingPhaseIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The lobby countdown no longer hands out the roles while the players are still in the lobby")
    void lobbyDoesNotPrepareRoles(@NotNull Env env) {
        List<String> calls = new ArrayList<>();
        EventNode<Event> node = recordingNode(calls);

        LobbyPhase lobbyPhase = new LobbyPhase(new GameConfig.Round(2, 10, 30, 600));
        lobbyPhase.setCurrentTicks(0);
        lobbyPhase.onUpdate();

        MinecraftServer.getGlobalEventHandler().removeChild(node);
        assertTrue(calls.isEmpty(), "No role assignment expected in the lobby, got " + calls);
    }

    @Test
    @DisplayName("The roles are handed out right before the launch and the teleport into the game map")
    void preparesRolesRightBeforeTheTeleport(@NotNull Env env) {
        List<String> calls = new ArrayList<>();
        EventNode<Event> node = recordingNode(calls);
        Instance gameInstance = env.createFlatInstance();

        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> calls.add("teleport"), () -> gameInstance, new WakeUpTransition());
        phase.setCurrentTicks(1);
        phase.onUpdate();

        MinecraftServer.getGlobalEventHandler().removeChild(node);
        env.destroyInstance(gameInstance, true);
        assertEquals(List.of("prepare", "launch", "teleport"), calls);
    }

    @Test
    @DisplayName("Players can neither walk nor jump while the map is switched")
    void freezesPlayersDuringTheSwitch(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(0, 42, 0));

        WakeUpTransition transition = new WakeUpTransition();
        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> {}, () -> instance, transition);
        phase.start();

        assertEquals(0.0, player.getAttributeValue(Attribute.MOVEMENT_SPEED));
        assertEquals(0.0, player.getAttributeValue(Attribute.JUMP_STRENGTH));
        // A walking speed of zero makes the client skip the speed based FOV zoom
        assertEquals(0f, player.getFieldViewModifier());

        phase.finish();
        transition.cancelAll();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Players can move again once the waiting phase is over")
    void releasesPlayersAfterwards(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(0, 42, 0));
        double speed = player.getAttributeValue(Attribute.MOVEMENT_SPEED);
        double jump = player.getAttributeValue(Attribute.JUMP_STRENGTH);
        float fieldView = player.getFieldViewModifier();

        WakeUpTransition transition = new WakeUpTransition();
        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> {}, () -> instance, transition);
        phase.start();
        phase.finish();
        transition.cancelAll();

        assertEquals(speed, player.getAttributeValue(Attribute.MOVEMENT_SPEED));
        assertEquals(jump, player.getAttributeValue(Attribute.JUMP_STRENGTH));
        assertEquals(fieldView, player.getFieldViewModifier());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Finishing the waiting phase keeps the wake-up transition running until the round ends")
    void waitingPhaseEndKeepsTheWakeUpTransitionRunning(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(0, 42, 0));
        WakeUpTransition transition = new WakeUpTransition();
        transition.open(player, instance);

        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> {}, () -> instance, transition);
        phase.start();
        phase.finish();

        assertTrue(transition.isRunning(player), "The wake-up transition must still run after the waiting phase ended");
        assertTrue(instance.getEntities().stream().anyMatch(entity -> entity.getEntityType() == EntityType.SPIDER),
                "The spider must still exist after the waiting phase ended");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Starting the waiting phase closes the eyes of the players in the lobby")
    void onStartClosesTheEyesInTheLobby(@NotNull Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        Player player = env.createPlayer(lobby, new Pos(0, 42, 0));
        AtomicReference<Instance> active = new AtomicReference<>(lobby);
        WakeUpTransition transition = new WakeUpTransition();
        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> active.set(game), () -> {}, active::get, transition);

        phase.start();

        assertTrue(transition.isRunning(player, WakeUpTransition.Kind.CLOSE), "Starting the waiting phase must close the eyes of the lobby players");
        assertTrue(lobby.getEntities().stream().anyMatch(entity -> entity.getEntityType() == EntityType.SPIDER),
                "The close spider must be spawned in the lobby, where the player is when the phase starts");
        transition.cancelAll();
        phase.finish();
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    @DisplayName("Tick 1 opens the eyes for the game instance while the lobby close is still running")
    void tickOneOpensTheEyesInTheGameInstance(@NotNull Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        Player player = env.createPlayer(lobby, new Pos(0, 42, 0));
        AtomicReference<Instance> active = new AtomicReference<>(lobby);
        WakeUpTransition transition = new WakeUpTransition();
        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> active.set(game), () -> {}, active::get, transition);
        phase.start();

        phase.setCurrentTicks(1);
        phase.onUpdate();

        assertTrue(transition.isRunning(player, WakeUpTransition.Kind.OPEN), "Tick 1 must start the open transition for the game instance");
        assertTrue(transition.isRunning(player, WakeUpTransition.Kind.CLOSE), "The lobby close must keep running until the player leaves the lobby");
        assertTrue(game.getEntities().stream().noneMatch(entity -> entity.getEntityType() == EntityType.SPIDER),
                "The open spider must wait for the player to arrive in the game instance");
        transition.cancelAll();
        phase.finish();
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    private static EventNode<Event> recordingNode(List<String> calls) {
        EventNode<Event> node = EventNode.all("waiting-phase-test");
        node.addListener(GamePrepareEvent.class, _ -> calls.add("prepare"));
        node.addListener(GamePreLaunchEvent.class, _ -> calls.add("launch"));
        MinecraftServer.getGlobalEventHandler().addChild(node);
        return node;
    }
}
