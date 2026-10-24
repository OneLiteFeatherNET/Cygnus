package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static net.onelitefeather.cygnus.common.config.ConfigAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LureConfigTest {

    private static final double EPSILON = 1.0E-9;

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaults() {
        LureConfig config = LureConfig.DEFAULT;

        assertTrue(config.enabled());
        assertEquals(30, config.aloneRadius());
        assertEquals(5, config.rollSeconds());
        assertEquals(0.08D, config.rollChance());
        assertEquals(15, config.minDistance());
        assertEquals(25, config.maxDistance());
        assertEquals(80, config.minAngle());
        assertEquals(130, config.maxAngle());
        assertEquals(3, config.maxHeightOffset());
        assertEquals(20, config.waitSeconds());
        assertEquals(700, config.gazeMillis());
        assertEquals(35, config.gazeAngle());
        assertEquals(0.30D, config.fearGain());
        assertEquals(0.20D, config.slownessWeight());
        assertEquals(0.15D, config.betrayalWeight());
        assertEquals(0.15D, config.blindnessWeight());
        assertEquals(4, config.effectSeconds());
        assertEquals(60, config.respiteSeconds());
        assertEquals(0.50D, config.nothingWeight(), EPSILON);
    }

    @Test
    @DisplayName("Contradicting distances, angles and weights and a chance outside 0 to 1 are rejected")
    void rejectsBrokenValues() {
        assertDoesNotThrow(() -> withAngles(75, 130));
        assertRejected("minAngle", () -> withAngles(130, 130));
        assertRejected("minDistance", () -> withDistances(25, 25));
        assertRejected("slownessWeight + betrayalWeight + blindnessWeight (1.1) must be at most 1",
                () -> withWeights(0.5D, 0.4D, 0.2D));
        assertRejected("rollChance", () -> new LureConfig(true, 30, 5, 1.5D, 15, 25, 80, 130, 3, 20, 700, 35,
                0.30D, 0.20D, 0.15D, 0.15D, 4, 60));
    }

    @Test
    @DisplayName("The keys carry the lure prefix, and a broken group falls back to the defaults")
    void readsKeysAndFallsBack() {
        Properties properties = new Properties();
        properties.setProperty("lure.rollChance", "0.2");
        assertEquals(0.2D, LureConfig.read(ConfigSection.root(properties).section("lure.")).rollChance());

        Properties broken = new Properties();
        broken.setProperty("lure.maxDistance", "5");
        assertEquals(LureConfig.DEFAULT, LureConfig.read(ConfigSection.root(broken).section("lure.")));
    }

    private static LureConfig withAngles(int minAngle, int maxAngle) {
        return new LureConfig(true, 30, 5, 0.08D, 15, 25, minAngle, maxAngle, 3, 20, 700, 35,
                0.30D, 0.20D, 0.15D, 0.15D, 4, 60);
    }

    private static LureConfig withDistances(int minDistance, int maxDistance) {
        return new LureConfig(true, 30, 5, 0.08D, minDistance, maxDistance, 80, 130, 3, 20, 700, 35,
                0.30D, 0.20D, 0.15D, 0.15D, 4, 60);
    }

    private static LureConfig withWeights(double slowness, double betrayal, double blindness) {
        return new LureConfig(true, 30, 5, 0.08D, 15, 25, 80, 130, 3, 20, 700, 35,
                0.30D, slowness, betrayal, blindness, 4, 60);
    }
}
