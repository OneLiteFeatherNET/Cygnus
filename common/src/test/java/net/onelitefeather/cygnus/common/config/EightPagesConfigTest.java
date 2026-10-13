package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EightPagesConfigTest {

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaults() {
        EightPagesConfig config = EightPagesConfig.DEFAULT;

        assertEquals(50, config.bonusOneSurvivor());
        assertEquals(25, config.bonusTwoSurvivors());
        assertEquals(0, config.bonusThreeSurvivors());
    }

    @Test
    @DisplayName("Each survivor count gets its own bonus and every other count none")
    void bonusForSurvivorCount() {
        EightPagesConfig config = new EightPagesConfig(30, 20, 10);

        assertEquals(0, config.bonusFor(0));
        assertEquals(30, config.bonusFor(1));
        assertEquals(20, config.bonusFor(2));
        assertEquals(10, config.bonusFor(3));
        assertEquals(0, config.bonusFor(4));
    }

    @Test
    @DisplayName("A bonus on a half heart is rounded to the heart above or below")
    void halfHeartBonusIsRoundedByChance() {
        EightPagesConfig config = EightPagesConfig.DEFAULT;

        assertEquals(30, config.wholeHeartBonusFor(2, coin(true)));
        assertEquals(20, config.wholeHeartBonusFor(2, coin(false)));
    }

    @Test
    @DisplayName("A bonus of whole hearts stays as it is")
    void wholeHeartBonusIsKept() {
        EightPagesConfig config = new EightPagesConfig(50, 20, 0);

        assertEquals(50, config.wholeHeartBonusFor(1, coin(true)));
        assertEquals(20, config.wholeHeartBonusFor(2, coin(true)));
        assertEquals(0, config.wholeHeartBonusFor(4, coin(true)));
    }

    @Test
    @DisplayName("A bonus must not be negative and has to be a multiple of 5")
    void valuesAreChecked() {
        assertRejected("bonusOneSurvivor", () -> new EightPagesConfig(-5, 25, 0));
        assertRejected("bonusOneSurvivor", () -> new EightPagesConfig(7, 25, 0));
        assertRejected("bonusTwoSurvivors", () -> new EightPagesConfig(50, 3, 0));
        assertRejected("bonusThreeSurvivors", () -> new EightPagesConfig(50, 25, 1));
        assertDoesNotThrow(() -> new EightPagesConfig(0, 0, 0));
    }

    private static Random coin(boolean up) {
        return new Random() {
            @Override
            public boolean nextBoolean() {
                return up;
            }
        };
    }

    private static void assertRejected(String field, Executable creation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, creation);
        assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }
}
