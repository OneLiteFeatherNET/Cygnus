package net.onelitefeather.cygnus.lure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LureOutcomeTest {

    @Test
    @DisplayName("The roll falls into slowness, betrayal, blindness and then nothing, as wide as their weights")
    void bandsFollowTheWeights() {
        assertEquals(LureOutcome.SLOWNESS, LureOutcome.roll(0.0D));
        assertEquals(LureOutcome.SLOWNESS, LureOutcome.roll(0.199D));
        assertEquals(LureOutcome.BETRAYAL, LureOutcome.roll(0.20D));
        assertEquals(LureOutcome.BETRAYAL, LureOutcome.roll(0.349D));
        assertEquals(LureOutcome.BLINDNESS, LureOutcome.roll(0.35D));
        assertEquals(LureOutcome.BLINDNESS, LureOutcome.roll(0.499D));
        assertEquals(LureOutcome.NOTHING, LureOutcome.roll(0.50D));
        assertEquals(LureOutcome.NOTHING, LureOutcome.roll(0.999D));
    }
}
