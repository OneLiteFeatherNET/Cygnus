package net.onelitefeather.cygnus.gaze;

import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the boss bar the gaze is signalled through.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
class BossBarGazeSignalTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("Attaching gives the survivor an unflagged bar in the signal colour")
    void attachingCreatesAnUnflaggedBar(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());

        signal.attach(survivor);

        BossBar bar = signal.barOf(survivor);
        assertNotNull(bar);
        assertEquals(BossBarGazeSignal.SIGNAL_COLOR, bar.color(), "the pack hides exactly this colour");
        assertTrue(bar.name().equals(net.kyori.adventure.text.Component.empty()), "an empty name draws no text");
        assertFalse(bar.hasFlag(BossBar.Flag.DARKEN_SCREEN), "nothing is signalled before a level arrives");
    }

    @Test
    @DisplayName("A level raises the flag, none takes it back down")
    void levelTogglesTheFlag(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());
        signal.attach(survivor);

        signal.level(survivor, 2);
        assertTrue(signal.barOf(survivor).hasFlag(BossBar.Flag.DARKEN_SCREEN));

        signal.level(survivor, SlenderGaze.NONE);
        assertFalse(signal.barOf(survivor).hasFlag(BossBar.Flag.DARKEN_SCREEN));
    }

    @Test
    @DisplayName("Every survivor gets their own bar, so one gaze is not everyone's")
    void barsAreNotShared(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Instance instance = env.createFlatInstance();
        Player first = connect(env, instance);
        Player second = connect(env, instance);
        signal.attach(first);
        signal.attach(second);

        signal.level(first, 3);

        assertNotSame(signal.barOf(first), signal.barOf(second));
        assertTrue(signal.barOf(first).hasFlag(BossBar.Flag.DARKEN_SCREEN));
        assertFalse(signal.barOf(second).hasFlag(BossBar.Flag.DARKEN_SCREEN),
                "a shared bar would broadcast one survivor's gaze to everyone");
    }

    @Test
    @DisplayName("Detaching drops the bar")
    void detachingDropsTheBar(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());
        signal.attach(survivor);

        signal.detach(survivor);

        assertNull(signal.barOf(survivor));
    }

    @Test
    @DisplayName("A level for an unattached survivor is ignored rather than thrown")
    void levelWithoutAttachIsIgnored(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());

        signal.level(survivor, 1);

        assertNull(signal.barOf(survivor));
    }

    private Player connect(Env env, Instance instance) {
        return env.createConnection().connect(instance, new Pos(0, 40, 0));
    }
}
