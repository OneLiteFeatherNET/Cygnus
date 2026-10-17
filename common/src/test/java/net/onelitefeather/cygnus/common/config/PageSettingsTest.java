package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.onelitefeather.cygnus.common.config.ConfigAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PageSettingsTest {

    @Test
    @DisplayName("The defaults are the values the game used before maps had settings")
    void defaultsKeepTheOldValues() {
        assertEquals(new PageSettings(20, 4, 60, 15), PageSettings.DEFAULT);
    }

    @Test
    @DisplayName("Every value is checked against its bounds")
    void boundsAreChecked() {
        assertRejected("proximityRange", () -> new PageSettings(0, 4, 60, 15));
        assertRejected("proximityRange", () -> new PageSettings(65, 4, 60, 15));
        assertRejected("minActivePages", () -> new PageSettings(20, 0, 60, 15));
        assertRejected("minActivePages", () -> new PageSettings(20, 17, 60, 15));
        assertRejected("ttlSeconds", () -> new PageSettings(20, 4, 9, 15));
        assertRejected("ttlSeconds", () -> new PageSettings(20, 4, 301, 15));
        assertRejected("respawnDelaySeconds", () -> new PageSettings(20, 4, 60, 121));
    }

    @Test
    @DisplayName("The respawn delay never drops below the jitter, so a rolled delay is never negative")
    void respawnDelayStaysAboveTheJitter() {
        int jitter = GameConfig.PAGE_RESPAWN_DELAY_JITTER;
        assertRejected("respawnDelaySeconds", () -> new PageSettings(20, 4, 60, jitter - 1));
        assertDoesNotThrow(() -> new PageSettings(20, 4, 60, jitter));
    }

    @Test
    @DisplayName("The bounds themselves are allowed")
    void boundsAreInclusive() {
        assertDoesNotThrow(() -> new PageSettings(1, 1, PageSettings.MIN_TTL_SECONDS, PageSettings.MAX_RESPAWN_DELAY_SECONDS));
        assertDoesNotThrow(() -> new PageSettings(PageSettings.MAX_PROXIMITY_RANGE, PageSettings.MAX_MIN_ACTIVE_PAGES,
                PageSettings.MAX_TTL_SECONDS, 15));
    }
}
