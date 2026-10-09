package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessionConfigTest {

    @Test
    @DisplayName("The possession lasts 8 seconds, has a 60 second cooldown and widens the sight by half")
    void defaultsMatchTheDesign() {
        assertEquals(8, PossessionConfig.DEFAULT.maxSeconds());
        assertEquals(60, PossessionConfig.DEFAULT.cooldownSeconds());
        assertEquals(1.5D, PossessionConfig.DEFAULT.sightFactor());
    }

    @Test
    @DisplayName("The possession needs a duration above 0, no negative cooldown and a factor of at least 1")
    void valuesAreChecked() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new PossessionConfig(0, 60, 1.5D))
                .getMessage().contains("maxSeconds"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new PossessionConfig(8, -1, 1.5D))
                .getMessage().contains("cooldownSeconds"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new PossessionConfig(8, 60, 0.9D))
                .getMessage().contains("sightFactor"));
        new PossessionConfig(1, 0, 1.0D);
    }
}
