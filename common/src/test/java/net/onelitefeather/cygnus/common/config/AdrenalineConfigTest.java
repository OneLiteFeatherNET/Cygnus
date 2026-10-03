package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdrenalineConfigTest {

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaults() {
        AdrenalineConfig config = AdrenalineConfig.DEFAULT;

        assertEquals(8, config.radius());
        assertEquals(0.2D, config.speedBonus());
        assertEquals(4, config.durationSeconds());
        assertEquals(20, config.cooldownSeconds());
    }

    @Test
    @DisplayName("Radius and duration need at least 1")
    void lowerBounds() {
        assertRejected("radius", () -> new AdrenalineConfig(0, 0.2D, 4, 20));
        assertRejected("durationSeconds", () -> new AdrenalineConfig(8, 0.2D, 0, 20));
    }

    @Test
    @DisplayName("The bonus has to be above 0 and at most 1")
    void bonusRange() {
        assertRejected("speedBonus", () -> new AdrenalineConfig(8, 0.0D, 4, 20));
        assertRejected("speedBonus", () -> new AdrenalineConfig(8, 1.5D, 4, 20));
    }

    @Test
    @DisplayName("A cooldown of 0 is allowed, below that is rejected")
    void cooldownNotNegative() {
        assertRejected("cooldownSeconds", () -> new AdrenalineConfig(8, 0.2D, 4, -1));
        assertDoesNotThrow(() -> new AdrenalineConfig(8, 0.2D, 4, 0));
    }

    private static void assertRejected(String field, Executable creation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, creation);
        assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }
}
