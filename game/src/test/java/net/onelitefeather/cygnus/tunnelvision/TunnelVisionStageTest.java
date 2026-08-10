package net.onelitefeather.cygnus.tunnelvision;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how a continuous intensity becomes the discrete, pulsing stage the overlay renders.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class TunnelVisionStageTest {

    /** Enough updates to cover several periods of the slowest heartbeat. */
    private static final int SAMPLES = 60;

    @Test
    @DisplayName("Without any threat the overlay stays off")
    void calmIntensityStaysOff() {
        TunnelVisionStage stage = new TunnelVisionStage();
        assertEquals(0, stage.update(0.0D));
    }

    @Test
    @DisplayName("Full intensity pulses between the last two stages")
    void fullIntensityPulses() {
        TunnelVisionStage stage = new TunnelVisionStage();
        int lowest = TunnelVisionStage.MAX_STAGE;
        int highest = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            int current = stage.update(1.0D);
            lowest = Math.min(lowest, current);
            highest = Math.max(highest, current);
        }
        assertEquals(TunnelVisionStage.MAX_STAGE, highest, "the pulse never reaches the peak");
        assertEquals(TunnelVisionStage.MAX_STAGE - 1, lowest, "the pulse does not open up again");
    }

    @Test
    @DisplayName("Low intensity barely pulses at all")
    void lowIntensityIsSteady() {
        TunnelVisionStage stage = new TunnelVisionStage();
        int first = stage.update(0.125D);
        for (int sample = 0; sample < SAMPLES; sample++) {
            assertEquals(first, stage.update(0.125D), "a barely threatened survivor should not flicker");
        }
    }

    @Test
    @DisplayName("A small fluctuation does not move the stage")
    void hysteresisHoldsTheStage() {
        TunnelVisionStage stage = new TunnelVisionStage();
        int settled = highestOver(stage, 0.5D);
        assertEquals(4, settled, "half intensity should settle on the middle stage");
        assertEquals(settled, highestOver(stage, 0.55D), "the stage moved on a small fluctuation");
    }

    @Test
    @DisplayName("A real change moves the stage")
    void largerChangeMovesTheStage() {
        TunnelVisionStage stage = new TunnelVisionStage();
        assertEquals(4, highestOver(stage, 0.5D));
        assertEquals(5, highestOver(stage, 0.6D), "the stage should follow a real change");
    }

    @Test
    @DisplayName("The stage never leaves its bounds")
    void stageStaysWithinBounds() {
        TunnelVisionStage stage = new TunnelVisionStage();
        for (int sample = 0; sample < SAMPLES; sample++) {
            int current = stage.update(sample % 2 == 0 ? 1.0D : 0.0D);
            assertTrue(current >= 0 && current <= TunnelVisionStage.MAX_STAGE, "stage out of bounds: " + current);
        }
    }

    /**
     * Feeds a constant intensity for a while and reports the highest stage seen, which is the
     * stage the pulse starts from.
     *
     * @param stage    the stage state to drive
     * @param combined the constant intensity to feed
     * @return the highest stage observed
     */
    private int highestOver(TunnelVisionStage stage, double combined) {
        int highest = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            highest = Math.max(highest, stage.update(combined));
        }
        return highest;
    }
}
