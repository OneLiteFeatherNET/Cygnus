package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegenerationConfigTest {

    private static final double EPSILON = 1.0E-9;

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaults() {
        RegenerationConfig config = RegenerationConfig.DEFAULT;

        assertEquals(3, config.delaySeconds());
        assertEquals(4.0D, config.perSecond());
        assertEquals(List.of(0.40D, 0.30D, 0.25D, 0.20D), config.shares());
    }

    @Test
    @DisplayName("Every three survivors take the next share, the last one holds for every bigger round")
    void shareShrinksWithTheRound() {
        RegenerationConfig config = RegenerationConfig.DEFAULT;

        assertEquals(0.40D, config.shareFor(0), EPSILON);
        assertEquals(0.40D, config.shareFor(3), EPSILON);
        assertEquals(0.30D, config.shareFor(4), EPSILON);
        assertEquals(0.25D, config.shareFor(9), EPSILON);
        assertEquals(0.20D, config.shareFor(10), EPSILON);
        assertEquals(0.20D, config.shareFor(30), EPSILON);
    }

    @Test
    @DisplayName("Negative values, an empty list and shares outside 0 to 1 are rejected")
    void valuesAreChecked() {
        assertRejected("delaySeconds", () -> new RegenerationConfig(-1, 4.0D, List.of(0.4D)));
        assertRejected("perSecond", () -> new RegenerationConfig(3, -1.0D, List.of(0.4D)));
        assertRejected("shares", () -> new RegenerationConfig(3, 4.0D, List.of()));
        assertRejected("shares", () -> new RegenerationConfig(3, 4.0D, List.of(0.4D, 1.5D)));
        assertDoesNotThrow(() -> new RegenerationConfig(0, 0.0D, List.of(0.0D)));
    }

    private static void assertRejected(String field, Executable creation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, creation);
        assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }
}
