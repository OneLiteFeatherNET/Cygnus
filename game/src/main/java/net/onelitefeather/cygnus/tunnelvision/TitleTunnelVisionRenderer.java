package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the tunnel vision as a HUD overlay through the title.
 * <p>
 * The title is the better channel of the two the server has. It renders centred on the screen and
 * at four times scale, which makes the vignette's position independent of the client's resolution
 * — the action bar hangs off the bottom edge, so a glyph anchored to it drifts with every window
 * size.
 * </p>
 * <p>
 * Each stage is a glyph of the {@code cygnus:tunnel_vision} bitmap font shipped with the resource
 * pack. Minecraft cannot animate font textures — {@code .mcmeta} animation is limited to block,
 * item, particle, painting and effect textures — so the heartbeat is the server walking through
 * the stages, one glyph per frame.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class TitleTunnelVisionRenderer implements TunnelVisionRenderer {

    /** Bitmap font provided by {@code cygnus-pack} that carries the vignette glyphs. */
    static final Key FONT = Key.key("cygnus", "tunnel_vision");

    /** Code point of stage 1; the remaining stages follow consecutively. */
    static final int FIRST_CODE_POINT = 0xE000;

    /**
     * How long a frame survives without a follow-up. Comfortably longer than the service tick, so
     * the overlay never blinks between updates, and short enough to disappear on its own should
     * the server stop drawing.
     */
    private static final Title.Times TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofSeconds(2),
            Duration.ZERO
    );

    /** Prepared once: the overlay is refreshed ten times per second per survivor. */
    private static final Component[] GLYPHS = buildGlyphs();

    private final Set<UUID> timed = ConcurrentHashMap.newKeySet();

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(Player player, int stage) {
        if (stage <= 0) {
            this.clear(player);
            return;
        }

        this.ensureTimes(player);
        player.sendTitlePart(TitlePart.TITLE, GLYPHS[Math.min(stage, TunnelVisionStage.MAX_STAGE) - 1]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(Player player) {
        this.timed.remove(player.getUuid());
        player.sendTitlePart(TitlePart.TITLE, Component.empty());
    }

    /**
     * Sends the fade timings once per player.
     * <p>
     * They stay in effect for every following title, so repeating them ten times a second would
     * only double the packet count.
     * </p>
     *
     * @param player the player to prepare
     */
    private void ensureTimes(Player player) {
        if (!this.timed.add(player.getUuid())) return;
        player.sendTitlePart(TitlePart.TIMES, TIMES);
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
