package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;

/**
 * Draws the tunnel vision as a HUD overlay through the action bar.
 * <p>
 * Each stage is a glyph of the {@code cygnus:tunnel_vision} bitmap font shipped with the resource
 * pack. The action bar is the only HUD channel a survivor has free — the progress bar in
 * {@code StaminaColors} belongs to the slender — and unlike a title it needs no fade timing.
 * </p>
 * <p>
 * Font glyphs are positioned relative to the action bar, and the server knows neither the client's
 * resolution nor its GUI scale, so the vignette cannot be centred exactly. The textures are
 * therefore larger than any realistic viewport and opaque at their edge, which turns the offset
 * into something the soft vignette hides.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class ActionBarTunnelVisionRenderer implements TunnelVisionRenderer {

    /** Bitmap font provided by {@code cygnus-pack} that carries the vignette glyphs. */
    static final Key FONT = Key.key("cygnus", "tunnel_vision");

    /** Code point of stage 1; the remaining stages follow consecutively. */
    static final int FIRST_CODE_POINT = 0xE000;

    /** Prepared once: the overlay is refreshed ten times per second per survivor. */
    private static final Component[] GLYPHS = buildGlyphs();

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(Player player, int stage) {
        if (stage <= 0) {
            this.clear(player);
            return;
        }
        player.sendActionBar(GLYPHS[Math.min(stage, TunnelVisionStage.MAX_STAGE) - 1]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(Player player) {
        player.sendActionBar(Component.empty());
    }

    /**
     * Builds the component for every stage.
     * <p>
     * The shadow is switched off explicitly: with it, Minecraft renders the whole vignette a
     * second time, offset by a pixel, underneath itself.
     * </p>
     *
     * @return the prepared components, indexed by {@code stage - 1}
     */
    private static Component[] buildGlyphs() {
        Component[] glyphs = new Component[TunnelVisionStage.MAX_STAGE];
        for (int stage = 1; stage <= TunnelVisionStage.MAX_STAGE; stage++) {
            String glyph = new String(Character.toChars(FIRST_CODE_POINT + stage - 1));
            glyphs[stage - 1] = Component.text(glyph)
                    .font(FONT)
                    .shadowColor(ShadowColor.none());
        }
        return glyphs;
    }
}
