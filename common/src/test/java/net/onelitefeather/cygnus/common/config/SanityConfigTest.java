package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanityConfigTest {

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaultsMatchTheDesign() {
        SanityConfig config = SanityConfig.DEFAULT;

        assertEquals(0.5D, config.pageFloorWeight());
        assertEquals(0.10D, config.pageFoundGain());
        assertEquals(0.10D, config.sightingGain());
        assertEquals(20, config.sightingCooldownSeconds());
        assertEquals(0.30D, config.caughtGain());
        assertEquals(0.25D, config.deathGain());
        assertEquals(0.005D, config.decayPerSecond());
        assertEquals(0.015D, config.stalkGainPerSecond());
        assertEquals(0.25D, config.residualShare());
        assertEquals(0.15D, config.timeFloorWeight());
        assertEquals(0.55D, config.floorCap());
    }

    @Test
    @DisplayName("Gains and the floor weight have to stay between 0 and 1")
    void sharesBetweenZeroAndOne() {
        assertRejected("pageFloorWeight", () -> new SanityConfig(1.5D, 0.1D, 0.1D, 20, 0.3D, 0.25D, 0.005D, 0.015D, 0.25D, 0.15D, 0.55D));
        assertRejected("pageFoundGain", () -> new SanityConfig(0.5D, -0.1D, 0.1D, 20, 0.3D, 0.25D, 0.005D, 0.015D, 0.25D, 0.15D, 0.55D));
        assertRejected("sightingGain", () -> new SanityConfig(0.5D, 0.1D, 1.1D, 20, 0.3D, 0.25D, 0.005D, 0.015D, 0.25D, 0.15D, 0.55D));
        assertRejected("caughtGain", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 20, 2.0D, 0.25D, 0.005D, 0.015D, 0.25D, 0.15D, 0.55D));
        assertRejected("deathGain", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 20, 0.3D, -1.0D, 0.005D, 0.015D, 0.25D, 0.15D, 0.55D));
        assertRejected("stalkGainPerSecond", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 20, 0.3D, 0.25D, 0.005D, -0.1D, 0.25D, 0.15D, 0.55D));
        assertRejected("residualShare", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 20, 0.3D, 0.25D, 0.005D, 0.015D, 1.5D, 0.15D, 0.55D));
        assertRejected("timeFloorWeight", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 20, 0.3D, 0.25D, 0.005D, 0.015D, 0.25D, -0.1D, 0.55D));
        assertRejected("floorCap", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 20, 0.3D, 0.25D, 0.005D, 0.015D, 0.25D, 0.15D, 1.2D));
    }

    @Test
    @DisplayName("The cooldown needs at least 1, the decay must not be negative")
    void lowerBounds() {
        assertRejected("sightingCooldownSeconds", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 0, 0.3D, 0.25D, 0.005D, 0.015D, 0.25D, 0.15D, 0.55D));
        assertRejected("decayPerSecond", () -> new SanityConfig(0.5D, 0.1D, 0.1D, 20, 0.3D, 0.25D, -0.001D, 0.015D, 0.25D, 0.15D, 0.55D));
    }

    private static void assertRejected(String field, Executable creation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, creation);
        assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }
}
