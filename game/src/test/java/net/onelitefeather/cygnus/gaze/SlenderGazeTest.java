package net.onelitefeather.cygnus.gaze;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies when the sight of the slender starts to tear a survivor's view apart.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class SlenderGazeTest {

    /** A survivor in the origin looking towards positive Z, which is a yaw of zero. */
    private static final Pos SURVIVOR = new Pos(0, 40, 0, 0, 0);

    @Test
    @DisplayName("A slender straight ahead and close tears the view apart")
    void closeAndAheadIsStrongest() {
        assertEquals(SlenderGaze.LEVELS - 1, SlenderGaze.levelOf(SURVIVOR, new Pos(0, 40, 4)));
    }

    @Test
    @DisplayName("A slender ahead but far off barely registers")
    void farAheadIsWeak() {
        int level = SlenderGaze.levelOf(SURVIVOR, new Pos(0, 40, 28));
        assertTrue(level >= 0 && level < SlenderGaze.LEVELS - 1, "expected a weak level, got " + level);
    }

    @Test
    @DisplayName("Out of range there is nothing, however clear the line")
    void beyondRangeIsNothing() {
        assertEquals(SlenderGaze.NONE, SlenderGaze.levelOf(SURVIVOR, new Pos(0, 40, 80)));
    }

    @Test
    @DisplayName("Standing behind a survivor does nothing, however close")
    void behindIsNothing() {
        assertEquals(SlenderGaze.NONE, SlenderGaze.levelOf(SURVIVOR, new Pos(0, 40, -4)),
                "the effect is about seeing him, not about him being there");
    }

    @Test
    @DisplayName("Just outside the corner of the eye does nothing either")
    void besideIsNothing() {
        assertEquals(SlenderGaze.NONE, SlenderGaze.levelOf(SURVIVOR, new Pos(6, 40, 0)));
    }

    @Test
    @DisplayName("Turning towards him brings it on")
    void turningTowardsHimBringsItOn() {
        Pos turned = new Pos(0, 40, 0, -90, 0);
        assertTrue(SlenderGaze.levelOf(turned, new Pos(6, 40, 0)) > SlenderGaze.NONE,
                "he is in front of the survivor now");
    }

    @Test
    @DisplayName("Closing in never weakens the effect")
    void levelIsMonotonic() {
        int previous = SlenderGaze.NONE;
        for (int distance = 40; distance >= 1; distance--) {
            int current = SlenderGaze.levelOf(SURVIVOR, new Pos(0, 40, distance));
            assertTrue(current >= previous, "the tearing eased off at distance " + distance);
            previous = current;
        }
    }
}
