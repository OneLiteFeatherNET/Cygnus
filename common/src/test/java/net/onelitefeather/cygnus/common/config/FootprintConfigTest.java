package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FootprintConfigTest {

    private static final FootprintConfig D = FootprintConfig.DEFAULT;

    @Test
    @DisplayName("The defaults are the start values from the design")
    void defaults() {
        assertEquals(3.0D, D.slenderStepBlocks());
        assertEquals(0.25D, D.slenderChance());
        assertEquals(1000, D.slenderDelayMinMillis());
        assertEquals(2000, D.slenderDelayMaxMillis());
        assertEquals(12, D.slenderLifetimeSeconds());
        assertEquals(2.0D, D.survivorSampleBlocks());
        assertEquals(30, D.survivorHistorySeconds());
        assertEquals(40, D.scanRadius());
        assertEquals(5, D.scanGapSeconds());
        assertEquals(40, D.scanMaxPrints());
        assertEquals(15, D.scanLifetimeSeconds());
        assertEquals(120, D.scanCooldownSeconds());
        assertEquals(8.0D, D.teleportBlocks());
        assertEquals(0.7D, D.minSpacing());
        assertEquals(0.33D, D.fadeShare());
    }

    @Test
    @DisplayName("The fade share has to lie between 0 and 1")
    void fadeShareRange() {
        assertRejected("fadeShare", () -> withFadeShare(-0.1D));
        assertRejected("fadeShare", () -> withFadeShare(1.1D));
        assertDoesNotThrow(() -> withFadeShare(0.0D));
        assertDoesNotThrow(() -> withFadeShare(1.0D));
    }

    @Test
    @DisplayName("The chance has to be above 0 and at most 1")
    void chanceRange() {
        assertRejected("slenderChance", () -> withChance(0.0D));
        assertRejected("slenderChance", () -> withChance(1.5D));
        assertDoesNotThrow(() -> withChance(1.0D));
    }

    @Test
    @DisplayName("The longest delay may not be shorter than the shortest")
    void delayOrder() {
        assertRejected("slenderDelayMaxMillis", () -> new FootprintConfig(3.0D, 0.25D, 2000, 1000, 12,
                2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D));
        assertRejected("slenderDelayMinMillis", () -> new FootprintConfig(3.0D, 0.25D, -1, 1000, 12,
                2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D));
    }

    @Test
    @DisplayName("The gap has to be shorter than the history, or a scan never finds anything")
    void gapShorterThanHistory() {
        assertRejected("scanGapSeconds", () -> new FootprintConfig(3.0D, 0.25D, 1000, 2000, 12,
                2.0D, 30, 40, 30, 40, 15, 120, 8.0D, 0.7D, 0.33D));
    }

    @Test
    @DisplayName("A teleport has to be longer than a step and a sample")
    void teleportLongerThanSteps() {
        assertRejected("teleportBlocks", () -> new FootprintConfig(3.0D, 0.25D, 1000, 2000, 12,
                2.0D, 30, 40, 5, 40, 15, 120, 3.0D, 0.7D, 0.33D));
    }

    @Test
    @DisplayName("Counts and durations need at least 1")
    void lowerBounds() {
        assertRejected("slenderLifetimeSeconds", () -> new FootprintConfig(3.0D, 0.25D, 1000, 2000, 0,
                2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D));
        assertRejected("scanRadius", () -> new FootprintConfig(3.0D, 0.25D, 1000, 2000, 12,
                2.0D, 30, 0, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D));
        assertRejected("scanMaxPrints", () -> new FootprintConfig(3.0D, 0.25D, 1000, 2000, 12,
                2.0D, 30, 40, 5, 0, 15, 120, 8.0D, 0.7D, 0.33D));
        assertRejected("scanCooldownSeconds", () -> new FootprintConfig(3.0D, 0.25D, 1000, 2000, 12,
                2.0D, 30, 40, 5, 40, 15, -1, 8.0D, 0.7D, 0.33D));
        assertRejected("slenderStepBlocks", () -> new FootprintConfig(0.0D, 0.25D, 1000, 2000, 12,
                2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D));
    }

    private static FootprintConfig withChance(double chance) {
        return new FootprintConfig(3.0D, chance, 1000, 2000, 12, 2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D);
    }

    private static FootprintConfig withFadeShare(double fadeShare) {
        return new FootprintConfig(3.0D, 0.25D, 1000, 2000, 12, 2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, fadeShare);
    }

    private static void assertRejected(String field, Executable executable) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertTrue(exception.getMessage().startsWith(field), exception.getMessage());
    }
}
