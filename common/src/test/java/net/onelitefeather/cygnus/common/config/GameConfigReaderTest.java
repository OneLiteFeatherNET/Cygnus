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

    @Test
    void testInvalidConfigReadFallback(@org.junit.jupiter.api.io.TempDir Path tempDir) throws java.io.IOException {
        Path configFile = tempDir.resolve("config.properties");

        // Write invalid/malformed values alongside valid ones
        Files.writeString(configFile, "minPlayers=not-an-integer\nmaxPlayers=15\nlobbyTime=abc\n");

        GameConfigReader reader = new GameConfigReader(tempDir);
        GameConfig config = reader.getConfig();

        assertNotNull(config);
        // "minPlayers" was invalid, should fall back to default (2)
        assertEquals(2, config.minPlayers());
        // "maxPlayers" was valid, should parse correctly (15)
        assertEquals(15, config.maxPlayers());
        // "lobbyTime" was invalid, should fall back to default (30)
        assertEquals(30, config.lobbyTime());
    }
}
