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

        GameConfig gameConfig = new GameConfigReader(origin).getConfig();

        assertEquals(new GameConfig.Round(4, 10, 30, 300), gameConfig.round());
        assertEquals(new GameConfig.Teams(1, 12), gameConfig.teams());
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
        assertEquals(2, config.round().minPlayers());
        // "maxPlayers" was valid, should parse correctly (15)
        assertEquals(15, config.round().maxPlayers());
        // "lobbyTime" was invalid, should fall back to default (30)
        assertEquals(30, config.round().lobbyTime());
    }

    @Test
    void testGroupsMissingFromTheFileKeepTheirDefaults() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertNull(config.sentryDsn());
        assertEquals(GameConfig.ResourcePack.NONE, config.resourcePack());
        assertEquals(GameConfig.PageProximity.DEFAULT, config.pageProximity());
        assertEquals(GameConfig.DamageSound.DEFAULT, config.damageSound());
        assertEquals(GameConfig.Glitch.DEFAULT, config.glitch());
        assertEquals(GameConfig.SlenderStatic.DEFAULT, config.slenderStatic());
        assertEquals(GameConfig.DEFAULT_LOBBY_ATMOSPHERE_SHARE, config.lobbyAtmosphereShare());
    }

    @Test
    void testAMissingFileGivesTheDefaultConfig() {
        assertSame(GameConfig.DEFAULT, new GameConfigReader(Paths.get("")).getConfig());
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
        assertEquals(URI.create("https://example.com/pack.zip"), config.resourcePack().url());
        assertEquals("a".repeat(40), config.resourcePack().sha1());
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
        assertNull(config.resourcePack().url());
        assertNull(config.resourcePack().sha1());
    }

    @Test
    void testAMalformedResourcePackUrlDisablesTheFeature(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                resourcePackUrl=http://[::1
                resourcePackSha1=%s
                """.formatted("a".repeat(40)));

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertNull(config.resourcePack().url());
    }

    @Test
    void testAMalformedChecksumIsDroppedSoItGetsComputed(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                resourcePackUrl=https://example.com/pack.zip
                resourcePackSha1=not-a-checksum
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(URI.create("https://example.com/pack.zip"), config.resourcePack().url());
        assertNull(config.resourcePack().sha1());
    }

    @Test
    void testPageProximityValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityRange=32
                pageProximitySound=block.note_block.chime
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertTrue(config.pageProximity().enabled());
        assertEquals(32, config.pageProximity().range());
        assertEquals(Key.key("block.note_block.chime"), config.pageProximity().sound());
    }

    @Test
    void testPageProximityCanBeTurnedOff(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityEnabled=false
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertFalse(config.pageProximity().enabled());
    }

    @Test
    void testAMalformedSoundKeyFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximitySound=NOT A KEY
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(GameConfig.PageProximity.DEFAULT_SOUND, config.pageProximity().sound());
    }

    @Test
    void testARangeBeyondTheMaximumIsRejected(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityRange=%d
                """.formatted(GameConfig.PageProximity.MAX_RANGE + 1));

        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
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

        assertEquals(Key.key("cygnus", "vhs_static"), config.slenderStatic().sound(),
                "a resource pack sound has to survive the reader, it is the point of the setting");
        assertEquals(20, config.slenderStatic().quietInterval());
        assertEquals(2, config.slenderStatic().franticInterval());
        assertEquals(0.1F, config.slenderStatic().minVolume());
        assertEquals(1.0F, config.slenderStatic().maxVolume());
    }

    @Test
    void testSlenderStaticCanBeTurnedOff(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                slenderStaticEnabled=false
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertFalse(config.slenderStatic().enabled());
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
    void testLobbyAtmosphereShareIsReadWhenItIsConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                lobbyAtmosphereShare=0.6
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(0.6F, config.lobbyAtmosphereShare());
    }

    @Test
    void testALobbyAtmosphereShareOutsideItsRangeIsRejected(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                lobbyAtmosphereShare=1.5
                """);

        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testDamageSoundValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                damageSoundCooldown=30
                damageSound=entity.player.big_fall
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertTrue(config.damageSound().enabled());
        assertEquals(30, config.damageSound().cooldown());
        assertEquals(Key.key("entity.player.big_fall"), config.damageSound().sound());
    }

    @Test
    void testDamageSoundCanBeTurnedOff(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                damageSoundEnabled=false
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertFalse(config.damageSound().enabled());
    }

    @Test
    void testAMalformedDamageSoundKeyFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                damageSound=NOT A KEY
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(GameConfig.DamageSound.DEFAULT_SOUND, config.damageSound().sound());
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
    void testGlitchValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                glitchRange=20
                glitchCloseRange=6
                glitchViewAngle=45
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(20, config.glitch().range());
        assertEquals(6, config.glitch().closeRange());
        assertEquals(45, config.glitch().viewAngle());
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

        assertEquals(GameConfig.Glitch.DEFAULT.range(), config.glitch().range());
        assertEquals(6, config.glitch().closeRange());
    }

    @Test
    void testThePageProximityVolumeFactorIsReadWhenItIsConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageProximityVolumeFactor=3.5
                """);

        assertEquals(3.5F, new GameConfigReader(tempDir).getConfig().pageProximity().volumeFactor());
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

        assertEquals(GameConfig.PageProximity.DEFAULT_VOLUME_FACTOR,
                new GameConfigReader(tempDir).getConfig().pageProximity().volumeFactor());
    }
}
