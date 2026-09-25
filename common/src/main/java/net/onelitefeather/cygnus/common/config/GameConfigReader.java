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
 *     <li>slenderStaticEnabled</li>
 *     <li>slenderStaticSound</li>
 *     <li>slenderStaticQuietInterval</li>
 *     <li>slenderStaticFranticInterval</li>
 *     <li>slenderStaticMinVolume</li>
 *     <li>slenderStaticMaxVolume</li>
 *     <li>creek.* (see {@link CreekConfig})</li>
 * </ul>
 * <p>
 * If a property can not be found in the file, the default value will be used.
 * The default values are defined in {@link GameConfig#DEFAULT}.
 *
 * @author theEvilReaper
 * @version 1.4.0
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
    private static final String SLENDER_STATIC_SOUND_KEY = "slenderStaticSound";
    private static final String CREEK_PREFIX = "creek.";

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
        GameConfig.SlenderStatic slenderStatic = GameConfig.SlenderStatic.DEFAULT;
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
                new GameConfig.SlenderStatic(
                        getBoolean(properties, "slenderStaticEnabled", slenderStatic.enabled()),
                        getSound(properties, SLENDER_STATIC_SOUND_KEY, slenderStatic.sound()),
                        getInt(properties, "slenderStaticQuietInterval", slenderStatic.quietInterval()),
                        getInt(properties, "slenderStaticFranticInterval", slenderStatic.franticInterval()),
                        getFloat(properties, "slenderStaticMinVolume", slenderStatic.minVolume()),
                        getFloat(properties, "slenderStaticMaxVolume", slenderStatic.maxVolume())
                ),
                getCreek(properties),
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
        CreekConfig d = CreekConfig.DEFAULT;
        return new CreekConfig(
                getBoolean(properties, CREEK_PREFIX + "enabled", d.enabled()),
                getBoolean(properties, CREEK_PREFIX + "activeWithLastSurvivor", d.activeWithLastSurvivor()),
                getInt(properties, CREEK_PREFIX + "sightRange", d.sightRange()),
                getInt(properties, CREEK_PREFIX + "sightViewAngle", d.sightViewAngle()),
                getInt(properties, CREEK_PREFIX + "wanderPauseMillis", d.wanderPauseMillis()),
                getDouble(properties, CREEK_PREFIX + "wanderSpeed", d.wanderSpeed()),
                getDouble(properties, CREEK_PREFIX + "huntSpeed", d.huntSpeed()),
                getDouble(properties, CREEK_PREFIX + "stalkThreshold", d.stalkThreshold()),
                getDouble(properties, CREEK_PREFIX + "huntThreshold", d.huntThreshold()),
                getInt(properties, CREEK_PREFIX + "stalkMinDistance", d.stalkMinDistance()),
                getInt(properties, CREEK_PREFIX + "stalkMaxDistance", d.stalkMaxDistance()),
                getInt(properties, CREEK_PREFIX + "stalkMinAngle", d.stalkMinAngle()),
                getInt(properties, CREEK_PREFIX + "stalkMaxAngle", d.stalkMaxAngle()),
                getInt(properties, CREEK_PREFIX + "stalkRevealMillis", d.stalkRevealMillis()),
                getInt(properties, CREEK_PREFIX + "stalkMinSeconds", d.stalkMinSeconds()),
                getInt(properties, CREEK_PREFIX + "stalkMaxSeconds", d.stalkMaxSeconds()),
                getInt(properties, CREEK_PREFIX + "huntMaxSeconds", d.huntMaxSeconds()),
                getDouble(properties, CREEK_PREFIX + "catchDistance", d.catchDistance()),
                getInt(properties, CREEK_PREFIX + "vanishMinSeconds", d.vanishMinSeconds()),
                getInt(properties, CREEK_PREFIX + "vanishMaxSeconds", d.vanishMaxSeconds()),
                getInt(properties, CREEK_PREFIX + "respawnMinDistance", d.respawnMinDistance()),
                getInt(properties, CREEK_PREFIX + "personalSpace", d.personalSpace()),
                getInt(properties, CREEK_PREFIX + "stuckMillis", d.stuckMillis()),
                getDouble(properties, CREEK_PREFIX + "dreadPageWeight", d.dreadPageWeight()),
                getDouble(properties, CREEK_PREFIX + "dreadTimeWeight", d.dreadTimeWeight()),
                getDouble(properties, CREEK_PREFIX + "dreadIsolationWeight", d.dreadIsolationWeight()),
                getInt(properties, CREEK_PREFIX + "isolationRadius", d.isolationRadius()),
                getInt(properties, CREEK_PREFIX + "betrayalCatchCount", d.betrayalCatchCount()),
                getDouble(properties, CREEK_PREFIX + "betrayalChance", d.betrayalChance()),
                getInt(properties, CREEK_PREFIX + "betrayalGlowSeconds", d.betrayalGlowSeconds()),
                getInt(properties, CREEK_PREFIX + "slownessSeconds", d.slownessSeconds())
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
