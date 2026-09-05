package net.onelitefeather.cygnus.gaze;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.utils.PlayerState;
import org.jetbrains.annotations.Nullable;

/**
 * Signals the gaze level to the client by colouring a single glyph the resource pack's text shader
 * watches for.
 *
 * <p>A text component's colour arrives at the client's text shader as the {@code Color} vertex
 * attribute, unchanged and per player. That makes it a data channel: the pack recognises the
 * reserved range, reads the level out of the low bits, and blows the glyph's quad up to cover the
 * screen. The glyph is therefore never seen where it is written.</p>
 *
 * <p>The bar carries two channels at once. Its title colour is the level, which the pack's text
 * shader reads to draw a graded veil. Its {@code darken_screen} flag is a single bit arriving in
 * {@code lightmap.fsh} as {@code BossOverlayWorldDarkeningFactor} - the one per-player value that
 * reaches the world's own lighting, since the lightmap is multiplied into {@code vertexColor}
 * before any world shader sees it. The two passes cannot talk to each other, so the level travels
 * twice: as a bit for the world tint, as a colour for the veil on top.</p>
 *
 * <p>Night vision and blindness were the other candidates for the bit and are both already used for
 * gameplay - {@code AmbientProvider} puts blindness on survivors periodically, which would fire the
 * effect at random. A per-player biome would carry more than a bit but needs a chunk mesh rebuild on
 * every change.</p>
 *
 * <p>Three details decide whether the signal survives the trip, and all three are set here:</p>
 * <ul>
 *   <li>The shadow is switched off. Vanilla draws text shadows as a second pass in a darkened
 *       colour, which would arrive as a second, wrong signal.</li>
 *   <li>The colour is set as an RGB value, never through legacy formatting, which would snap it to
 *       one of the sixteen vanilla colours.</li>
 *   <li>The character is a real glyph from the pack's own font, not a space. A space produces no
 *       geometry, and a vertex the shader never sees carries nothing.</li>
 * </ul>
 *
 * <p>The carrier is a boss bar per player. Its own sprites are transparent in the pack, so the bar
 * itself draws nothing, and Adventure keeps a bar's title per bar rather than per viewer - one
 * shared bar would send every survivor the same level.</p>
 *
 * @author TheMeinerLP
 * @version 4.0.0
 * @since 2.7.3
 */
public final class BossBarGazeSignal implements GazeSink {

    /** The colour whose boss bar sprites the resource pack replaces with transparent ones. */
    static final BossBar.Color CARRIER_COLOR = BossBar.Color.PURPLE;

    /** The font the signal glyph is taken from; shipped by the resource pack. */
    static final Key SIGNAL_FONT = Key.key("cygnus", "glitch");

    /** The private-use glyph carrying the signal. */
    static final String SIGNAL_GLYPH = "";

    /** Base of the reserved colour range the pack's text shader watches for. */
    static final int SIGNAL_BASE = 0xFE0000;


    /*
     * One carrier, deliberately. A diagnostic round sent the signal over the boss bar title, the
     * scoreboard sidebar and the action bar at once to find out which of them reaches the pack's
     * text shader. All three did - and every recognised vertex expands to its own full-screen quad,
     * so the overlays stacked. The action bar made that visible as flicker: it fades by itself
     * after a few seconds, and Cygnus overwrites it in the lobby anyway, so one of the three
     * overlays kept appearing and vanishing underneath the others.
     */
    private final PlayerState<BossBar> bars = new PlayerState<>();

    /**
     * Builds the component that carries a level.
     *
     * @param level the level to encode, {@link SlenderGaze#NONE} for nothing to show
     * @return the component to put on the wire
     */
    static Component signalFor(int level) {
        int encoded = level == SlenderGaze.NONE ? 0 : Math.clamp(level + 1, 0, 4);
        return Component.text(SIGNAL_GLYPH)
                .font(SIGNAL_FONT)
                .color(TextColor.color(SIGNAL_BASE + encoded))
                .shadowColor(ShadowColor.none());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attach(Player survivor) {
        BossBar bar = BossBar.bossBar(
                signalFor(SlenderGaze.NONE), 1f, CARRIER_COLOR, BossBar.Overlay.PROGRESS);
        this.bars.put(survivor, bar);
        survivor.showBossBar(bar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void detach(Player survivor) {
        BossBar bar = this.bars.remove(survivor);
        if (bar == null) return;
        survivor.hideBossBar(bar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void level(Player survivor, int level) {
        BossBar bar = this.bars.get(survivor);
        if (bar == null) return;

        // Two channels on one bar, because the effect needs both halves and neither can carry it
        // alone. The title's colour is the level, read by the pack's text shader, which draws the
        // graded veil. The darken_screen flag is a single bit that arrives in lightmap.fsh as
        // BossOverlayWorldDarkeningFactor, and that is the only per-player value which reaches the
        // world's lighting - the lightmap is multiplied into vertexColor before any world shader
        // sees it, so a hue rotation there tints everything. The text shader runs in a different
        // pass and cannot tell the lightmap anything, which is why the level has to travel twice.
        bar.name(signalFor(level));

        if (level == SlenderGaze.NONE) {
            bar.removeFlag(BossBar.Flag.DARKEN_SCREEN);
            return;
        }
        bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
    }

    /**
     * Returns the signal bar of a survivor.
     *
     * @param survivor the survivor to look up
     * @return their bar, or {@code null} if they are not attached
     */
    public @Nullable BossBar barOf(Player survivor) {
        return this.bars.get(survivor);
    }
}
