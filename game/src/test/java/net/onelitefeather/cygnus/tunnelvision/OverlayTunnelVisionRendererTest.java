package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
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

/**
 * Verifies which texture the tunnel vision contributes to the shared screen overlay.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
class OverlayTunnelVisionRendererTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A stage is contributed as its overlay texture")
    void stageIsContributedAsTexture(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, 3);

        assertEquals(
                Key.key("cygnus", OverlayTunnelVisionRenderer.TEXTURE_PATH + "3"),
                overlay.of(OverlayLayer.TUNNEL_VISION),
                "the texture must match the stage"
        );
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
    @DisplayName("Stage zero drops the layer instead of drawing an empty texture")
    void zeroStageDropsTheLayer(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, 0);

        assertNull(overlay.of(OverlayLayer.TUNNEL_VISION));
    }

    @Test
    @DisplayName("The tightest stage has a texture of its own")
    void tightestStageHasItsOwnTexture(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, TunnelVisionStage.MAX_STAGE);

        assertEquals(
                Key.key("cygnus", OverlayTunnelVisionRenderer.TEXTURE_PATH + TunnelVisionStage.MAX_STAGE),
                overlay.of(OverlayLayer.TUNNEL_VISION)
        );
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
     * Records what a renderer contributes, standing in for the equipment-backed overlay.
     */
    private static final class RecordingOverlay implements ScreenOverlay {

        private final Map<OverlayLayer, Key> layers = new EnumMap<>(OverlayLayer.class);
        private boolean wiped;

        @Override
        public void set(Player player, OverlayLayer layer, @Nullable Key texture) {
            if (texture == null) {
                this.layers.remove(layer);
                return;
            }
            this.layers.put(layer, texture);
        }

        @Override
        public void clear(Player player) {
            this.wiped = true;
            this.layers.clear();
        }

        /**
         * @param layer the layer to look up
         * @return the texture currently set for the layer, or {@code null} if there is none
         */
        private @Nullable Key of(OverlayLayer layer) {
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
