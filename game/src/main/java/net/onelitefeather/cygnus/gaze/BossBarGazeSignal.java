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
 * <p>Four other channels were tried first and all of them failed. A boss bar's darken_screen flag
 * reaches only {@code lightmap.fsh}, which writes a 16x16 texture and has no screen position, so it
 * cannot carry a screen-space effect. Night vision and blindness are both already used for
 * gameplay - {@code AmbientProvider} puts blindness on survivors periodically, which would fire the
 * effect at random. A per-player biome would work but needs a chunk mesh rebuild on every change.
 * The colour of a glyph costs none of that.</p>
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
 * @version 2.0.0
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
        bar.name(signalFor(level));
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
