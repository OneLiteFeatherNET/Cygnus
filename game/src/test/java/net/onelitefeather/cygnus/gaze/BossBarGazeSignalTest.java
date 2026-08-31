package net.onelitefeather.cygnus.gaze;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
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
    @DisplayName("Attaching gives the survivor a carrier bar signalling nothing")
    void attachingCreatesACarrierBar(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());

        signal.attach(survivor);

        BossBar bar = signal.barOf(survivor);
        assertNotNull(bar);
        assertEquals(BossBarGazeSignal.CARRIER_COLOR, bar.color(), "the pack hides exactly this colour");
        assertEquals(BossBarGazeSignal.SIGNAL_BASE, colourOf(bar), "nothing is signalled before a level arrives");
    }

    @Test
    @DisplayName("The level is encoded into the glyph's colour")
    void levelIsEncodedIntoTheColour(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());
        signal.attach(survivor);

        signal.level(survivor, 0);
        assertEquals(BossBarGazeSignal.SIGNAL_BASE + 1, colourOf(signal.barOf(survivor)));

        signal.level(survivor, 3);
        assertEquals(BossBarGazeSignal.SIGNAL_BASE + 4, colourOf(signal.barOf(survivor)),
                "the strongest level must not wrap - that was the bug in the first encoding");

        signal.level(survivor, SlenderGaze.NONE);
        assertEquals(BossBarGazeSignal.SIGNAL_BASE, colourOf(signal.barOf(survivor)));
    }

    @Test
    @DisplayName("The signal carries font, glyph and no shadow")
    void signalCarriesEverythingTheShaderNeeds(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());
        signal.attach(survivor);
        signal.level(survivor, 2);

        Component name = signal.barOf(survivor).name();
        assertEquals(BossBarGazeSignal.SIGNAL_FONT, name.font(), "a missing font renders a fallback glyph");
        assertEquals(BossBarGazeSignal.SIGNAL_GLYPH, ((net.kyori.adventure.text.TextComponent) name).content(),
                "a space produces no geometry and therefore no vertex to read");
        assertEquals(ShadowColor.none(), name.shadowColor(),
                "a shadow is drawn as a second pass in a darkened colour, so it would arrive as a second signal");
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
        assertEquals(BossBarGazeSignal.SIGNAL_BASE + 4, colourOf(signal.barOf(first)));
        assertEquals(BossBarGazeSignal.SIGNAL_BASE, colourOf(signal.barOf(second)),
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

    @Test
    @DisplayName("The level travels twice: as a colour and as the world-tint bit")
    void levelCarriesBothChannels(Env env) {
        BossBarGazeSignal signal = new BossBarGazeSignal();
        Player survivor = connect(env, env.createFlatInstance());
        signal.attach(survivor);

        signal.level(survivor, 2);
        assertEquals(BossBarGazeSignal.SIGNAL_BASE + 3, colourOf(signal.barOf(survivor)));
        assertTrue(signal.barOf(survivor).hasFlag(BossBar.Flag.DARKEN_SCREEN),
                "the world tint needs the bit, the veil alone is not the effect");

        signal.level(survivor, SlenderGaze.NONE);
        assertEquals(BossBarGazeSignal.SIGNAL_BASE, colourOf(signal.barOf(survivor)));
        assertFalse(signal.barOf(survivor).hasFlag(BossBar.Flag.DARKEN_SCREEN));
    }

    /**
     * Reads the encoded colour back out of a bar's title.
     *
     * @param bar the bar to read
     * @return the colour value, or -1 if the title carries none
     */
    private static int colourOf(BossBar bar) {
        TextColor color = bar.name().color();
        return color == null ? -1 : color.value();
    }

    private Player connect(Env env, Instance instance) {
        return env.createConnection().connect(instance, new Pos(0, 40, 0));
    }
}
