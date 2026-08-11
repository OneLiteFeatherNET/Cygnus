package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how the service feeds survivors through the intensity calculation.
 * <p>
 * The slender no longer feeds into this — he speaks through {@code SlenderGazeService} — so what
 * is left here is the stamina and the lifecycle.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class TunnelVisionServiceTest extends CygnusPlayerTestBase {

    private static final double FULL_STAMINA = 1.0D;
    private static final double NO_STAMINA = 0.0D;

    @Test
    @DisplayName("An exhausted survivor sees the tightest stage")
    void exhaustedSurvivorIsFullyNarrowed(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        Player survivor = spawn(env, new Pos(0, 40, 0));
        TunnelVisionService service = new TunnelVisionService(renderer, player -> NO_STAMINA);
        service.track(survivor);

        service.tick();

        assertEquals(TunnelVisionStage.MAX_STAGE, renderer.stageOf(survivor));
    }

    @Test
    @DisplayName("A rested survivor alone in the dark sees nothing")
    void restedSurvivorSeesNothing(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        Player survivor = spawn(env, new Pos(0, 40, 0));
        TunnelVisionService service = new TunnelVisionService(renderer, player -> FULL_STAMINA);
        service.track(survivor);

        service.tick();

        assertEquals(0, renderer.stageOf(survivor));
    }

    @Test
    @DisplayName("A removed survivor gets their screen back and is no longer drawn")
    void removedSurvivorIsCleared(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        Player survivor = spawn(env, new Pos(0, 40, 0));
        TunnelVisionService service = new TunnelVisionService(renderer, player -> NO_STAMINA);
        service.track(survivor);
        service.tick();
        renderer.forget();

        service.remove(survivor);
        service.tick();

        assertTrue(renderer.wasCleared(survivor), "the last vignette would otherwise linger");
        assertNull(renderer.stageOf(survivor), "a removed survivor must not be drawn any more");
    }

    @Test
    @DisplayName("Clearing everyone gives every survivor their screen back")
    void clearAllClearsEveryone(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        Instance instance = env.createFlatInstance();
        Player first = spawn(env, instance, new Pos(0, 40, 0));
        Player second = spawn(env, instance, new Pos(4, 40, 0));
        TunnelVisionService service = new TunnelVisionService(renderer, player -> NO_STAMINA);
        service.track(first);
        service.track(second);
        service.tick();

        service.clearAll();

        assertTrue(renderer.wasCleared(first));
        assertTrue(renderer.wasCleared(second));

        renderer.forget();
        service.tick();
        assertNull(renderer.stageOf(first), "clearing must stop the drawing as well");
    }

    @Test
    @DisplayName("Starting and stopping the task is idempotent")
    void startAndStopTaskAreIdempotent(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        TunnelVisionService service = new TunnelVisionService(renderer, player -> NO_STAMINA);

        service.startTask();
        service.startTask();
        service.stopTask();
        service.stopTask();
    }

    @Test
    @DisplayName("The start of a round takes the survivors on board")
    void gameStartRegistersSurvivors(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        Player survivor = spawn(env, new Pos(0, 40, 0));
        TunnelVisionService service = new TunnelVisionService(renderer, player -> NO_STAMINA);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));

        EventDispatcher.call(new GameStartEvent());
        service.tick();

        assertEquals(TunnelVisionStage.MAX_STAGE, renderer.stageOf(survivor));
    }

    @Test
    @DisplayName("A dying survivor gets their screen back")
    void deathClearsTheOverlay(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        Player survivor = spawn(env, new Pos(0, 40, 0));
        TunnelVisionService service = new TunnelVisionService(renderer, player -> NO_STAMINA);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));
        service.track(survivor);
        service.tick();
        renderer.forget();

        EventDispatcher.call(new PlayerDeathEvent(survivor, Component.empty(), Component.empty()));
        service.tick();

        assertTrue(renderer.wasCleared(survivor));
        assertNull(renderer.stageOf(survivor), "a dead survivor must not be drawn any more");
    }

    @Test
    @DisplayName("The end of a round clears everyone")
    void gameFinishCleansUp(Env env) {
        RecordingRenderer renderer = new RecordingRenderer();
        Player survivor = spawn(env, new Pos(0, 40, 0));
        TunnelVisionService service = new TunnelVisionService(renderer, player -> NO_STAMINA);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));
        service.track(survivor);
        service.tick();
        renderer.forget();

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertTrue(renderer.wasCleared(survivor));
    }

    /**
     * Spawns a player in a fresh instance.
     *
     * @param env      the test environment
     * @param position where to place the player
     * @return the connected player
     */
    private Player spawn(Env env, Pos position) {
        return this.spawn(env, env.createFlatInstance(), position);
    }

    /**
     * Spawns a player in the given instance.
     *
     * @param env      the test environment
     * @param instance the instance to connect into
     * @param position where to place the player
     * @return the connected player
     */
    private Player spawn(Env env, Instance instance, Pos position) {
        return env.createConnection().connect(instance, position);
    }

    /**
     * Records what the service asked to be drawn, standing in for the action bar renderer.
     */
    private static final class RecordingRenderer implements TunnelVisionRenderer {

        private final Map<UUID, Integer> stages = new HashMap<>();
        private final Set<UUID> cleared = new HashSet<>();

        @Override
        public void render(Player player, int stage) {
            this.stages.put(player.getUuid(), stage);
        }

        @Override
        public void clear(Player player) {
            this.cleared.add(player.getUuid());
            this.stages.remove(player.getUuid());
        }

        /**
         * @param player the player to look up
         * @return the stage last drawn for the player, or {@code null} if nothing was drawn
         */
        private @Nullable Integer stageOf(Player player) {
            return this.stages.get(player.getUuid());
        }

        /**
         * @param player the player to look up
         * @return whether the player's overlay was cleared
         */
        private boolean wasCleared(Player player) {
            return this.cleared.contains(player.getUuid());
        }

        /**
         * Drops everything recorded so far, to tell repeated draws apart.
         */
        private void forget() {
            this.stages.clear();
            this.cleared.clear();
        }
    }
}
