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
        assertEquals(GameConfig.PageGlitch.DEFAULT, config.pageGlitch());
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
    void testPageGlitchValuesAreReadWhenTheyAreConfigured(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageGlitchPulseSeconds=7
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertTrue(config.pageGlitch().enabled());
        assertEquals(7, config.pageGlitch().pulseSeconds());
    }

    @Test
    void testPageGlitchMaxLevelDefaultsToTheWeakestLevel(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), "minPlayers=4\n");

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(0, config.pageGlitch().maxLevel());
    }

    @Test
    void testPageGlitchMaxLevelIsRead(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageGlitchMaxLevel=2
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertEquals(2, config.pageGlitch().maxLevel());
    }

    @Test
    void testAPageGlitchMaxLevelOutsideItsRangeIsRejected(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageGlitchMaxLevel=4
                """);

        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testPageGlitchCanBeTurnedOff(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageGlitchEnabled=false
                """);

        GameConfig config = new GameConfigReader(tempDir).getConfig();

        assertFalse(config.pageGlitch().enabled());
    }

    @Test
    void testAPageGlitchPulseOutsideItsRangeIsRejected(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=4
                pageGlitchPulseSeconds=31
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

    @Test
    void testCreekDefaultsWhenNotConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertEquals(CreekConfig.DEFAULT, config.creek());
    }

    @Test
    void testCreekValuesAreRead(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                creek.enabled=false
                creek.huntThreshold=0.7
                creek.stalkMaxDistance=40
                creek.routeLinkDistance=4.5
                creek.randomStopChance=0.4
                creek.randomStopMinMillis=500
                creek.randomStopMaxMillis=900
                """);

        CreekConfig creek = new GameConfigReader(tempDir).getConfig().creek();

        assertFalse(creek.enabled());
        assertEquals(0.7D, creek.huntThreshold(), 1.0E-9);
        assertEquals(40, creek.stalkMaxDistance());
        assertEquals(CreekConfig.DEFAULT.stalkMinDistance(), creek.stalkMinDistance());
        assertEquals(4.5D, creek.routeLinkDistance(), 1.0E-9);
        assertEquals(0.4D, creek.randomStopChance(), 1.0E-9);
        assertEquals(500, creek.randomStopMinMillis());
        assertEquals(900, creek.randomStopMaxMillis());
    }

    @Test
    void testCreekRejectsValuesThatDoNotFit(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                creek.enabled=false
                creek.stalkThreshold=0.9
                """);
        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testAnUnreadableCreekValueFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                creek.huntThreshold=not-a-number
                """);

        assertEquals(CreekConfig.DEFAULT.huntThreshold(),
                new GameConfigReader(tempDir).getConfig().creek().huntThreshold());
    }

    @Test
    void testAdrenalineDefaultsWhenNotConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertEquals(AdrenalineConfig.DEFAULT, config.adrenaline());
    }

    @Test
    void testAdrenalineValuesAreRead(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                adrenaline.radius=12
                adrenaline.speedBonus=0.3
                adrenaline.cooldownSeconds=30
                """);

        AdrenalineConfig adrenaline = new GameConfigReader(tempDir).getConfig().adrenaline();

        assertEquals(12, adrenaline.radius());
        assertEquals(0.3D, adrenaline.speedBonus(), 1.0E-9);
        assertEquals(30, adrenaline.cooldownSeconds());
        assertEquals(AdrenalineConfig.DEFAULT.durationSeconds(), adrenaline.durationSeconds());
    }

    @Test
    void testAdrenalineRejectsValuesOutOfRange(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                adrenaline.speedBonus=2
                """);

        GameConfigReader reader = new GameConfigReader(tempDir);
        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testStaminaDefaultsWhenNotConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertEquals(StaminaConfig.DEFAULT, config.stamina());
    }

    @Test
    void testStaminaValuesAreRead(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                stamina.sprintResumeShare=0.5
                stamina.slenderReappearCooldownSeconds=8
                """);

        StaminaConfig stamina = new GameConfigReader(tempDir).getConfig().stamina();

        assertEquals(0.5D, stamina.sprintResumeShare(), 1.0E-9);
        assertEquals(8, stamina.slenderReappearCooldownSeconds());
        assertEquals(StaminaConfig.DEFAULT.regenPerSecond(), stamina.regenPerSecond());
    }

    @Test
    void testStaminaRejectsValuesOutOfRange(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                stamina.regenPerSecond=-1
                """);

        GameConfigReader reader = new GameConfigReader(tempDir);
        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testTelemetryDefaultsWhenNotConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertEquals(TelemetryConfig.DEFAULT, config.telemetry());
    }

    @Test
    void testTelemetryThresholdIsRead(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                telemetry.slowTickThresholdMillis=80
                """);

        assertEquals(80, new GameConfigReader(tempDir).getConfig().telemetry().slowTickThresholdMillis());
    }

    @Test
    void testTelemetryRejectsThresholdOutOfRange(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                telemetry.slowTickThresholdMillis=0
                """);

        GameConfigReader reader = new GameConfigReader(tempDir);
        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testSanityDefaultsWhenNotConfigured() {
        GameConfig config = new GameConfigReader(Paths.get("src", "test", "resources")).getConfig();

        assertEquals(SanityConfig.DEFAULT, config.sanity());
    }

    @Test
    void testSanityValuesAreRead(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                sanity.pageFloorWeight=0.4
                sanity.caughtGain=0.2
                sanity.sightingCooldownSeconds=30
                sanity.decayPerSecond=0.01
                sanity.stalkGainPerSecond=0.02
                sanity.floorCap=0.5
                """);

        SanityConfig sanity = new GameConfigReader(tempDir).getConfig().sanity();

        assertEquals(0.4D, sanity.pageFloorWeight(), 1.0E-9);
        assertEquals(0.2D, sanity.caughtGain(), 1.0E-9);
        assertEquals(30, sanity.sightingCooldownSeconds());
        assertEquals(0.01D, sanity.decayPerSecond(), 1.0E-9);
        assertEquals(0.02D, sanity.stalkGainPerSecond(), 1.0E-9);
        assertEquals(0.5D, sanity.floorCap(), 1.0E-9);
        assertEquals(SanityConfig.DEFAULT.residualShare(), sanity.residualShare());
        assertEquals(SanityConfig.DEFAULT.pageFoundGain(), sanity.pageFoundGain());
    }

    @Test
    void testSanityRejectsValuesOutOfRange(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                sanity.deathGain=1.5
                """);
        GameConfigReader reader = new GameConfigReader(tempDir);

        assertThrows(IllegalArgumentException.class, reader::getConfig);
    }

    @Test
    void testAnUnreadableSanityValueFallsBackToTheDefault(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                sanity.caughtGain=lots
                """);

        assertEquals(SanityConfig.DEFAULT.caughtGain(),
                new GameConfigReader(tempDir).getConfig().sanity().caughtGain());
    }

    @Test
    void testTheOldDreadKeysAreIgnored(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.properties"), """
                minPlayers=2
                creek.dreadPageWeight=-5
                creek.isolationRadius=0
                """);

        assertEquals(CreekConfig.DEFAULT, new GameConfigReader(tempDir).getConfig().creek());
    }
}
