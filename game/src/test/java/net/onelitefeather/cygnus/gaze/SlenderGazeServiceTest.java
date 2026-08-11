package net.onelitefeather.cygnus.gaze;

import net.kyori.adventure.key.Key;
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
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.ScreenOverlay;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the tearing a survivor gets while the slender stands in their view.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class SlenderGazeServiceTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("Seeing the slender tears the survivor's view")
    void seeingHimTearsTheView(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.watch(survivor);

        service.tick();

        assertNotNull(overlay.of(survivor, OverlayLayer.GLITCH), "he is right in front of them");
    }

    @Test
    @DisplayName("With him behind them there is nothing to see")
    void behindThemNothingHappens(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, -5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.watch(survivor);

        service.tick();

        assertNull(overlay.of(survivor, OverlayLayer.GLITCH), "the effect is about seeing him");
    }

    @Test
    @DisplayName("Looking away takes it off again")
    void lookingAwayClearsIt(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.watch(survivor);
        service.tick();

        survivor.teleport(new Pos(0, 40, 0, 180, 0));
        service.tick();

        assertNull(overlay.of(survivor, OverlayLayer.GLITCH));
    }

    @Test
    @DisplayName("The tearing runs on while he stays in view")
    void tearingKeepsMoving(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.watch(survivor);

        service.tick();
        Key first = overlay.of(survivor, OverlayLayer.GLITCH);
        service.tick();

        assertNotEquals(first, overlay.of(survivor, OverlayLayer.GLITCH),
                "a still picture is not a glitch");
    }

    @Test
    @DisplayName("Without a slender nothing happens at all")
    void withoutASlenderNothingHappens(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player survivor = connect(env, env.createFlatInstance(), new Pos(0, 40, 0, 0, 0));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> null);
        service.watch(survivor);

        service.tick();

        assertNull(overlay.of(survivor, OverlayLayer.GLITCH));
    }

    @Test
    @DisplayName("A removed survivor gets their view back")
    void removedSurvivorIsCleared(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.watch(survivor);
        service.tick();

        service.remove(survivor);
        service.tick();

        assertNull(overlay.of(survivor, OverlayLayer.GLITCH));
    }

    @Test
    @DisplayName("Clearing gives every survivor their screen back")
    void clearWipesEveryone(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player first = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player second = connect(env, instance, new Pos(4, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.watch(first);
        service.watch(second);
        service.tick();

        service.clear();

        assertNull(overlay.of(first, OverlayLayer.GLITCH));
        assertNull(overlay.of(second, OverlayLayer.GLITCH));

        service.tick();
        assertNull(overlay.of(first, OverlayLayer.GLITCH), "clear must stop the drawing as well");
    }

    @Test
    @DisplayName("The start of a round takes the survivors on board")
    void gameStartWatchesSurvivors(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));

        EventDispatcher.call(new GameStartEvent());
        service.tick();

        assertNotNull(overlay.of(survivor, OverlayLayer.GLITCH));
    }

    @Test
    @DisplayName("A dying survivor gets their screen back")
    void deathRemovesTheSurvivor(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));
        service.watch(survivor);
        service.tick();

        EventDispatcher.call(new PlayerDeathEvent(survivor, Component.empty(), Component.empty()));
        service.tick();

        assertNull(overlay.of(survivor, OverlayLayer.GLITCH));
    }

    @Test
    @DisplayName("The end of a round clears everyone")
    void gameFinishClearsEveryone(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(overlay, () -> slender);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));
        service.watch(survivor);
        service.tick();

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertNull(overlay.of(survivor, OverlayLayer.GLITCH));
    }

    /**
     * Connects a player at the given position.
     *
     * @param env      the test environment
     * @param instance the instance to connect into
     * @param position where to place them
     * @return the connected player
     */
    private Player connect(Env env, Instance instance, Pos position) {
        return env.createConnection().connect(instance, position);
    }

    /**
     * Records what the service contributes, standing in for the equipment-backed overlay.
     */
    private static final class RecordingOverlay implements ScreenOverlay {

        private final Map<UUID, Map<OverlayLayer, Key>> layers = new HashMap<>();

        @Override
        public void set(Player player, OverlayLayer layer, @Nullable Key texture) {
            Map<OverlayLayer, Key> current =
                    this.layers.computeIfAbsent(player.getUuid(), key -> new EnumMap<>(OverlayLayer.class));
            if (texture == null) {
                current.remove(layer);
                return;
            }
            current.put(layer, texture);
        }

        @Override
        public void clear(Player player) {
            this.layers.remove(player.getUuid());
        }

        /**
         * @param player the player to look up
         * @param layer  the layer to look up
         * @return the texture currently set, or {@code null} if there is none
         */
        private @Nullable Key of(Player player, OverlayLayer layer) {
            return this.layers.getOrDefault(player.getUuid(), Map.of()).get(layer);
        }
    }
}
