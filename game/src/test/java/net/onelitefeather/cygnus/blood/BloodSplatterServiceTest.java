package net.onelitefeather.cygnus.blood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.overlay.OverlayFont;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.ScreenOverlay;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the splatter that flashes up when a player is hit and fades out on its own.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class BloodSplatterServiceTest extends CygnusPlayerTestBase {

    /** Always picks the first variant, so the expected code points are predictable. */
    private static final java.util.function.IntUnaryOperator FIRST_VARIANT = bound -> 0;

    @Test
    @DisplayName("A hit puts the first frame on screen right away")
    void hitShowsTheFirstFrame(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);

        service.splatter(player, BloodDirection.FRONT);

        assertEquals(codePointOf(BloodDirection.FRONT, 0, 0), glyphOf(overlay, player));
    }

    @Test
    @DisplayName("The direction of the hit picks a different set of frames")
    void directionPicksItsOwnFrames(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);

        service.splatter(player, BloodDirection.LEFT);

        assertEquals(codePointOf(BloodDirection.LEFT, 0, 0), glyphOf(overlay, player));
        assertNotEquals(codePointOf(BloodDirection.FRONT, 0, 0), glyphOf(overlay, player));
    }

    @Test
    @DisplayName("The splatter fades frame by frame and disappears")
    void splatterFadesAway(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        service.splatter(player, BloodDirection.FRONT);

        service.tick();
        assertEquals(codePointOf(BloodDirection.FRONT, 0, 1), glyphOf(overlay, player), "the second frame follows");

        for (int remaining = 1; remaining < BloodSplatterService.FRAMES; remaining++) {
            service.tick();
        }

        assertNull(overlay.of(player, OverlayLayer.BLOOD), "the splatter has to clean up after itself");
    }

    @Test
    @DisplayName("A second hit restarts the splatter")
    void secondHitRestarts(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        service.splatter(player, BloodDirection.FRONT);
        service.tick();
        service.tick();

        service.splatter(player, BloodDirection.FRONT);

        assertEquals(codePointOf(BloodDirection.FRONT, 0, 0), glyphOf(overlay, player), "a fresh hit starts over");
    }

    @Test
    @DisplayName("Being hit is announced by the damage event")
    void damageEventTriggersTheSplatter(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
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
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);
        service.splatter(player, BloodDirection.FRONT);

        service.clear(player);

        assertNull(overlay.of(player, OverlayLayer.BLOOD));
    }

    @Test
    @DisplayName("Two players bleed independently")
    void playersAreIndependent(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(4, 40, 0));
        BloodSplatterService service = new BloodSplatterService(overlay, FIRST_VARIANT);

        service.splatter(first, BloodDirection.FRONT);
        service.tick();
        service.splatter(second, BloodDirection.BACK);

        assertEquals(codePointOf(BloodDirection.FRONT, 0, 1), glyphOf(overlay, first));
        assertEquals(codePointOf(BloodDirection.BACK, 0, 0), glyphOf(overlay, second));
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
     * Reads the code point the blood layer currently shows for a player.
     *
     * @param overlay the overlay to read
     * @param player  the player to look up
     * @return the code point
     */
    private int glyphOf(RecordingOverlay overlay, Player player) {
        Component glyph = overlay.of(player, OverlayLayer.BLOOD);
        assertTrue(glyph != null, "no blood on screen");
        assertEquals(OverlayFont.KEY, glyph.style().font(), "the splatter must use the pack font");
        return PlainTextComponentSerializer.plainText().serialize(glyph).codePointAt(0);
    }

    /**
     * Works out the code point a direction, variant and frame map to.
     *
     * @param direction the direction of the hit
     * @param variant   the variant index
     * @param frame     the frame index
     * @return the code point
     */
    private int codePointOf(BloodDirection direction, int variant, int frame) {
        int index = (direction.ordinal() * BloodSplatterService.VARIANTS + variant) * BloodSplatterService.FRAMES;
        return BloodSplatterService.FIRST_CODE_POINT + index + frame;
    }

    /**
     * Records what the service contributes, standing in for the title-backed overlay.
     */
    private static final class RecordingOverlay implements ScreenOverlay {

        private final Map<UUID, Map<OverlayLayer, Component>> layers = new HashMap<>();

        @Override
        public void set(Player player, OverlayLayer layer, @Nullable Component glyph) {
            Map<OverlayLayer, Component> current =
                    this.layers.computeIfAbsent(player.getUuid(), key -> new EnumMap<>(OverlayLayer.class));
            if (glyph == null) {
                current.remove(layer);
                return;
            }
            current.put(layer, glyph);
        }

        @Override
        public void clear(Player player) {
            this.layers.remove(player.getUuid());
        }

        /**
         * @param player the player to look up
         * @param layer  the layer to look up
         * @return the glyph currently set, or {@code null} if there is none
         */
        private @Nullable Component of(Player player, OverlayLayer layer) {
            return this.layers.getOrDefault(player.getUuid(), Map.of()).get(layer);
        }
    }
}
