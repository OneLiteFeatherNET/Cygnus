package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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
        assertRejected("Slender team size must be at least 1", () -> new GameConfig.Teams(0, 12));
        assertRejected("Survivor team size must be at least 2", () -> new GameConfig.Teams(1, 0));
    }

    @Test
    void testInvalidLobbyTime() {
        assertRejected("Lobby time must be greater than " + GameConfig.FORCE_START_TIME,
                () -> new GameConfig.Round(2, 10, GameConfig.FORCE_START_TIME, 600));
    }

    @Test
    void testInvalidDamageSoundCooldown() {
        assertRejected("Damage sound cooldown must be at least 1 tick",
                () -> new GameConfig.DamageSound(true, 0, GameConfig.DamageSound.DEFAULT_SOUND));
    }

    @Test
    void testInvalidLobbyAtmosphereShare() {
        GameConfig defaults = GameConfig.DEFAULT;
        assertRejected("Lobby atmosphere share must be between 0 and 1", () -> new GameConfig(
                defaults.round(), defaults.teams(), null, defaults.resourcePack(), defaults.pageProximity(),
                defaults.damageSound(), defaults.glitch(), defaults.slenderStatic(), 1.5F));
    }

    @Test
    void testGlitchRangeRejectsValuesOutsideTheAllowedRange() {
        String expected = "Glitch range must be between 1 and " + GameConfig.Glitch.MAX_RANGE;
        assertRejected(expected, () -> new GameConfig.Glitch(0, 4, 30));
        assertRejected(expected, () -> new GameConfig.Glitch(GameConfig.Glitch.MAX_RANGE + 1, 4, 30));
    }

    @Test
    void testGlitchCloseRangeRejectsValuesBelowOne() {
        assertRejected("Glitch close range must be at least 1 block", () -> new GameConfig.Glitch(12, 0, 30));
    }

    @Test
    void testGlitchViewAngleRejectsValuesOutsideTheAllowedRange() {
        String expected = "Glitch view angle must be between 1 and " + GameConfig.Glitch.MAX_VIEW_ANGLE + " degrees";
        assertRejected(expected, () -> new GameConfig.Glitch(12, 4, 0));
        assertRejected(expected, () -> new GameConfig.Glitch(12, 4, GameConfig.Glitch.MAX_VIEW_ANGLE + 1));
    }

    /**
     * Each of the two distances is a valid number on its own. Without the check the slope between
     * them would divide by zero or run backwards.
     */
    @Test
    void testGlitchCloseRangeMustStayBelowTheGlitchRange() {
        assertRejected("Glitch close range (12) must be below the glitch range (12)", () -> new GameConfig.Glitch(12, 12, 30));
        assertRejected("Glitch close range (16) must be below the glitch range (8)", () -> new GameConfig.Glitch(8, 16, 30));
    }

    @Test
    void testPageProximityVolumeFactorRejectsValuesOutsideTheAllowedRange() {
        String expected = "Page proximity volume factor must be between 1 and " + GameConfig.PageProximity.MAX_VOLUME_FACTOR;
        assertRejected(expected, () -> proximityWithFactor(0.5F));
        assertRejected(expected, () -> proximityWithFactor(GameConfig.PageProximity.MAX_VOLUME_FACTOR + 1));
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

    private static GameConfig.PageProximity proximityWithFactor(float volumeFactor) {
        return new GameConfig.PageProximity(true, 20, 20, GameConfig.PageProximity.DEFAULT_SOUND, volumeFactor);
    }

    private static void assertRejected(String message, Executable creation) {
        assertEquals(message, assertThrows(IllegalArgumentException.class, creation).getMessage());
    }
}
