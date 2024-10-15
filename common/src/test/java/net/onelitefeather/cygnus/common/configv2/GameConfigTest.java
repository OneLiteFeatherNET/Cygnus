package net.onelitefeather.cygnus.common.configv2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

}