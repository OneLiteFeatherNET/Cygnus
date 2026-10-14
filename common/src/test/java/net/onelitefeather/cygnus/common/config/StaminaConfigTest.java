package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.onelitefeather.cygnus.common.config.ConfigAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StaminaConfigTest {

    @Test
    @DisplayName("The defaults are the values from the playtest")
    void defaults() {
        StaminaConfig config = StaminaConfig.DEFAULT;

        assertEquals(0.3D, config.sprintResumeShare());
        assertEquals(1.25D, config.regenPerSecond());
        assertEquals(5, config.slenderReappearCooldownSeconds());
        assertEquals(4, config.slenderDamageRange());
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

    @Test
    @DisplayName("The slender's damage range has to be between 1 and 16 blocks")
    void damageRangeBounds() {
        assertRejected("slenderDamageRange", () -> new StaminaConfig(0.3D, 1.25D, 5, 0));
        assertRejected("slenderDamageRange", () -> new StaminaConfig(0.3D, 1.25D, 5, 17));
        assertDoesNotThrow(() -> new StaminaConfig(0.3D, 1.25D, 5, 1));
        assertDoesNotThrow(() -> new StaminaConfig(0.3D, 1.25D, 5, 16));
    }
}
