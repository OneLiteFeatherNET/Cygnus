package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.overlay.OverlayFont;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.ScreenOverlay;

/**
 * Contributes the tunnel vision to the shared screen overlay.
 * <p>
 * Each stage is a glyph of the overlay font shipped with the resource pack. Minecraft cannot
 * animate font textures — {@code .mcmeta} animation is limited to block, item, particle, painting
 * and effect textures — so the heartbeat is the server walking through the stages, one glyph per
 * frame.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class OverlayTunnelVisionRenderer implements TunnelVisionRenderer {

    /** Code point of stage 1; the remaining stages follow consecutively. */
    static final int FIRST_CODE_POINT = 0xE000;

    /** Prepared once: the overlay is refreshed ten times per second per survivor. */
    private static final Component[] GLYPHS = buildGlyphs();

    private final ScreenOverlay overlay;

    /**
     * Creates a renderer drawing into the given overlay.
     *
     * @param overlay the overlay that owns the HUD channel
     */
    public OverlayTunnelVisionRenderer(ScreenOverlay overlay) {
        this.overlay = overlay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(Player player, int stage) {
        if (stage <= 0) {
            this.clear(player);
            return;
        }
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, GLYPHS[Math.min(stage, TunnelVisionStage.MAX_STAGE) - 1]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only this layer is dropped. Wiping the whole screen would take the blood splatter with it.
     * </p>
     */
    @Override
    public void clear(Player player) {
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, null);
    }

    /**
     * Builds the component for every stage.
     *
     * @return the prepared components, indexed by {@code stage - 1}
     */
    private static Component[] buildGlyphs() {
        Component[] glyphs = new Component[TunnelVisionStage.MAX_STAGE];
        for (int stage = 1; stage <= TunnelVisionStage.MAX_STAGE; stage++) {
            glyphs[stage - 1] = OverlayFont.glyph(FIRST_CODE_POINT + stage - 1);
        }
        return glyphs;
    }
}
