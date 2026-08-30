package net.onelitefeather.cygnus.gaze;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.utils.PlayerState;
import org.jetbrains.annotations.Nullable;

/**
 * Signals the gaze to the client through the {@code darken_screen} flag of a boss bar the player
 * cannot see.
 *
 * <p>The flag is one of the few things a server can set for a single player that reaches the
 * client's lightmap shader, as {@code BossOverlayWorldDarkeningFactor}. That makes it a channel:
 * the resource pack reads the factor and draws whatever it likes, and by leaving vanilla's own
 * darkening out of its shader the pack turns the flag into a signal nobody sees as darkening.</p>
 *
 * <p>The bar is invisible because the pack ships fully transparent sprites for
 * {@code boss_bar/purple_background.png} and {@code purple_progress.png}. That colour is used for
 * nothing else - the game view is white, the setup module red and green - so overriding it hides
 * this bar and only this bar. Its name is empty, so no text is drawn either.</p>
 *
 * <p>Every survivor gets their own bar. Adventure keeps flags on the bar rather than per viewer, so
 * one shared bar would broadcast one survivor's gaze to everyone. The bar is also shown for as long
 * as the survivor is tracked rather than only while the slender is in view: bars stack, and one
 * appearing would push the game view's bar down the screen.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class BossBarGazeSignal implements GazeSink {

    /**
     * The colour whose sprites the resource pack replaces with transparent ones. Nothing else in
     * Cygnus uses it.
     */
    static final BossBar.Color SIGNAL_COLOR = BossBar.Color.PURPLE;

    private final PlayerState<BossBar> bars = new PlayerState<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void attach(Player survivor) {
        BossBar bar = BossBar.bossBar(Component.empty(), 1f, SIGNAL_COLOR, BossBar.Overlay.PROGRESS);
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

        if (level == SlenderGaze.NONE) {
            bar.removeFlag(BossBar.Flag.DARKEN_SCREEN);
            return;
        }
        bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
    }

    /**
     * Returns the signal bar of a survivor, for tests and for anything that needs to look at what
     * is currently being signalled.
     *
     * @param survivor the survivor to look up
     * @return their bar, or {@code null} if they are not attached
     */
    public @Nullable BossBar barOf(Player survivor) {
        return this.bars.get(survivor);
    }
}
