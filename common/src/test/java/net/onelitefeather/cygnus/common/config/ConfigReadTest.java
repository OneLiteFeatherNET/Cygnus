package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigReadTest {

    private static ConfigSection section(String prefix, String... pairs) {
        Properties properties = new Properties();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.setProperty(pairs[i], pairs[i + 1]);
        }
        return ConfigSection.root(properties).section(prefix);
    }

    @Test
    @DisplayName("Without any keys every group reads its defaults")
    void emptyGivesDefaults() {
        assertEquals(CreekConfig.DEFAULT, CreekConfig.read(section("creek.")));
        assertEquals(SanityConfig.DEFAULT, SanityConfig.read(section("sanity.")));
        assertEquals(StaminaConfig.DEFAULT, StaminaConfig.read(section("stamina.")));
        assertEquals(AdrenalineConfig.DEFAULT, AdrenalineConfig.read(section("adrenaline.")));
        assertEquals(FootprintConfig.DEFAULT, FootprintConfig.read(section("footprint.")));
        assertEquals(PossessionConfig.DEFAULT, PossessionConfig.read(section("possession.")));
        assertEquals(TelemetryConfig.DEFAULT, TelemetryConfig.read(section("telemetry.")));
        assertEquals(MinimapConfig.DEFAULT, MinimapConfig.read(section("minimap.")));
        assertEquals(EightPagesConfig.DEFAULT, EightPagesConfig.read(section("eightPages.")));
        assertEquals(RegenerationConfig.DEFAULT, RegenerationConfig.read(section("regeneration.")));
    }

    @Test
    @DisplayName("Each group reads its keys under its own prefix")
    void readsUnderTheGroupPrefix() {
        assertEquals(0.7D, CreekConfig.read(section("creek.", "creek.huntThreshold", "0.7")).hunt().threshold(), 1.0E-9);
        assertEquals(0.4D, SanityConfig.read(section("sanity.", "sanity.caughtGain", "0.4")).caughtGain(), 1.0E-9);
        assertEquals(7, StaminaConfig.read(section("stamina.", "stamina.slenderReappearCooldownSeconds", "7")).slenderReappearCooldownSeconds());
        assertEquals(12, AdrenalineConfig.read(section("adrenaline.", "adrenaline.radius", "12")).radius());
        assertEquals(50, FootprintConfig.read(section("footprint.", "footprint.scanRadius", "50")).scanRadius());
        assertEquals(5, PossessionConfig.read(section("possession.", "possession.maxSeconds", "5")).maxSeconds());
        assertEquals(80, TelemetryConfig.read(section("telemetry.", "telemetry.slowTickThresholdMillis", "80")).slowTickThresholdMillis());
        assertEquals(MinimapConfig.Mode.FAIR, MinimapConfig.read(section("minimap.", "minimap.mode", "fair")).mode());
        assertEquals(40, EightPagesConfig.read(section("eightPages.", "eightPages.bonusOneSurvivor", "40")).bonusOneSurvivor());
        assertEquals(6.0D, RegenerationConfig.read(section("regeneration.", "regeneration.perSecond", "6")).perSecond(), 1.0E-9);
        assertEquals(java.util.List.of(0.5D, 0.1D), RegenerationConfig.read(section("regeneration.", "regeneration.shares", "0.5, 0.1")).shares());
        assertEquals(20, RegenerationConfig.read(section("regeneration.", "regeneration.minMissing", "20")).minMissing());
    }

    @Test
    @DisplayName("An unknown minimap mode falls back to the default")
    void unknownMinimapModeFallsBack() {
        assertEquals(MinimapConfig.DEFAULT, MinimapConfig.read(section("minimap.", "minimap.mode", "radar")));
    }

    @Test
    @DisplayName("A possession setting out of range falls back to the defaults")
    void invalidPossessionFallsBack() {
        assertEquals(PossessionConfig.DEFAULT, PossessionConfig.read(section("possession.", "possession.sightFactor", "0.5")));
    }

    @Test
    @DisplayName("An Eight Pages bonus that is no multiple of 5 falls back to the defaults")
    void invalidEightPagesFallsBack() {
        assertEquals(EightPagesConfig.DEFAULT, EightPagesConfig.read(section("eightPages.", "eightPages.bonusOneSurvivor", "7")));
    }

    @Test
    @DisplayName("Regeneration shares that are no numbers fall back to the defaults")
    void invalidRegenerationSharesFallBack() {
        assertEquals(RegenerationConfig.DEFAULT, RegenerationConfig.read(section("regeneration.", "regeneration.shares", "0.4,lots")));
    }
}
