package net.onelitefeather.cygnus.lure;

import net.onelitefeather.cygnus.common.config.LureConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LureOutcomeTest {

    @Test
    @DisplayName("The roll falls into slowness, betrayal, blindness and then nothing, as wide as their weights")
    void bandsFollowTheWeights() {
        LureConfig config = LureConfig.DEFAULT;

        assertEquals(LureOutcome.SLOWNESS, LureOutcome.roll(config, 0.0D));
        assertEquals(LureOutcome.SLOWNESS, LureOutcome.roll(config, 0.199D));
        assertEquals(LureOutcome.BETRAYAL, LureOutcome.roll(config, 0.20D));
        assertEquals(LureOutcome.BETRAYAL, LureOutcome.roll(config, 0.349D));
        assertEquals(LureOutcome.BLINDNESS, LureOutcome.roll(config, 0.35D));
        assertEquals(LureOutcome.BLINDNESS, LureOutcome.roll(config, 0.499D));
        assertEquals(LureOutcome.NOTHING, LureOutcome.roll(config, 0.50D));
        assertEquals(LureOutcome.NOTHING, LureOutcome.roll(config, 0.999D));
    }
}
