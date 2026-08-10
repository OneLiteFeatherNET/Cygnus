package net.onelitefeather.cygnus.tunnelvision;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the intensity curves that drive the survivor's tunnel vision.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class TunnelVisionIntensityTest {

    private static final double DELTA = 1.0E-6D;

    /** Survivor standing in the origin, looking towards positive Z. */
    private static final Pos SURVIVOR = new Pos(0, 0, 0, 0, 0);

    @DisplayName("Stamina above half a bar produces no tunnel vision")
    @ParameterizedTest
    @CsvSource({"1.0", "0.75", "0.5"})
    void staminaAboveHalfIsCalm(double stamina) {
        assertEquals(0.0D, TunnelVisionIntensity.fromStamina(stamina), DELTA);
    }

    @Test
    @DisplayName("An empty stamina bar produces full intensity")
    void emptyStaminaIsFull() {
        assertEquals(1.0D, TunnelVisionIntensity.fromStamina(0.0D), DELTA);
    }

    @Test
    @DisplayName("The stamina curve accelerates towards the empty bar")
    void staminaCurveIsQuadratic() {
        assertEquals(0.25D, TunnelVisionIntensity.fromStamina(0.25D), DELTA);
    }

    @Test
    @DisplayName("Draining stamina never lowers the intensity")
    void staminaIsMonotonic() {
        double previous = -1.0D;
        for (int step = 20; step >= 0; step--) {
            double current = TunnelVisionIntensity.fromStamina(step / 20.0D);
            assertTrue(current >= previous, "intensity dropped at stamina " + step / 20.0D);
            previous = current;
        }
    }

    @Test
    @DisplayName("A slender beyond the outer radius stays unnoticed")
    void distantSlenderIsUnnoticed() {
        assertEquals(0.0D, TunnelVisionIntensity.fromSlender(SURVIVOR, new Pos(0, 0, 25)), DELTA);
    }

    @Test
    @DisplayName("Looking straight at a nearby slender produces full intensity")
    void facingNearbySlenderIsFull() {
        assertEquals(1.0D, TunnelVisionIntensity.fromSlender(SURVIVOR, new Pos(0, 0, 5)), DELTA);
    }

    @Test
    @DisplayName("A slender in the back is dampened by the view factor")
    void slenderBehindIsDampened() {
        assertEquals(0.6D, TunnelVisionIntensity.fromSlender(SURVIVOR, new Pos(0, 0, -5)), DELTA);
    }

    @Test
    @DisplayName("Approaching the slender never lowers the intensity")
    void slenderIsMonotonic() {
        double previous = -1.0D;
        for (int distance = 30; distance >= 1; distance--) {
            double current = TunnelVisionIntensity.fromSlender(SURVIVOR, new Pos(0, 0, distance));
            assertTrue(current >= previous, "intensity dropped at distance " + distance);
            previous = current;
        }
    }

    @Test
    @DisplayName("Without either source the combination is calm")
    void combinationOfNothingIsCalm() {
        assertEquals(0.0D, TunnelVisionIntensity.combine(0.0D, 0.0D), DELTA);
    }

    @Test
    @DisplayName("A saturated source saturates the combination")
    void combinationSaturates() {
        assertEquals(1.0D, TunnelVisionIntensity.combine(1.0D, 0.3D), DELTA);
    }

    @Test
    @DisplayName("Both sources add up without exceeding the maximum")
    void combinationAddsUp() {
        assertEquals(0.75D, TunnelVisionIntensity.combine(0.5D, 0.5D), DELTA);
    }
}
