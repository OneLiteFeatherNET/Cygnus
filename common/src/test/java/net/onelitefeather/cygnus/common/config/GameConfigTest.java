package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    void testDefaultConfig() {
        GameConfig config = GameConfig.DEFAULT;

        assertEquals(new GameConfig.Round(2, 13, 30, 900), config.round());
        assertEquals(new GameConfig.Teams(1, 12), config.teams());
        assertNull(config.sentryDsn());
        assertEquals(GameConfig.ResourcePack.NONE, config.resourcePack());
    }

    @Test
    void testInvalidTeamSizes() {
        assertRejected("slenderTeamSize (0) must be at least 1", () -> new GameConfig.Teams(0, 12));
        assertRejected("survivorTeamSize (0) must be at least 2", () -> new GameConfig.Teams(1, 0));
    }

    @Test
    void testInvalidLobbyTime() {
        assertRejected("lobbyTime (" + GameConfig.FORCE_START_TIME + ") must be at least " + (GameConfig.FORCE_START_TIME + 1),
                () -> new GameConfig.Round(2, 10, GameConfig.FORCE_START_TIME, 600));
    }

    @Test
    void testInvalidDamageSoundCooldown() {
        assertRejected("damageSoundCooldown (0) must be at least 1",
                () -> new GameConfig.DamageSound(true, 0, GameConfig.DamageSound.DEFAULT_SOUND));
    }

    @Test
    void testInvalidLobbyAtmosphereShare() {
        GameConfig defaults = GameConfig.DEFAULT;
        assertRejected("lobbyAtmosphereShare (1.5) must be between 0 and 1", () -> new GameConfig(
                defaults.round(), defaults.teams(), null, defaults.resourcePack(), defaults.pageProximity(),
                defaults.damageSound(), defaults.glitch(), defaults.pageGlitch(), defaults.creek(),
                defaults.sanity(), defaults.stamina(), defaults.adrenaline(), defaults.footprint(), defaults.possession(), defaults.telemetry(), defaults.minimap(), defaults.eightPages(), defaults.regeneration(), defaults.clientGuard(), defaults.clientApi(), defaults.lunar(), 1.5F));
    }

    @Test
    void testGlitchRangeRejectsValuesOutsideTheAllowedRange() {
        int max = GameConfig.Glitch.MAX_RANGE;
        assertRejected("glitchRange (0) must be between 1 and " + max, () -> new GameConfig.Glitch(0, 4, 30));
        assertRejected("glitchRange (" + (max + 1) + ") must be between 1 and " + max, () -> new GameConfig.Glitch(max + 1, 4, 30));
    }

    @Test
    void testGlitchCloseRangeRejectsValuesBelowOne() {
        assertRejected("glitchCloseRange (0) must be at least 1", () -> new GameConfig.Glitch(12, 0, 30));
    }

    @Test
    void testGlitchViewAngleRejectsValuesOutsideTheAllowedRange() {
        int max = GameConfig.Glitch.MAX_VIEW_ANGLE;
        assertRejected("glitchViewAngle (0) must be between 1 and " + max, () -> new GameConfig.Glitch(12, 4, 0));
        assertRejected("glitchViewAngle (" + (max + 1) + ") must be between 1 and " + max, () -> new GameConfig.Glitch(12, 4, max + 1));
    }

    /**
     * Each of the two distances is a valid number on its own. Without the check the slope between
     * them would divide by zero or run backwards.
     */
    @Test
    void testGlitchCloseRangeMustStayBelowTheGlitchRange() {
        assertRejected("glitchCloseRange (12) must be below glitchRange (12)", () -> new GameConfig.Glitch(12, 12, 30));
        assertRejected("glitchCloseRange (16) must be below glitchRange (8)", () -> new GameConfig.Glitch(8, 16, 30));
    }

    @Test
    void testPageProximityVolumeFactorRejectsValuesOutsideTheAllowedRange() {
        assertRejected("pageProximityVolumeFactor (0.5) must be between 1 and 8", () -> proximityWithFactor(0.5F));
        assertRejected("pageProximityVolumeFactor (9) must be between 1 and 8",
                () -> proximityWithFactor(GameConfig.PageProximity.MAX_VOLUME_FACTOR + 1));
    }

    /**
     * A factor of 1 is the behaviour that left the chime silent at the range's edge, so the shipped
     * default has to be above it.
     */
    @Test
    void testTheDefaultVolumeFactorStretchesPastTheRange() {
        assertTrue(GameConfig.PageProximity.DEFAULT.volumeFactor() > 1.0F,
                "a factor of 1 puts the chime's silence exactly at the range's edge");
    }

    @Test
    void testPageGlitchMaxLevelRejectsValuesOutsideTheAllowedRange() {
        int max = GameConfig.PageGlitch.MAX_LEVEL;
        assertRejected("pageGlitchMaxLevel (-1) must be between 0 and " + max, () -> new GameConfig.PageGlitch(true, 3, -1));
        assertRejected("pageGlitchMaxLevel (" + (max + 1) + ") must be between 0 and " + max, () -> new GameConfig.PageGlitch(true, 3, max + 1));
    }

    @Test
    void testPageGlitchDefaultsToTheWeakestLevel() {
        assertEquals(0, GameConfig.PageGlitch.DEFAULT.maxLevel());
    }

    private static GameConfig.PageProximity proximityWithFactor(float volumeFactor) {
        return new GameConfig.PageProximity(true, 20, GameConfig.PageProximity.DEFAULT_SOUND, volumeFactor);
    }

    private static void assertRejected(String message, Executable creation) {
        assertEquals(message, assertThrows(IllegalArgumentException.class, creation).getMessage());
    }

    @Test
    void testMinimapModeMustBeSet() {
        assertRejected("Minimap mode must not be null", () -> new MinimapConfig(null));
    }

    @Test
    void testReadWithoutKeysGivesTheDefaults() {
        assertEquals(GameConfig.DEFAULT, GameConfig.read(ConfigSection.root(new Properties())));
    }

    @Test
    void testReadTakesEveryGroupFromItsKeys() {
        Properties properties = new Properties();
        properties.setProperty("minPlayers", "3");
        properties.setProperty("survivorTeamSize", "10");
        properties.setProperty("sentryDsn", "https://key@sentry.example/1");
        properties.setProperty("resourcePackSha1", "0123456789abcdef0123456789abcdef01234567");
        properties.setProperty("pageProximityRange", "30");
        properties.setProperty("damageSoundCooldown", "40");
        properties.setProperty("glitchRange", "20");
        properties.setProperty("pageGlitchMaxLevel", "2");
        properties.setProperty("creek.huntThreshold", "0.7");
        properties.setProperty("minimap.mode", "fair");
        properties.setProperty("lobbyAtmosphereShare", "0.5");

        GameConfig config = GameConfig.read(ConfigSection.root(properties));

        assertEquals(3, config.round().minPlayers());
        assertEquals(10, config.teams().survivorSize());
        assertEquals("https://key@sentry.example/1", config.sentryDsn());
        assertEquals("0123456789abcdef0123456789abcdef01234567", config.resourcePack().sha1());
        assertEquals(30, config.pageProximity().range());
        assertEquals(40, config.damageSound().cooldown());
        assertEquals(20, config.glitch().range());
        assertEquals(2, config.pageGlitch().maxLevel());
        assertEquals(0.7D, config.creek().hunt().threshold(), 1.0E-9);
        assertEquals(MinimapConfig.Mode.FAIR, config.minimap().mode());
        assertEquals(0.5F, config.lobbyAtmosphereShare());
    }
}
