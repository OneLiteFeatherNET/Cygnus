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

        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> {}, () -> instance, new WakeUpTransition());
        phase.start();

        assertEquals(0.0, player.getAttributeValue(Attribute.MOVEMENT_SPEED));
        assertEquals(0.0, player.getAttributeValue(Attribute.JUMP_STRENGTH));
        // A walking speed of zero makes the client skip the speed based FOV zoom
        assertEquals(0f, player.getFieldViewModifier());

        phase.finish();
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

        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> {}, () -> instance, new WakeUpTransition());
        phase.start();
        phase.finish();

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
        transition.start(player, instance);

        WaitingPhase phase = new WaitingPhase(new GameViewImpl(), () -> {}, () -> {}, () -> instance, transition);
        phase.start();
        phase.finish();

        assertTrue(transition.isRunning(player), "The wake-up transition must still run after the waiting phase ended");
        assertTrue(instance.getEntities().stream().anyMatch(entity -> entity.getEntityType() == EntityType.SPIDER),
                "The spider must still exist after the waiting phase ended");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    private static EventNode<Event> recordingNode(List<String> calls) {
        EventNode<Event> node = EventNode.all("waiting-phase-test");
        node.addListener(GamePrepareEvent.class, _ -> calls.add("prepare"));
        node.addListener(GamePreLaunchEvent.class, _ -> calls.add("launch"));
        MinecraftServer.getGlobalEventHandler().addChild(node);
        return node;
    }
}
