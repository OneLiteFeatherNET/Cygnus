package net.onelitefeather.cygnus.gaze;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies when the sight of the slender starts to tear a survivor's view apart.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
class SlenderGazeTest {

    /** A survivor in the origin looking towards positive Z, which is a yaw of zero. */
    private static final Pos SURVIVOR = new Pos(0, 40, 0, 0, 0);

    /** The shipped defaults: 12 blocks of reach, worst at 4, within 30 degrees of the view. */
    private static final SlenderGaze GAZE = new SlenderGaze(12, 4, 30);

    @Test
    @DisplayName("A slender straight ahead and close tears the view apart")
    void closeAndAheadIsStrongest() {
        assertEquals(SlenderGaze.LEVELS - 1, GAZE.levelOf(SURVIVOR, new Pos(0, 40, 4)));
    }

    @Test
    @DisplayName("A slender at the very edge of the range barely registers")
    void atTheEdgeOfRangeIsWeak() {
        int level = GAZE.levelOf(SURVIVOR, new Pos(0, 40, 12));
        assertTrue(level >= 0 && level < SlenderGaze.LEVELS - 1, "expected a weak level, got " + level);
    }

    @Test
    @DisplayName("One block past the range there is nothing at all")
    void justBeyondRangeIsNothing() {
        assertEquals(SlenderGaze.NONE, GAZE.levelOf(SURVIVOR, new Pos(0, 40, 13)),
                "beyond the range the screen has to be clean, not merely faint");
    }

    @Test
    @DisplayName("Out of range there is nothing, however clear the line")
    void beyondRangeIsNothing() {
        assertEquals(SlenderGaze.NONE, GAZE.levelOf(SURVIVOR, new Pos(0, 40, 80)));
    }

    @Test
    @DisplayName("Standing behind a survivor does nothing, however close")
    void behindIsNothing() {
        assertEquals(SlenderGaze.NONE, GAZE.levelOf(SURVIVOR, new Pos(0, 40, -4)),
                "the effect is about seeing him, not about him being there");
    }

    @Test
    @DisplayName("Just outside the corner of the eye does nothing either")
    void besideIsNothing() {
        assertEquals(SlenderGaze.NONE, GAZE.levelOf(SURVIVOR, new Pos(6, 40, 0)));
    }

    @Test
    @DisplayName("Turning towards him brings it on")
    void turningTowardsHimBringsItOn() {
        Pos turned = new Pos(0, 40, 0, -90, 0);
        assertTrue(GAZE.levelOf(turned, new Pos(6, 40, 0)) > SlenderGaze.NONE,
                "he is in front of the survivor now");
    }

    @Test
    @DisplayName("Closing in never weakens the effect")
    void levelIsMonotonic() {
        int previous = SlenderGaze.NONE;
        for (int distance = 40; distance >= 1; distance--) {
            int current = GAZE.levelOf(SURVIVOR, new Pos(0, 40, distance));
            assertTrue(current >= previous, "the tearing eased off at distance " + distance);
            previous = current;
        }
    }

    @Test
    @DisplayName("At the edge of the view angle he counts as seen, past it he does not")
    void viewAngleDecidesWhatCountsAsSeen() {
        // Six blocks ahead, then stepped sideways. Three blocks across is about 26 degrees off the
        // line of sight and inside a 30 degree cone; four is about 34 degrees and outside it.
        assertTrue(GAZE.levelOf(SURVIVOR, new Pos(3, 40, 6)) > SlenderGaze.NONE,
                "he is still within 30 degrees of where they are looking");
        assertEquals(SlenderGaze.NONE, GAZE.levelOf(SURVIVOR, new Pos(4, 40, 6)),
                "past the cone he is on screen but not being looked at");
    }

    @Test
    @DisplayName("A wider cone catches what a narrow one lets through")
    void theViewAngleIsHonoured() {
        Pos offToTheSide = new Pos(4, 40, 6);
        SlenderGaze wide = new SlenderGaze(12, 4, 60);
        assertEquals(SlenderGaze.NONE, GAZE.levelOf(SURVIVOR, offToTheSide));
        assertNotEquals(SlenderGaze.NONE, wide.levelOf(SURVIVOR, offToTheSide),
                "the configured angle has to reach the decision, not just be stored");
    }

    @Test
    @DisplayName("A wider range reaches further than a narrow one")
    void theRangeIsHonoured() {
        Pos wellAhead = new Pos(0, 40, 20);
        SlenderGaze far = new SlenderGaze(32, 6, 30);
        assertEquals(SlenderGaze.NONE, GAZE.levelOf(SURVIVOR, wellAhead));
        assertNotEquals(SlenderGaze.NONE, far.levelOf(SURVIVOR, wellAhead),
                "the configured range has to reach the decision, not just be stored");
    }

    @Test
    @DisplayName("A close range that meets the range is refused")
    void closeRangeMustStayBelowTheRange() {
        assertThrows(IllegalArgumentException.class, () -> new SlenderGaze(12, 12, 30),
                "the slope between the two would divide by zero");
        assertThrows(IllegalArgumentException.class, () -> new SlenderGaze(12, 16, 30),
                "the slope between the two would run backwards");
    }
}
