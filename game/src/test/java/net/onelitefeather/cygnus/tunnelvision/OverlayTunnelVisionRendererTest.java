package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.overlay.OverlayFont;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.ScreenOverlay;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies which glyph the tunnel vision contributes to the shared screen overlay.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class OverlayTunnelVisionRendererTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A stage is contributed as its glyph in the pack font")
    void stageIsContributedAsGlyph(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, 3);

        Component glyph = overlay.of(OverlayLayer.TUNNEL_VISION);
        assertEquals(OverlayFont.KEY, glyph.style().font(), "the overlay must use the pack font");
        assertEquals(glyphOf(3), plain(glyph), "the glyph must match the stage");
    }

    @Test
    @DisplayName("The glyph is drawn without a text shadow")
    void glyphHasNoShadow(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, TunnelVisionStage.MAX_STAGE);

        assertEquals(ShadowColor.none(), overlay.of(OverlayLayer.TUNNEL_VISION).style().shadowColor(),
                "a shadow would render the vignette a second time, offset");
    }

    @Test
    @DisplayName("Clearing drops only the tunnel vision layer")
    void clearingDropsOnlyItsOwnLayer(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        OverlayTunnelVisionRenderer renderer = new OverlayTunnelVisionRenderer(overlay);
        renderer.render(player, 4);

        renderer.clear(player);

        assertNull(overlay.of(OverlayLayer.TUNNEL_VISION), "the layer must be gone");
        assertFalse(overlay.wasWiped(), "wiping the screen would take the blood splatter with it");
    }

    @Test
    @DisplayName("Stage zero drops the layer instead of drawing an empty glyph")
    void zeroStageDropsTheLayer(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, 0);

        assertNull(overlay.of(OverlayLayer.TUNNEL_VISION));
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
     * Serialises a component down to its bare text.
     *
     * @param component the component to serialise
     * @return the plain text
     */
    private String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Builds the glyph expected for a stage.
     *
     * @param stage the stage
     * @return the glyph as a string
     */
    private String glyphOf(int stage) {
        return new String(Character.toChars(OverlayTunnelVisionRenderer.FIRST_CODE_POINT + stage - 1));
    }

    /**
     * Records what a renderer contributes, standing in for the title-backed overlay.
     */
    private static final class RecordingOverlay implements ScreenOverlay {

        private final Map<OverlayLayer, Component> layers = new EnumMap<>(OverlayLayer.class);
        private boolean wiped;

        @Override
        public void set(Player player, OverlayLayer layer, @Nullable Component glyph) {
            if (glyph == null) {
                this.layers.remove(layer);
                return;
            }
            this.layers.put(layer, glyph);
        }

        @Override
        public void clear(Player player) {
            this.wiped = true;
            this.layers.clear();
        }

        /**
         * @param layer the layer to look up
         * @return the glyph currently set for the layer, or {@code null} if there is none
         */
        private @Nullable Component of(OverlayLayer layer) {
            return this.layers.get(layer);
        }

        /**
         * @return whether the whole screen was cleared rather than a single layer
         */
        private boolean wasWiped() {
            return this.wiped;
        }
    }
}
