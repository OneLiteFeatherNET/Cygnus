package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
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

    @Test
    void testOptionalValuesAreAbsentWhenTheyAreNotConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertNull(config.sentryDsn());
        assertNull(config.resourcePackUrl());
        assertNull(config.resourcePackSha1());
    }

    @Test
    void testOptionalValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                sentryDsn=https://key@sentry.example.com/1
                resourcePackUrl=https://example.com/pack.zip
                resourcePackSha1=%s
                """.formatted("a".repeat(40)));

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals("https://key@sentry.example.com/1", config.sentryDsn());
        assertEquals(URI.create("https://example.com/pack.zip"), config.resourcePackUrl());
        assertEquals("a".repeat(40), config.resourcePackSha1());
    }

    @Test
    void testBlankOptionalValuesCountAsAbsent(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                sentryDsn=
                resourcePackUrl=   
                resourcePackSha1=
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertNull(config.sentryDsn());
        assertNull(config.resourcePackUrl());
        assertNull(config.resourcePackSha1());
    }

    @Test
    void testAMalformedResourcePackUrlDisablesTheFeature(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                resourcePackUrl=http://[::1
                resourcePackSha1=%s
                """.formatted("a".repeat(40)));

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertNull(config.resourcePackUrl());
    }

    @Test
    void testAMalformedChecksumIsDroppedSoItGetsComputed(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                resourcePackUrl=https://example.com/pack.zip
                resourcePackSha1=not-a-checksum
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(URI.create("https://example.com/pack.zip"), config.resourcePackUrl());
        assertNull(config.resourcePackSha1());
    }

    @Test
    void testPageProximityDefaultsWhenNothingIsConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertTrue(config.pageProximityEnabled());
        assertEquals(20, config.pageProximityRange());
        assertEquals(20, config.pageProximityInterval());
        assertEquals(GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND, config.pageProximitySound());
    }

    @Test
    void testPageProximityValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityRange=32
                pageProximityInterval=40
                pageProximitySound=block.note_block.chime
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertTrue(config.pageProximityEnabled());
        assertEquals(32, config.pageProximityRange());
        assertEquals(40, config.pageProximityInterval());
        assertEquals(Key.key("block.note_block.chime"), config.pageProximitySound());
    }

    @Test
    void testPageProximityCanBeTurnedOff(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityEnabled=false
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertFalse(config.pageProximityEnabled());
    }

    @Test
    void testAMalformedSoundKeyFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximitySound=NOT A KEY
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND, config.pageProximitySound());
    }

    @Test
    void testARangeBeyondTheMaximumIsRejected(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityRange=%d
                """.formatted(GameConfig.MAX_PAGE_PROXIMITY_RANGE + 1));

        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }
}
