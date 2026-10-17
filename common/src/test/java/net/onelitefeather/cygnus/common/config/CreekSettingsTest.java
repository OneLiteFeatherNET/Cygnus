package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.onelitefeather.cygnus.common.config.ConfigAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreekSettingsTest {

    @Test
    @DisplayName("The defaults are the values the creek used before maps had settings")
    void defaultsKeepTheOldValues() {
        assertEquals(new CreekSettings(48, 20, 35, 30, 3.0D, 15, 5.0D), CreekSettings.DEFAULT);
    }

    @Test
    @DisplayName("Every value is checked against its bounds")
    void boundsAreChecked() {
        assertRejected("sightRange", () -> new CreekSettings(0, 20, 35, 30, 3.0D, 15, 5.0D));
        assertRejected("sightRange", () -> new CreekSettings(97, 20, 35, 30, 3.0D, 15, 5.0D));
        assertRejected("stalkMaxDistance", () -> new CreekSettings(48, 20, 65, 30, 3.0D, 15, 5.0D));
        assertRejected("respawnMinDistance", () -> new CreekSettings(48, 20, 35, 0, 3.0D, 15, 5.0D));
        assertRejected("respawnMinDistance", () -> new CreekSettings(48, 20, 35, 129, 3.0D, 15, 5.0D));
        assertRejected("routeLinkDistance", () -> new CreekSettings(48, 20, 35, 30, 0.4D, 15, 5.0D));
        assertRejected("routeLinkDistance", () -> new CreekSettings(48, 20, 35, 30, 16.1D, 15, 5.0D));
        assertRejected("personalSpace", () -> new CreekSettings(48, 40, 50, 30, 3.0D, 33, 5.0D));
        assertRejected("launchHeight", () -> new CreekSettings(48, 20, 35, 30, 3.0D, 15, -0.1D));
        assertRejected("launchHeight", () -> new CreekSettings(48, 20, 35, 30, 3.0D, 15, 20.1D));
    }

    @Test
    @DisplayName("The personal space has to fit inside the stalk band")
    void personalSpaceInsideTheBand() {
        assertRejected("stalkMinDistance", () -> new CreekSettings(48, 15, 35, 30, 3.0D, 15, 5.0D));
    }

    @Test
    @DisplayName("The stalk band must not run backwards")
    void stalkBandForwards() {
        assertRejected("stalkMaxDistance", () -> new CreekSettings(48, 35, 35, 30, 3.0D, 15, 5.0D));
    }

    @Test
    @DisplayName("A launch height of 0 turns the launch off and is allowed")
    void zeroLaunchHeightIsAllowed() {
        assertDoesNotThrow(() -> new CreekSettings(48, 20, 35, 30, 3.0D, 15, 0.0D));
    }
}
