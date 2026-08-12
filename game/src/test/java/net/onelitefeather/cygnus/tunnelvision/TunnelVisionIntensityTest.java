package net.onelitefeather.cygnus.tunnelvision;

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
}
