package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class InternalGameConfigTest {

    @Test
    void testInternalConfigUsage() {
        GameConfigReader gameConfigReader = new GameConfigReader(Paths.get(""));
        assertNotNull(gameConfigReader);

        GameConfig gameConfig = gameConfigReader.getConfig();
        assertNotNull(gameConfig);
        assertInstanceOf(InternalGameConfig.class, gameConfig);

        assertEquals(2, gameConfig.minPlayers());
        assertEquals(13, gameConfig.maxPlayers());
        assertEquals(30, gameConfig.lobbyTime());
        assertEquals(900, gameConfig.gameTime());
        assertEquals(1, gameConfig.slenderTeamSize());
        assertEquals(12, gameConfig.survivorTeamSize());
    }
}
