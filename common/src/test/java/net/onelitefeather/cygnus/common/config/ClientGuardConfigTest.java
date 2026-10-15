package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers how the {@code client-guard.} keys are read and what a broken value falls back to.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class ClientGuardConfigTest {

    private static ConfigSection section(String... pairs) {
        Properties properties = new Properties();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.setProperty("client-guard." + pairs[i], pairs[i + 1]);
        }
        return ConfigSection.root(properties).section("client-guard.");
    }

    @Test
    @DisplayName("Without any keys the client guard kicks the known cheat clients")
    void emptySectionGivesDefaults() {
        assertEquals(ClientGuardConfig.DEFAULT, ClientGuardConfig.read(section()),
                "an empty section must read the defaults");
        assertEquals(ClientGuardConfig.Mode.KICK, ClientGuardConfig.DEFAULT.mode(), "the default mode must kick");
    }

    @Test
    @DisplayName("The default brands are the cheat clients the check is meant for")
    void defaultBrandsContainMeteor() {
        assertEquals(List.of("meteor", "wurst", "aristois", "liquidbounce", "impact", "future", "rusherhack",
                "inertia", "bleachhack"), ClientGuardConfig.DEFAULT.blockedBrands());
    }

    @Test
    @DisplayName("The mode is read regardless of its case")
    void modeIsReadCaseInsensitively() {
        assertEquals(ClientGuardConfig.Mode.LOG, ClientGuardConfig.read(section("mode", " Log ")).mode());
        assertEquals(ClientGuardConfig.Mode.OFF, ClientGuardConfig.read(section("mode", "off")).mode());
    }

    @Test
    @DisplayName("An unknown mode falls back to the default mode")
    void unknownModeFallsBack() {
        assertEquals(ClientGuardConfig.Mode.KICK, ClientGuardConfig.read(section("mode", "ban")).mode());
    }

    @Test
    @DisplayName("Brand lists are split on commas, trimmed and lower cased")
    void brandListIsNormalised() {
        ClientGuardConfig config = ClientGuardConfig.read(section("blocked-brands", " Foo , BAR ,, "));

        assertEquals(List.of("foo", "bar"), config.blockedBrands(), "entries must be trimmed, lower cased and non-empty");
    }

    @Test
    @DisplayName("A blank block list keeps its default instead of clearing it")
    void blankBlockListKeepsDefault() {
        ClientGuardConfig config = ClientGuardConfig.read(section("blocked-brands", "   ", "blocked-channels", ""));

        assertEquals(ClientGuardConfig.DEFAULT_BLOCKED_BRANDS, config.blockedBrands(), "a blank brand list must keep the defaults");
        assertEquals(ClientGuardConfig.DEFAULT_BLOCKED_CHANNELS, config.blockedChannels(), "a blank channel list must keep the defaults");
    }

    @Test
    @DisplayName("The allowed brands stay empty unless they are configured")
    void allowedBrandsDefaultToEmpty() {
        assertEquals(List.of(), ClientGuardConfig.read(section()).allowedBrands(),
                "without a whitelist every brand may join");
    }

    @Test
    @DisplayName("Configured allowed brands are kept as a whitelist")
    void allowedBrandsAreRead() {
        assertEquals(List.of("vanilla", "fabric"),
                ClientGuardConfig.read(section("allowed-brands", "Vanilla,fabric")).allowedBrands());
    }

    @Test
    @DisplayName("Channel prefixes are read from their own key")
    void channelsAreRead() {
        assertEquals(List.of("wdl:", "freecam:"),
                ClientGuardConfig.read(section("blocked-channels", "wdl:,freecam:")).blockedChannels());
    }

    @Test
    @DisplayName("A missing mode is rejected by the record")
    void nullModeIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ClientGuardConfig(null, List.of(), List.of(), List.of(), true,
                        List.of(), List.of(), 100));

        assertEquals("Client guard mode must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("The shader probe is on by default")
    void shaderProbeIsOnByDefault() {
        assertTrue(ClientGuardConfig.read(section()).shaderProbe(), "the shader probe must be on without any keys");
    }

    @Test
    @DisplayName("Iris, OptiFine and Canvas are the incompatible shader mods by default")
    void incompatibleModsDefaultToTheBreakingOnes() {
        assertEquals(List.of(ShaderMod.IRIS, ShaderMod.OPTIFINE, ShaderMod.CANVAS),
                ClientGuardConfig.read(section()).incompatibleMods());
    }

    @Test
    @DisplayName("Sodium, Embeddium and Nvidium are only logged by default")
    void loggedModsDefaultToTheCosmeticOnes() {
        assertEquals(List.of(ShaderMod.SODIUM, ShaderMod.EMBEDDIUM, ShaderMod.NVIDIUM),
                ClientGuardConfig.read(section()).loggedMods());
    }

    @Test
    @DisplayName("Shader mod ids are read regardless of their case and spacing")
    void shaderModIdsAreReadLeniently() {
        ClientGuardConfig config = ClientGuardConfig.read(section("incompatible-mods", " Iris ,OPTIFINE"));

        assertEquals(List.of(ShaderMod.IRIS, ShaderMod.OPTIFINE), config.incompatibleMods());
    }

    @Test
    @DisplayName("An unknown shader mod id is skipped and the known ids are kept")
    void unknownShaderModIsSkipped() {
        ClientGuardConfig config = ClientGuardConfig.read(section("logged-mods", "sodium,notamod"));

        assertEquals(List.of(ShaderMod.SODIUM), config.loggedMods(), "only the known mod may stay in the list");
    }

    @Test
    @DisplayName("A blank shader mod list keeps its default instead of clearing it")
    void blankShaderModListKeepsDefault() {
        ClientGuardConfig config = ClientGuardConfig.read(section("incompatible-mods", "  "));

        assertEquals(ClientGuardConfig.DEFAULT_INCOMPATIBLE_MODS, config.incompatibleMods(),
                "a blank list must keep the default mods");
    }

    @Test
    @DisplayName("The shader probe can be switched off")
    void shaderProbeCanBeSwitchedOff() {
        assertFalse(ClientGuardConfig.read(section("shader-probe", "false")).shaderProbe(),
                "shader-probe=false must turn the probe off");
    }

    @Test
    @DisplayName("The probe timeout is read in ticks")
    void probeTimeoutIsReadInTicks() {
        assertEquals(40, ClientGuardConfig.read(section("shader-probe-timeout-ticks", "40")).shaderProbeTimeoutTicks());
    }

    @Test
    @DisplayName("A probe timeout below one tick falls back to the default")
    void nonPositiveProbeTimeoutFallsBack() {
        assertEquals(ClientGuardConfig.DEFAULT_SHADER_PROBE_TIMEOUT_TICKS,
                ClientGuardConfig.read(section("shader-probe-timeout-ticks", "0")).shaderProbeTimeoutTicks());
    }

    @Test
    @DisplayName("A probe timeout of zero ticks is rejected by the record")
    void zeroProbeTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClientGuardConfig(ClientGuardConfig.Mode.KICK, List.of(), List.of(), List.of(), true,
                        List.of(), List.of(), 0));
    }
}
