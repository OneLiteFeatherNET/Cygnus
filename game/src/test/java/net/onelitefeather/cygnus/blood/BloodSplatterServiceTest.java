package net.onelitefeather.cygnus.blood;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.RecordingScreenOverlay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the splatter that flashes up when a player is hit and fades out on its own.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.7.0
 */
class BloodSplatterServiceTest extends CygnusPlayerTestBase {

    /** Always picks the first variant, so the expected code points are predictable. */
    private static final java.util.function.IntUnaryOperator FIRST_VARIANT = bound -> 0;

    @Test
    @DisplayName("A hit puts the first frame on screen right away")
    void hitShowsTheFirstFrame(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);

        service.splatter(player, BloodDirection.FRONT);

        assertEquals(textureOf(BloodDirection.FRONT, 0, 0), overlay.of(player, OverlayLayer.BLOOD));
    }

    @Test
    @DisplayName("The direction of the hit picks a different set of frames")
    void directionPicksItsOwnFrames(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);

        service.splatter(player, BloodDirection.LEFT);

        assertEquals(textureOf(BloodDirection.LEFT, 0, 0), overlay.of(player, OverlayLayer.BLOOD));
        assertNotEquals(textureOf(BloodDirection.FRONT, 0, 0), overlay.of(player, OverlayLayer.BLOOD));
    }

    @Test
    @DisplayName("The splatter fades frame by frame and disappears")
    void splatterFadesAway(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        service.splatter(player, BloodDirection.FRONT);

        service.tick();
        assertEquals(textureOf(BloodDirection.FRONT, 0, 1), overlay.of(player, OverlayLayer.BLOOD), "the second frame follows");

        for (int remaining = 1; remaining < BloodSplatterService.FRAMES; remaining++) {
            service.tick();
        }

        assertNull(overlay.of(player, OverlayLayer.BLOOD), "the splatter has to clean up after itself");
    }

    @Test
    @DisplayName("A second hit restarts the splatter")
    void secondHitRestarts(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        service.splatter(player, BloodDirection.FRONT);
        service.tick();
        service.tick();

        service.splatter(player, BloodDirection.FRONT);

        assertEquals(textureOf(BloodDirection.FRONT, 0, 0), overlay.of(player, OverlayLayer.BLOOD), "a fresh hit starts over");
    }

    @Test
    @DisplayName("Being hit is announced by the damage event")
    void damageEventTriggersTheSplatter(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new PlayerDamagedEvent(player, new Pos(0, 40, 6), 1.0F));

        assertNull(overlay.of(player, OverlayLayer.TUNNEL_VISION), "only the blood layer belongs to this service");
        assertTrue(overlay.of(player, OverlayLayer.BLOOD) != null, "a hit has to show blood");
    }

    @Test
    @DisplayName("Clearing takes the splatter off the screen")
    void clearingRemovesTheSplatter(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        service.splatter(player, BloodDirection.FRONT);

        service.clear(player);

        assertNull(overlay.of(player, OverlayLayer.BLOOD));
    }

    @Test
    @DisplayName("The fade task only runs while something is bleeding")
    void fadeTaskTracksActiveSplatters(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        assertFalse(service.fadeTask.isRunning(), "nothing is bleeding yet");

        service.splatter(player, BloodDirection.FRONT);
        assertTrue(service.fadeTask.isRunning(), "a hit has to keep the fade task alive");

        for (int remaining = 0; remaining < BloodSplatterService.FRAMES; remaining++) {
            service.tick();
        }

        assertFalse(service.fadeTask.isRunning(), "the task stops itself once nothing is bleeding any more");
    }

    @Test
    @DisplayName("Two players bleed independently")
    void playersAreIndependent(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(4, 40, 0));
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);

        service.splatter(first, BloodDirection.FRONT);
        service.tick();
        service.splatter(second, BloodDirection.BACK);

        assertEquals(textureOf(BloodDirection.FRONT, 0, 1), overlay.of(first, OverlayLayer.BLOOD));
        assertEquals(textureOf(BloodDirection.BACK, 0, 0), overlay.of(second, OverlayLayer.BLOOD));
    }

    /**
     * Connects a player into a fresh instance.
     *
     * @param env the test environment
     * @return the connected player
     */
    private Player spawn(Env env) {
        Instance instance = env.createFlatInstance();
        return env.createConnection().connect(instance, new Pos(0, 40, 0));
    }

    /**
     * Works out the texture a direction, variant and frame map to.
     *
     * @param direction the direction of the hit
     * @param variant   the variant index
     * @param frame     the frame index
     * @return the texture key
     */
    private Key textureOf(BloodDirection direction, int variant, int frame) {
        return Key.key("cygnus", "%s%s_%d_%d".formatted(
                BloodSplatterService.TEXTURE_PATH,
                direction.name().toLowerCase(java.util.Locale.ROOT),
                variant + 1,
                frame + 1
        ));
    }
}
