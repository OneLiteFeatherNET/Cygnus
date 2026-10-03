package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaminaConfigTest {

    @Test
    @DisplayName("The defaults are the values from the playtest")
    void defaults() {
        StaminaConfig config = StaminaConfig.DEFAULT;

        assertEquals(0.3D, config.sprintResumeShare());
        assertEquals(1.25D, config.regenPerSecond());
        assertEquals(5, config.slenderReappearCooldownSeconds());
    }

    @Test
    @DisplayName("The resume share has to be above 0 and at most 1")
    void resumeShareRange() {
        assertRejected("sprintResumeShare", () -> new StaminaConfig(0.0D, 1.25D, 5));
        assertRejected("sprintResumeShare", () -> new StaminaConfig(1.1D, 1.25D, 5));
        assertDoesNotThrow(() -> new StaminaConfig(1.0D, 1.25D, 5));
    }

    @Test
    @DisplayName("The bar has to regenerate at all")
    void regenAboveZero() {
        assertRejected("regenPerSecond", () -> new StaminaConfig(0.3D, 0.0D, 5));
    }

    @Test
    @DisplayName("A cooldown of 0 turns it off, below that is rejected")
    void cooldownNotNegative() {
        assertRejected("slenderReappearCooldownSeconds", () -> new StaminaConfig(0.3D, 1.25D, -1));
        assertDoesNotThrow(() -> new StaminaConfig(0.3D, 1.25D, 0));
    }

    private static void assertRejected(String field, Executable creation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, creation);
        assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }
}
