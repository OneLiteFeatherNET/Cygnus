package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryConfigTest {

    @Test
    @DisplayName("The default threshold is the 50 ms budget of a tick")
    void defaults() {
        assertEquals(50, TelemetryConfig.DEFAULT.slowTickThresholdMillis());
    }

    @Test
    @DisplayName("A threshold below 1 ms is rejected and names the field")
    void thresholdBelowOneIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new TelemetryConfig(0));
        assertTrue(exception.getMessage().contains("slowTickThresholdMillis"), exception.getMessage());
    }

    @Test
    @DisplayName("A threshold of exactly 1 ms is accepted")
    void thresholdOfOneIsAccepted() {
        assertDoesNotThrow(() -> new TelemetryConfig(1));
    }
}
