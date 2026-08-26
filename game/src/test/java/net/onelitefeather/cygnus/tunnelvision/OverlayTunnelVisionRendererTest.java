package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.RecordingScreenOverlay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies which texture the tunnel vision contributes to the shared screen overlay.
 *
 * @author TheMeinerLP
 * @version 2.1.0
 * @since 2.7.0
 */
class OverlayTunnelVisionRendererTest extends CygnusPlayerTestBase {

    private static final Key BLOOD_TEXTURE = Key.key("cygnus", "gui/blood/stage_1");

    @Test
    @DisplayName("A stage is contributed as its overlay texture")
    void stageIsContributedAsTexture(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, 3);

        assertEquals(
                Key.key("cygnus", OverlayTunnelVisionRenderer.TEXTURE_PATH + "3"),
                overlay.of(player, OverlayLayer.TUNNEL_VISION),
                "the texture must match the stage"
        );
    }

    @Test
    @DisplayName("Clearing drops only the tunnel vision layer")
    void clearingDropsOnlyItsOwnLayer(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        overlay.set(player, OverlayLayer.BLOOD, BLOOD_TEXTURE);
        OverlayTunnelVisionRenderer renderer = new OverlayTunnelVisionRenderer(overlay);
        renderer.render(player, 4);

        renderer.clear(player);

        assertNull(overlay.of(player, OverlayLayer.TUNNEL_VISION), "the layer must be gone");
        assertEquals(
                BLOOD_TEXTURE,
                overlay.of(player, OverlayLayer.BLOOD),
                "wiping the screen would take the blood splatter with it"
        );
    }

    @Test
    @DisplayName("Stage zero drops the layer instead of drawing an empty texture")
    void zeroStageDropsTheLayer(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, 0);

        assertNull(overlay.of(player, OverlayLayer.TUNNEL_VISION));
    }

    @Test
    @DisplayName("The tightest stage has a texture of its own")
    void tightestStageHasItsOwnTexture(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);

        new OverlayTunnelVisionRenderer(overlay).render(player, TunnelVisionStage.MAX_STAGE);

        assertEquals(
                Key.key("cygnus", OverlayTunnelVisionRenderer.TEXTURE_PATH + TunnelVisionStage.MAX_STAGE),
                overlay.of(player, OverlayLayer.TUNNEL_VISION)
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
}
