package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.onelitefeather.cygnus.common.config.ConfigAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RegenerationConfigTest {

    private static final double EPSILON = 1.0E-9;

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaults() {
        RegenerationConfig config = RegenerationConfig.DEFAULT;

        assertEquals(4.0D, config.perSecond());
        assertEquals(List.of(0.40D, 0.30D, 0.25D, 0.20D), config.shares());
        assertEquals(10, config.minMissing());
        assertEquals(0.5D, config.minSpeed());
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
    @DisplayName("Fear slows the healing down to the lowest speed, but never further")
    void speedFallsWithFear() {
        RegenerationConfig config = RegenerationConfig.DEFAULT;

        assertEquals(4.0D, config.speedFor(0.0D), EPSILON);
        assertEquals(3.0D, config.speedFor(0.5D), EPSILON);
        assertEquals(2.0D, config.speedFor(1.0D), EPSILON);
        assertEquals(2.0D, config.speedFor(1.5D), EPSILON);
        assertEquals(4.0D, config.speedFor(-0.5D), EPSILON);
    }

    @Test
    @DisplayName("Without a lowest speed fear can stop the healing, at 1 it plays no part")
    void minSpeedBounds() {
        assertEquals(0.0D, new RegenerationConfig(4.0D, List.of(0.4D), 10, 0.0D).speedFor(1.0D), EPSILON);
        assertEquals(4.0D, new RegenerationConfig(4.0D, List.of(0.4D), 10, 1.0D).speedFor(1.0D), EPSILON);
    }

    @Test
    @DisplayName("A negative rate, an empty list, shares outside 0 to 1 and no minimum injury and a lowest speed outside 0 to 1 are rejected")
    void valuesAreChecked() {
        assertRejected("perSecond", () -> new RegenerationConfig(-1.0D, List.of(0.4D), 10, 0.5D));
        assertRejected("shares", () -> new RegenerationConfig(4.0D, List.of(), 10, 0.5D));
        assertRejected("shares", () -> new RegenerationConfig(4.0D, List.of(0.4D, 1.5D), 10, 0.5D));
        assertRejected("minMissing", () -> new RegenerationConfig(4.0D, List.of(0.4D), 0, 0.5D));
        assertRejected("minSpeed", () -> new RegenerationConfig(4.0D, List.of(0.4D), 10, -0.1D));
        assertRejected("minSpeed", () -> new RegenerationConfig(4.0D, List.of(0.4D), 10, 1.1D));
        assertDoesNotThrow(() -> new RegenerationConfig(0.0D, List.of(0.0D), 1, 0.0D));
    }
}
