package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@code pass.*} keys of the slender pass earning. Every test builds its own properties.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class SlenderPassConfigTest {

    @Test
    void theDefaultsAreTenSurvivorRoundsAndACapOfFive() {
        assertEquals(new SlenderPassConfig(10, 5), read(new Properties()));
    }

    @Test
    void theConfiguredValuesAreRead() {
        Properties properties = new Properties();
        properties.setProperty("pass.pity-rounds", "4");
        properties.setProperty("pass.max-balance", "2");

        assertEquals(new SlenderPassConfig(4, 2), read(properties));
    }

    @Test
    void aPityThresholdBelowOneIsRefusedWithItsKey() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new SlenderPassConfig(0, 5));

        assertTrue(error.getMessage().contains("pass.pity-rounds (0) must be at least 1"), error.getMessage());
    }

    @Test
    void aNegativeCapIsRefusedWithItsKey() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new SlenderPassConfig(10, -1));

        assertTrue(error.getMessage().contains("pass.max-balance"), error.getMessage());
    }

    @Test
    void anInvalidValueFromTheFileFailsTheStartInsteadOfFallingBackToTheDefault() {
        Properties properties = new Properties();
        properties.setProperty("pass.pity-rounds", "0");

        assertThrows(IllegalArgumentException.class, () -> read(properties));
    }

    private static SlenderPassConfig read(Properties properties) {
        return SlenderPassConfig.read(ConfigSection.root(properties).section("pass."));
    }
}
