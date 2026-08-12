package net.onelitefeather.cygnus.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link Helper#clamp(int, int, int)} and {@link Helper#clamp(double, double, double)},
 * which replace the hand-rolled {@code Math.min(hi, Math.max(lo, x))} scattered across the tunnel
 * vision and slender gaze code.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class HelperTest {

    @Test
    @DisplayName("An int within bounds is returned unchanged")
    void intWithinBoundsIsUnchanged() {
        assertEquals(5, Helper.clamp(5, 0, 10));
    }

    @Test
    @DisplayName("An int below the lower bound is raised to it")
    void intBelowLowerBoundIsRaised() {
        assertEquals(0, Helper.clamp(-5, 0, 10));
    }

    @Test
    @DisplayName("An int above the upper bound is lowered to it")
    void intAboveUpperBoundIsLowered() {
        assertEquals(10, Helper.clamp(15, 0, 10));
    }

    @Test
    @DisplayName("A double within bounds is returned unchanged")
    void doubleWithinBoundsIsUnchanged() {
        assertEquals(0.5D, Helper.clamp(0.5D, 0.0D, 1.0D));
    }

    @Test
    @DisplayName("A double below the lower bound is raised to it")
    void doubleBelowLowerBoundIsRaised() {
        assertEquals(0.0D, Helper.clamp(-0.5D, 0.0D, 1.0D));
    }

    @Test
    @DisplayName("A double above the upper bound is lowered to it")
    void doubleAboveUpperBoundIsLowered() {
        assertEquals(1.0D, Helper.clamp(1.5D, 0.0D, 1.0D));
    }
}
