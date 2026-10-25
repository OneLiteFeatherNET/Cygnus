package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static net.onelitefeather.cygnus.common.config.ConfigAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LureConfigTest {

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaults() {
        LureConfig config = LureConfig.DEFAULT;

        assertTrue(config.enabled());
        assertEquals(30, config.aloneRadius());
        assertEquals(0.08D, config.rollChance());
        assertEquals(0.30D, config.fearGain());
        assertEquals(4, config.effectSeconds());
        assertEquals(60, config.respiteSeconds());
    }

    @Test
    @DisplayName("No radius, a chance or gain outside 0 to 1, no follow-up and a negative respite are rejected")
    void rejectsBrokenValues() {
        assertRejected("aloneRadius", () -> new LureConfig(true, 0, 0.08D, 0.30D, 4, 60));
        assertRejected("rollChance", () -> new LureConfig(true, 30, 1.5D, 0.30D, 4, 60));
        assertRejected("fearGain", () -> new LureConfig(true, 30, 0.08D, -0.1D, 4, 60));
        assertRejected("effectSeconds", () -> new LureConfig(true, 30, 0.08D, 0.30D, 0, 60));
        assertRejected("respiteSeconds", () -> new LureConfig(true, 30, 0.08D, 0.30D, 4, -1));
        assertDoesNotThrow(() -> new LureConfig(false, 1, 0.0D, 0.0D, 1, 0));
    }

    @Test
    @DisplayName("The keys carry the lure prefix, and a broken group falls back to the defaults")
    void readsKeysAndFallsBack() {
        Properties properties = new Properties();
        properties.setProperty("lure.rollChance", "0.2");
        assertEquals(0.2D, LureConfig.read(ConfigSection.root(properties).section("lure.")).rollChance());

        Properties broken = new Properties();
        broken.setProperty("lure.rollChance", "5");
        assertEquals(LureConfig.DEFAULT, LureConfig.read(ConfigSection.root(broken).section("lure.")));
    }
}
