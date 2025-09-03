package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigReaderTest {

    @Test
    void testValidConfigRead() {
        Path origin = Paths.get("src", "test", "resources");
        assertNotNull(origin);
        if (!Files.exists(origin)) {
            fail("Config file not found");
        }

        GameConfigReader gameConfigReader = new GameConfigReader(origin);
        assertNotNull(gameConfigReader);

        GameConfig gameConfig = gameConfigReader.getConfig();
        assertNotNull(gameConfig);
        assertInstanceOf(GameConfig.class, gameConfig);

        assertEquals(4, gameConfig.minPlayers());
        assertEquals(10, gameConfig.maxPlayers());
        assertEquals(30, gameConfig.lobbyTime());
        assertEquals(300, gameConfig.gameTime());
        assertEquals(1, gameConfig.slenderTeamSize());
        assertEquals(12, gameConfig.survivorTeamSize());
    }
}
