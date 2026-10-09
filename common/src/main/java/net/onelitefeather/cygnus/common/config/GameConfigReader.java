package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.InvalidKeyException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * The {@link GameConfigReader} can be used to parse a properties file and create a new {@link GameConfig} instance.
 * If the file does not exist or is empty the default values will be used.
 * A file must have the name "config.properties" and must be located in the root directory of the game.
 * <p>
 * The properties are:
 * <ul>
 *     <li>minPlayers</li>
 *     <li>maxPlayers</li>
 *     <li>lobbyTime</li>
 *     <li>gameTime</li>
 *     <li>survivorTeamSize</li>
 *     <li>slenderTeamSize</li>
 *     <li>sentryDsn</li>
 *     <li>resourcePackUrl</li>
 *     <li>resourcePackSha1</li>
 *     <li>pageProximityEnabled</li>
 *     <li>pageProximityRange</li>
 *     <li>pageProximitySound</li>
 *     <li>pageProximityVolumeFactor</li>
 *     <li>damageSoundEnabled</li>
 *     <li>damageSoundCooldown</li>
 *     <li>damageSound</li>
 *     <li>glitchRange</li>
 *     <li>glitchCloseRange</li>
 *     <li>glitchViewAngle</li>
 *     <li>lobbyAtmosphereShare</li>
 *     <li>pageGlitchEnabled</li>
 *     <li>pageGlitchPulseSeconds</li>
 *     <li>pageGlitchMaxLevel</li>
 *     <li>creek.* (see {@link CreekConfig})</li>
 *     <li>sanity.* (see {@link SanityConfig})</li>
 *     <li>stamina.* (see {@link StaminaConfig})</li>
 *     <li>adrenaline.* (see {@link AdrenalineConfig})</li>
 *     <li>footprint.* (see {@link FootprintConfig})</li>
 *     <li>telemetry.* (see {@link TelemetryConfig})</li>
 *     <li>minimap.mode (see {@link MinimapConfig})</li>
 * </ul>
 * <p>
 * If a property can not be found in the file, the default value will be used.
 * The default values are defined in {@link GameConfig#DEFAULT}.
 *
 * @author theEvilReaper
 * @version 1.11.0
 * @see GameConfig
 * @since 1.0.0
 */
public final class GameConfigReader {

    private static final Logger CONFIG_LOGGER = LoggerFactory.getLogger(GameConfigReader.class);
    private static final String SENTRY_DSN_KEY = "sentryDsn";
    private static final String RESOURCE_PACK_URL_KEY = "resourcePackUrl";
    private static final String RESOURCE_PACK_SHA1_KEY = "resourcePackSha1";
    private static final Pattern SHA1_PATTERN = Pattern.compile("[0-9a-fA-F]{40}");
    private static final String PAGE_PROXIMITY_SOUND_KEY = "pageProximitySound";
    private static final String DAMAGE_SOUND_KEY = "damageSound";
    private static final String CREEK_PREFIX = "creek.";
    private static final String SANITY_PREFIX = "sanity.";
    private static final String STAMINA_PREFIX = "stamina.";
    private static final String ADRENALINE_PREFIX = "adrenaline.";
    private static final String FOOTPRINT_PREFIX = "footprint.";
    private static final String TELEMETRY_PREFIX = "telemetry.";
    private static final String MINIMAP_MODE_KEY = "minimap.mode";

    private final Path path;

    /**
     * Creates a new instance of the {@link GameConfigReader}.
     * The path must be the root directory of the game.
     *
     * @param path the root directory of the game
     */
    public GameConfigReader(Path path) {
        this.path = path.resolve("config.properties");
    }

    /**
     * Reads the properties file and creates a new {@link GameConfig} instance.
     * If the file does not exist or is empty the default values will be used.
     *
     * @return the new game configuration
     */
    public GameConfig getConfig() {
        if (!Files.exists(path)) {
            CONFIG_LOGGER.warn("No config file found. Using default values");
            return GameConfig.DEFAULT;
        }

        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(path)) {
            properties.load(stream);
        } catch (Exception exception) {
            CONFIG_LOGGER.error("Failed to load config file", exception);
            return GameConfig.DEFAULT;
        }

        if (properties.isEmpty()) {
            CONFIG_LOGGER.warn("Found config file but it is empty. Falling back to default values");
            return GameConfig.DEFAULT;
        }

        GameConfig.Round round = GameConfig.Round.DEFAULT;
        GameConfig.Teams teams = GameConfig.Teams.DEFAULT;
        GameConfig.PageProximity proximity = GameConfig.PageProximity.DEFAULT;
        GameConfig.DamageSound damage = GameConfig.DamageSound.DEFAULT;
        GameConfig.Glitch glitch = GameConfig.Glitch.DEFAULT;
        GameConfig.PageGlitch pageGlitch = GameConfig.PageGlitch.DEFAULT;
        return new GameConfig(
                new GameConfig.Round(
                        getInt(properties, "minPlayers", round.minPlayers()),
                        getInt(properties, "maxPlayers", round.maxPlayers()),
                        getInt(properties, "lobbyTime", round.lobbyTime()),
                        getInt(properties, "gameTime", round.gameTime())
                ),
                new GameConfig.Teams(
                        getInt(properties, "slenderTeamSize", teams.slenderSize()),
                        getInt(properties, "survivorTeamSize", teams.survivorSize())
                ),
                getString(properties, SENTRY_DSN_KEY),
                new GameConfig.ResourcePack(getResourcePackUrl(properties), getResourcePackSha1(properties)),
                new GameConfig.PageProximity(
                        getBoolean(properties, "pageProximityEnabled", proximity.enabled()),
                        getInt(properties, "pageProximityRange", proximity.range()),
                        getSound(properties, PAGE_PROXIMITY_SOUND_KEY, proximity.sound()),
                        getFloat(properties, "pageProximityVolumeFactor", proximity.volumeFactor())
                ),
                new GameConfig.DamageSound(
                        getBoolean(properties, "damageSoundEnabled", damage.enabled()),
                        getInt(properties, "damageSoundCooldown", damage.cooldown()),
                        getSound(properties, DAMAGE_SOUND_KEY, damage.sound())
                ),
                new GameConfig.Glitch(
                        getInt(properties, "glitchRange", glitch.range()),
                        getInt(properties, "glitchCloseRange", glitch.closeRange()),
                        getInt(properties, "glitchViewAngle", glitch.viewAngle())
                ),
                new GameConfig.PageGlitch(
                        getBoolean(properties, "pageGlitchEnabled", pageGlitch.enabled()),
                        getInt(properties, "pageGlitchPulseSeconds", pageGlitch.pulseSeconds()),
                        getInt(properties, "pageGlitchMaxLevel", pageGlitch.maxLevel())
                ),
                getCreek(properties),
                getSanity(properties),
                getStamina(properties),
                getAdrenaline(properties),
                getFootprint(properties),
                getTelemetry(properties),
                getMinimap(properties),
                getFloat(properties, "lobbyAtmosphereShare", GameConfig.DEFAULT.lobbyAtmosphereShare())
        );
    }

    private int getInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException _) {
            CONFIG_LOGGER.warn("Failed to parse integer config value for key '{}': '{}'. Falling back to default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads a decimal from the properties. Follows {@link #getInt} in falling back rather than
     * failing: a value the operator cannot have meant is not worth taking the service down for.
     *
     * @param properties   the loaded properties
     * @param key          the key to read
     * @param defaultValue the value to use when the key is absent or unreadable
     * @return the parsed value
     */
    private float getFloat(Properties properties, String key, float defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException _) {
            CONFIG_LOGGER.warn("Failed to parse decimal config value for key '{}': '{}'. Falling back to default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads a decimal with double precision. The creek's values are compared with each other,
     * and float rounding would turn 0.6 into 0.6000000238.
     *
     * @param properties   the loaded properties
     * @param key          the key to read
     * @param defaultValue the value to use when the key is absent or unreadable
     * @return the parsed value
     */
    private double getDouble(Properties properties, String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException _) {
            CONFIG_LOGGER.warn("Failed to parse decimal config value for key '{}': '{}'. Falling back to default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads the creek settings. All keys start with {@value #CREEK_PREFIX}.
     * <p>
     * Like every other group, an unreadable value falls back to its default, while values that
     * contradict each other are rejected by {@link CreekConfig}.
     * </p>
     *
     * @param properties the loaded properties
     * @return the creek settings, never {@code null}
     * @throws IllegalArgumentException if the values do not fit together
     */
    private CreekConfig getCreek(Properties properties) {
        CreekConfig defaults = CreekConfig.DEFAULT;
        return new CreekConfig(
                getBoolean(properties, CREEK_PREFIX + "enabled", defaults.enabled()),
                getBoolean(properties, CREEK_PREFIX + "activeWithLastSurvivor", defaults.activeWithLastSurvivor()),
                getInt(properties, CREEK_PREFIX + "sightRange", defaults.sightRange()),
                getInt(properties, CREEK_PREFIX + "sightViewAngle", defaults.sightViewAngle()),
                getInt(properties, CREEK_PREFIX + "wanderPauseMillis", defaults.wanderPauseMillis()),
                getDouble(properties, CREEK_PREFIX + "wanderSpeed", defaults.wanderSpeed()),
                getDouble(properties, CREEK_PREFIX + "huntSpeed", defaults.huntSpeed()),
                getDouble(properties, CREEK_PREFIX + "stalkThreshold", defaults.stalkThreshold()),
                getDouble(properties, CREEK_PREFIX + "huntThreshold", defaults.huntThreshold()),
                getInt(properties, CREEK_PREFIX + "stalkMinDistance", defaults.stalkMinDistance()),
                getInt(properties, CREEK_PREFIX + "stalkMaxDistance", defaults.stalkMaxDistance()),
                getInt(properties, CREEK_PREFIX + "stalkMinAngle", defaults.stalkMinAngle()),
                getInt(properties, CREEK_PREFIX + "stalkMaxAngle", defaults.stalkMaxAngle()),
                getInt(properties, CREEK_PREFIX + "stalkRevealMillis", defaults.stalkRevealMillis()),
                getInt(properties, CREEK_PREFIX + "stalkMinSeconds", defaults.stalkMinSeconds()),
                getInt(properties, CREEK_PREFIX + "stalkMaxSeconds", defaults.stalkMaxSeconds()),
                getInt(properties, CREEK_PREFIX + "huntMaxSeconds", defaults.huntMaxSeconds()),
                getInt(properties, CREEK_PREFIX + "huntMinStalkSeconds", defaults.huntMinStalkSeconds()),
                getInt(properties, CREEK_PREFIX + "huntCooldownSeconds", defaults.huntCooldownSeconds()),
                getDouble(properties, CREEK_PREFIX + "catchDistance", defaults.catchDistance()),
                getInt(properties, CREEK_PREFIX + "vanishMinSeconds", defaults.vanishMinSeconds()),
                getInt(properties, CREEK_PREFIX + "vanishMaxSeconds", defaults.vanishMaxSeconds()),
                getInt(properties, CREEK_PREFIX + "respawnMinDistance", defaults.respawnMinDistance()),
                getInt(properties, CREEK_PREFIX + "personalSpace", defaults.personalSpace()),
                getInt(properties, CREEK_PREFIX + "stuckMillis", defaults.stuckMillis()),
                getInt(properties, CREEK_PREFIX + "betrayalCatchCount", defaults.betrayalCatchCount()),
                getDouble(properties, CREEK_PREFIX + "betrayalChance", defaults.betrayalChance()),
                getInt(properties, CREEK_PREFIX + "betrayalGlowSeconds", defaults.betrayalGlowSeconds()),
                getInt(properties, CREEK_PREFIX + "slownessSeconds", defaults.slownessSeconds()),
                getDouble(properties, CREEK_PREFIX + "routeLinkDistance", defaults.routeLinkDistance()),
                getDouble(properties, CREEK_PREFIX + "randomStopChance", defaults.randomStopChance()),
                getInt(properties, CREEK_PREFIX + "randomStopMinMillis", defaults.randomStopMinMillis()),
                getInt(properties, CREEK_PREFIX + "randomStopMaxMillis", defaults.randomStopMaxMillis()),
                getDouble(properties, CREEK_PREFIX + "launchHeight", defaults.launchHeight()),
                getDouble(properties, CREEK_PREFIX + "swapChance", defaults.swapChance()),
                getDouble(properties, CREEK_PREFIX + "launchDamage", defaults.launchDamage())
        );
    }

    /**
     * Reads the fear settings. All keys start with {@value #SANITY_PREFIX}.
     * <p>
     * An unreadable value falls back to its default, while values out of range are rejected by
     * {@link SanityConfig}.
     * </p>
     *
     * @param properties the loaded properties
     * @return the fear settings, never {@code null}
     * @throws IllegalArgumentException if a value is out of range
     */
    private SanityConfig getSanity(Properties properties) {
        SanityConfig defaults = SanityConfig.DEFAULT;
        return new SanityConfig(
                getDouble(properties, SANITY_PREFIX + "pageFloorWeight", defaults.pageFloorWeight()),
                getDouble(properties, SANITY_PREFIX + "pageFoundGain", defaults.pageFoundGain()),
                getDouble(properties, SANITY_PREFIX + "sightingGain", defaults.sightingGain()),
                getInt(properties, SANITY_PREFIX + "sightingCooldownSeconds", defaults.sightingCooldownSeconds()),
                getDouble(properties, SANITY_PREFIX + "caughtGain", defaults.caughtGain()),
                getDouble(properties, SANITY_PREFIX + "selectedGain", defaults.selectedGain()),
                getDouble(properties, SANITY_PREFIX + "deathGain", defaults.deathGain()),
                getDouble(properties, SANITY_PREFIX + "decayPerSecond", defaults.decayPerSecond()),
                getDouble(properties, SANITY_PREFIX + "stalkGainPerSecond", defaults.stalkGainPerSecond()),
                getDouble(properties, SANITY_PREFIX + "residualShare", defaults.residualShare()),
                getDouble(properties, SANITY_PREFIX + "timeFloorWeight", defaults.timeFloorWeight()),
                getDouble(properties, SANITY_PREFIX + "floorCap", defaults.floorCap())
        );
    }

    /**
     * Reads the sprint settings. All keys start with {@value #STAMINA_PREFIX}.
     * <p>
     * An unreadable value falls back to its default, while values out of range are rejected by
     * {@link StaminaConfig}.
     * </p>
     *
     * @param properties the loaded properties
     * @return the sprint settings, never {@code null}
     * @throws IllegalArgumentException if a value is out of range
     */
    private StaminaConfig getStamina(Properties properties) {
        StaminaConfig defaults = StaminaConfig.DEFAULT;
        return new StaminaConfig(
                getDouble(properties, STAMINA_PREFIX + "sprintResumeShare", defaults.sprintResumeShare()),
                getDouble(properties, STAMINA_PREFIX + "regenPerSecond", defaults.regenPerSecond()),
                getInt(properties, STAMINA_PREFIX + "slenderReappearCooldownSeconds", defaults.slenderReappearCooldownSeconds()),
                getInt(properties, STAMINA_PREFIX + "slenderDamageRange", defaults.slenderDamageRange())
        );
    }

    /**
     * Reads the telemetry settings. All keys start with {@value #TELEMETRY_PREFIX}.
     * <p>
     * An unreadable value falls back to its default, while values out of range are rejected by
     * {@link TelemetryConfig}.
     * </p>
     *
     * @param properties the loaded properties
     * @return the telemetry settings, never {@code null}
     * @throws IllegalArgumentException if a value is out of range
     */
    private TelemetryConfig getTelemetry(Properties properties) {
        return new TelemetryConfig(getInt(properties, TELEMETRY_PREFIX + "slowTickThresholdMillis",
                TelemetryConfig.DEFAULT.slowTickThresholdMillis()));
    }

    /**
     * Reads the minimap mode. Like every unreadable value, an unknown mode falls back to the default.
     *
     * @param properties the loaded properties
     * @return the minimap settings, never {@code null}
     */
    private MinimapConfig getMinimap(Properties properties) {
        String value = getString(properties, MINIMAP_MODE_KEY);
        if (value == null) {
            return MinimapConfig.DEFAULT;
        }
        for (MinimapConfig.Mode mode : MinimapConfig.Mode.values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return new MinimapConfig(mode);
            }
        }
        CONFIG_LOGGER.warn("'{}' is not a minimap mode (disabled, fair, off): '{}'. Falling back to default: {}",
                MINIMAP_MODE_KEY, value, MinimapConfig.DEFAULT.mode());
        return MinimapConfig.DEFAULT;
    }

    /**
     * Reads the adrenaline settings. All keys start with {@value #ADRENALINE_PREFIX}.
     * <p>
     * An unreadable value falls back to its default, while values out of range are rejected by
     * {@link AdrenalineConfig}.
     * </p>
     *
     * @param properties the loaded properties
     * @return the adrenaline settings, never {@code null}
     * @throws IllegalArgumentException if a value is out of range
     */
    private AdrenalineConfig getAdrenaline(Properties properties) {
        AdrenalineConfig defaults = AdrenalineConfig.DEFAULT;
        return new AdrenalineConfig(
                getInt(properties, ADRENALINE_PREFIX + "radius", defaults.radius()),
                getDouble(properties, ADRENALINE_PREFIX + "speedBonus", defaults.speedBonus()),
                getInt(properties, ADRENALINE_PREFIX + "durationSeconds", defaults.durationSeconds()),
                getInt(properties, ADRENALINE_PREFIX + "cooldownSeconds", defaults.cooldownSeconds())
        );
    }

    private FootprintConfig getFootprint(Properties properties) {
        FootprintConfig defaults = FootprintConfig.DEFAULT;
        return new FootprintConfig(
                getDouble(properties, FOOTPRINT_PREFIX + "slenderStepBlocks", defaults.slenderStepBlocks()),
                getDouble(properties, FOOTPRINT_PREFIX + "slenderChance", defaults.slenderChance()),
                getInt(properties, FOOTPRINT_PREFIX + "slenderDelayMinMillis", defaults.slenderDelayMinMillis()),
                getInt(properties, FOOTPRINT_PREFIX + "slenderDelayMaxMillis", defaults.slenderDelayMaxMillis()),
                getInt(properties, FOOTPRINT_PREFIX + "slenderLifetimeSeconds", defaults.slenderLifetimeSeconds()),
                getDouble(properties, FOOTPRINT_PREFIX + "survivorSampleBlocks", defaults.survivorSampleBlocks()),
                getInt(properties, FOOTPRINT_PREFIX + "survivorHistorySeconds", defaults.survivorHistorySeconds()),
                getInt(properties, FOOTPRINT_PREFIX + "scanRadius", defaults.scanRadius()),
                getInt(properties, FOOTPRINT_PREFIX + "scanGapSeconds", defaults.scanGapSeconds()),
                getInt(properties, FOOTPRINT_PREFIX + "scanMaxPrints", defaults.scanMaxPrints()),
                getInt(properties, FOOTPRINT_PREFIX + "scanLifetimeSeconds", defaults.scanLifetimeSeconds()),
                getInt(properties, FOOTPRINT_PREFIX + "scanCooldownSeconds", defaults.scanCooldownSeconds()),
                getDouble(properties, FOOTPRINT_PREFIX + "teleportBlocks", defaults.teleportBlocks()),
                getDouble(properties, FOOTPRINT_PREFIX + "minSpacing", defaults.minSpacing()),
                getDouble(properties, FOOTPRINT_PREFIX + "fadeShare", defaults.fadeShare())
        );
    }

    /**
     * Reads a flag from the properties. Anything other than {@code true} or {@code false} is not a
     * decision the operator made on purpose, so it falls back to the default instead of silently
     * counting as {@code false} the way {@link Boolean#parseBoolean(String)} would.
     *
     * @param properties   the loaded properties
     * @param key          the key to read
     * @param defaultValue the value to use when the key is absent or unreadable
     * @return the parsed flag
     */
    private boolean getBoolean(Properties properties, String key, boolean defaultValue) {
        String value = getString(properties, key);
        if (value == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        CONFIG_LOGGER.warn("Failed to parse boolean config value for key '{}': '{}'. Falling back to default: {}", key, value, defaultValue);
        return defaultValue;
    }

    /**
     * Reads a sound key from the properties. Only the key syntax is checked here - whether the key
     * names a sound the client knows is not something this reader can answer.
     *
     * @param properties   the loaded properties
     * @param key          the key to read
     * @param defaultValue the sound to use when the key is absent or malformed
     * @return the parsed sound key
     */
    private Key getSound(Properties properties, String key, Key defaultValue) {
        String value = getString(properties, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Key.key(value);
        } catch (InvalidKeyException exception) {
            CONFIG_LOGGER.warn("'{}' is not a valid sound key: '{}'. Falling back to default: {}", key, value, defaultValue, exception);
            return defaultValue;
        }
    }

    /**
     * Reads a trimmed value from the properties.
     *
     * @param properties the loaded properties
     * @param key        the key to read
     * @return the value, or {@code null} if the key is absent or holds nothing but whitespace
     */
    private @Nullable String getString(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Reads the ResourcePack location. A value that is not a valid URI turns the feature off
     * instead of failing the start - a broken pack URL is not worth taking the service down for.
     *
     * @param properties the loaded properties
     * @return the parsed URL, or {@code null} if it is absent or unusable
     */
    private @Nullable URI getResourcePackUrl(Properties properties) {
        String value = getString(properties, RESOURCE_PACK_URL_KEY);
        if (value == null) {
            return null;
        }
        try {
            return URI.create(value);
        } catch (IllegalArgumentException exception) {
            CONFIG_LOGGER.warn("'{}' is not a valid URI: '{}'. Disabling the ResourcePack feature", RESOURCE_PACK_URL_KEY, value, exception);
            return null;
        }
    }

    /**
     * Reads the ResourcePack checksum. Anything that is not 40 hexadecimal characters is not a
     * SHA-1 and is dropped, which leaves the checksum to be computed from the pack at runtime.
     *
     * @param properties the loaded properties
     * @return the checksum, or {@code null} if it is absent or malformed
     */
    private @Nullable String getResourcePackSha1(Properties properties) {
        String value = getString(properties, RESOURCE_PACK_SHA1_KEY);
        if (value == null) {
            return null;
        }
        if (!SHA1_PATTERN.matcher(value).matches()) {
            CONFIG_LOGGER.warn("'{}' is not a SHA-1 checksum: '{}'. It will be computed from the pack instead", RESOURCE_PACK_SHA1_KEY, value);
            return null;
        }
        return value;
    }
}
