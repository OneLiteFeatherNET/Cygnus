package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameConfigTest {

    @Test
    void testInvalidSlenderUsage() {
        GameConfig.Builder builder = GameConfig.builder();
        assertNotNull(builder);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.slenderTeamSize(0));
        assertNotNull(exception);

        assertEquals("Slender team size must be at least 1", exception.getMessage());
    }

    @Test
    void testInvalidSurvivorUsage() {
        GameConfig.Builder builder = GameConfig.builder();
        assertNotNull(builder);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.survivorTeamSize(0));
        assertNotNull(exception);
        assertEquals("Survivor team size must be at least 2", exception.getMessage());
    }

    @Test
    void testInvalidLobbyTimeUsage() {
        GameConfig.Builder builder = GameConfig.builder();
        assertNotNull(builder);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.lobbyTime(0));
        assertNotNull(exception);
        assertEquals("Lobby time must be greater than " + GameConfig.FORCE_START_TIME, exception.getMessage());
    }

    @Test
    void testInvalidDamageSoundCooldownUsage() {
        GameConfig.Builder builder = GameConfig.builder();
        assertNotNull(builder);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.damageSoundCooldown(0));
        assertNotNull(exception);
        assertEquals("Damage sound cooldown must be at least 1 tick", exception.getMessage());
    }

    @Test
    void testBuilderUsage() {
        GameConfig.Builder builder = GameConfig.builder();
        assertNotNull(builder);

        builder.gameTime(500).lobbyTime(12).minPlayers(1).maxPlayers(12).survivorTeamSize(2).slenderTeamSize(2);

        GameConfig config = builder.build();

        assertEquals(500, config.gameTime());
        assertEquals(12, config.lobbyTime());
        assertEquals(1, config.minPlayers());
        assertEquals(12, config.maxPlayers());
        assertEquals(2, config.survivorTeamSize());
    }


    @Test
    void testGlitchRangeRejectsValuesOutsideTheAllowedRange() {
        GameConfig.Builder builder = GameConfig.builder();

        String expected = "Glitch range must be between 1 and " + GameConfig.MAX_GLITCH_RANGE;
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> builder.glitchRange(0)).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class,
                () -> builder.glitchRange(GameConfig.MAX_GLITCH_RANGE + 1)).getMessage());
    }

    @Test
    void testGlitchCloseRangeRejectsValuesBelowOne() {
        GameConfig.Builder builder = GameConfig.builder();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.glitchCloseRange(0));
        assertEquals("Glitch close range must be at least 1 block", exception.getMessage());
    }

    @Test
    void testGlitchViewAngleRejectsValuesOutsideTheAllowedRange() {
        GameConfig.Builder builder = GameConfig.builder();

        String expected = "Glitch view angle must be between 1 and " + GameConfig.MAX_GLITCH_VIEW_ANGLE + " degrees";
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> builder.glitchViewAngle(0)).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class,
                () -> builder.glitchViewAngle(GameConfig.MAX_GLITCH_VIEW_ANGLE + 1)).getMessage());
    }

    /**
     * Each of the two distances is a valid number on its own, so neither setter can reject this
     * pairing - only {@code build()} sees both. Without the check the slope between them would
     * divide by zero or run backwards.
     */
    @Test
    void testGlitchCloseRangeMustStayBelowTheGlitchRange() {
        GameConfig.Builder equal = GameConfig.builder().glitchRange(12).glitchCloseRange(12);
        assertEquals("Glitch close range (12) must be below the glitch range (12)",
                assertThrows(IllegalArgumentException.class, equal::build).getMessage());

        GameConfig.Builder crossed = GameConfig.builder().glitchRange(8).glitchCloseRange(16);
        assertEquals("Glitch close range (16) must be below the glitch range (8)",
                assertThrows(IllegalArgumentException.class, crossed::build).getMessage());
    }

    @Test
    void testGlitchValuesReachTheBuiltConfiguration() {
        GameConfig config = GameConfig.builder()
                .lobbyTime(12)
                .glitchRange(20)
                .glitchCloseRange(6)
                .glitchViewAngle(45)
                .build();

        assertEquals(20, config.glitchRange());
        assertEquals(6, config.glitchCloseRange());
        assertEquals(45, config.glitchViewAngle());
    }

    /**
     * A builder that says nothing about the gaze still has to produce a usable configuration -
     * otherwise every caller would be forced to set three values it has no opinion on.
     */
    @Test
    void testGlitchDefaultsApplyWhenTheBuilderSaysNothing() {
        GameConfig config = GameConfig.builder().lobbyTime(12).build();

        assertEquals(GameConfig.DEFAULT_GLITCH_RANGE, config.glitchRange());
        assertEquals(GameConfig.DEFAULT_GLITCH_CLOSE_RANGE, config.glitchCloseRange());
        assertEquals(GameConfig.DEFAULT_GLITCH_VIEW_ANGLE, config.glitchViewAngle());
    }

    @Test
    void testPageProximityVolumeFactorRejectsValuesOutsideTheAllowedRange() {
        GameConfig.Builder builder = GameConfig.builder();

        String expected = "Page proximity volume factor must be between 1 and "
                + GameConfig.MAX_PAGE_PROXIMITY_VOLUME_FACTOR;
        assertEquals(expected, assertThrows(IllegalArgumentException.class,
                () -> builder.pageProximityVolumeFactor(0.5F)).getMessage());
        assertEquals(expected, assertThrows(IllegalArgumentException.class,
                () -> builder.pageProximityVolumeFactor(GameConfig.MAX_PAGE_PROXIMITY_VOLUME_FACTOR + 1)).getMessage());
    }

    @Test
    void testPageProximityVolumeFactorReachesTheBuiltConfiguration() {
        GameConfig config = GameConfig.builder().lobbyTime(12).pageProximityVolumeFactor(3.5F).build();

        assertEquals(3.5F, config.pageProximityVolumeFactor());
    }

    /**
     * A factor of 1 is the behaviour that left the chime silent at the range's edge, so the shipped
     * default has to be above it.
     */
    @Test
    void testTheDefaultVolumeFactorStretchesPastTheRange() {
        GameConfig config = GameConfig.builder().lobbyTime(12).build();

        assertEquals(GameConfig.DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR, config.pageProximityVolumeFactor());
        assertTrue(GameConfig.DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR > 1.0F,
                "a factor of 1 puts the chime's silence exactly at the range's edge");
    }
}
