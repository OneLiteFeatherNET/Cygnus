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

    @Test
    void testSlenderStaticDefaultsWhenNothingIsConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertTrue(config.slenderStaticEnabled());
        assertEquals(GameConfig.DEFAULT_SLENDER_STATIC_SOUND, config.slenderStaticSound());
        assertEquals(GameConfig.DEFAULT_SLENDER_STATIC_QUIET_INTERVAL, config.slenderStaticQuietInterval());
        assertEquals(GameConfig.DEFAULT_SLENDER_STATIC_FRANTIC_INTERVAL, config.slenderStaticFranticInterval());
        assertEquals(GameConfig.DEFAULT_SLENDER_STATIC_MIN_VOLUME, config.slenderStaticMinVolume());
        assertEquals(GameConfig.DEFAULT_SLENDER_STATIC_MAX_VOLUME, config.slenderStaticMaxVolume());
    }

    @Test
    void testSlenderStaticValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                slenderStaticSound=cygnus:vhs_static
                slenderStaticQuietInterval=20
                slenderStaticFranticInterval=2
                slenderStaticMinVolume=0.1
                slenderStaticMaxVolume=1.0
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(Key.key("cygnus", "vhs_static"), config.slenderStaticSound(),
                "a resource pack sound has to survive the reader, it is the point of the setting");
        assertEquals(20, config.slenderStaticQuietInterval());
        assertEquals(2, config.slenderStaticFranticInterval());
        assertEquals(0.1F, config.slenderStaticMinVolume());
        assertEquals(1.0F, config.slenderStaticMaxVolume());
    }

    @Test
    void testSlenderStaticCanBeTurnedOff(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                slenderStaticEnabled=false
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertFalse(config.slenderStaticEnabled());
    }

    @Test
    void testAFranticIntervalAtOrAboveTheQuietOneIsRejected(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                slenderStaticQuietInterval=5
                slenderStaticFranticInterval=5
                """);

        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testDamageSoundDefaultsWhenNothingIsConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertTrue(config.damageSoundEnabled());
        assertEquals(20, config.damageSoundCooldown());
        assertEquals(GameConfig.DEFAULT_DAMAGE_SOUND, config.damageSound());
    }

    @Test
    void testDamageSoundValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                damageSoundCooldown=30
                damageSound=entity.player.big_fall
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertTrue(config.damageSoundEnabled());
        assertEquals(30, config.damageSoundCooldown());
        assertEquals(Key.key("entity.player.big_fall"), config.damageSound());
    }

    @Test
    void testDamageSoundCanBeTurnedOff(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                damageSoundEnabled=false
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertFalse(config.damageSoundEnabled());
    }

    @Test
    void testAMalformedDamageSoundKeyFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                damageSound=NOT A KEY
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(GameConfig.DEFAULT_DAMAGE_SOUND, config.damageSound());
    }

    @Test
    void testADamageSoundCooldownBelowOneTickIsRejected(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                damageSoundCooldown=0
                """);

        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testGlitchDefaultsWhenNothingIsConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertEquals(GameConfig.DEFAULT_GLITCH_RANGE, config.glitchRange());
        assertEquals(GameConfig.DEFAULT_GLITCH_CLOSE_RANGE, config.glitchCloseRange());
        assertEquals(GameConfig.DEFAULT_GLITCH_VIEW_ANGLE, config.glitchViewAngle());
    }

    @Test
    void testGlitchValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                glitchRange=20
                glitchCloseRange=6
                glitchViewAngle=45
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(20, config.glitchRange());
        assertEquals(6, config.glitchCloseRange());
        assertEquals(45, config.glitchViewAngle());
    }

    /**
     * An unreadable number falls back to its default the same way every other integer entry does.
     * That matters more here than elsewhere: falling back to zero would put the close range at or
     * above the range and take the whole service down over one typo.
     */
    @Test
    void testAnUnreadableGlitchValueFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                glitchRange=not-a-number
                glitchCloseRange=6
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(GameConfig.DEFAULT_GLITCH_RANGE, config.glitchRange());
        assertEquals(6, config.glitchCloseRange());
    }

    @Test
    void testThePageProximityVolumeFactorDefaultsWhenNothingIsConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertEquals(GameConfig.DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR, config.pageProximityVolumeFactor());
    }

    @Test
    void testThePageProximityVolumeFactorIsReadWhenItIsConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityVolumeFactor=3.5
                """);

        assertEquals(3.5F, new GameConfigReader(tempDir).getConfig().pageProximityVolumeFactor());
    }

    /**
     * Decimals fall back like every other number rather than failing the start - and here the
     * fallback matters twice over, since a zero would be rejected by the builder outright.
     */
    @Test
    void testAnUnreadableVolumeFactorFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityVolumeFactor=loud
                """);

        assertEquals(GameConfig.DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR,
                new GameConfigReader(tempDir).getConfig().pageProximityVolumeFactor());
    }
}
