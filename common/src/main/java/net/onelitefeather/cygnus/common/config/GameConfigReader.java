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
 *     <li>pageProximityInterval</li>
 *     <li>pageProximitySound</li>
 *     <li>pageProximityVolumeFactor</li>
 *     <li>damageSoundEnabled</li>
 *     <li>damageSoundCooldown</li>
 *     <li>damageSound</li>
 *     <li>glitchRange</li>
 *     <li>glitchCloseRange</li>
 *     <li>glitchViewAngle</li>
 *     <li>slenderStaticEnabled</li>
 *     <li>slenderStaticSound</li>
 *     <li>slenderStaticQuietInterval</li>
 *     <li>slenderStaticFranticInterval</li>
 *     <li>slenderStaticMinVolume</li>
 *     <li>slenderStaticMaxVolume</li>
 * </ul>
 * <p>
 * If a property can not be found in the file, the default value will be used.
 * The default values are defined in the {@link InternalGameConfig} class.
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
            return InternalGameConfig.defaultConfig();
        }

        Properties properties = new Properties();

        try (InputStream stream = Files.newInputStream(path)) {
            properties.load(stream);
        } catch (Exception exception) {
            CONFIG_LOGGER.error("Failed to load config file", exception);
            return InternalGameConfig.defaultConfig();
        }

        if (properties.isEmpty()) {
            CONFIG_LOGGER.warn("Found config file but it is empty. Falling back to default values");
            return InternalGameConfig.defaultConfig();
        }

        GameConfig internal = InternalGameConfig.defaultConfig();
        GameConfig.Builder configBuilder = GameConfig.builder();

        configBuilder.minPlayers(getInt(properties, "minPlayers", internal.minPlayers()))
                .maxPlayers(getInt(properties, "maxPlayers", internal.maxPlayers()))
                .lobbyTime(getInt(properties, "lobbyTime", internal.lobbyTime()))
                .gameTime(getInt(properties, "gameTime", internal.gameTime()))
                .survivorTeamSize(getInt(properties, "survivorTeamSize", internal.survivorTeamSize()))
                .slenderTeamSize(getInt(properties, "slenderTeamSize", internal.slenderTeamSize()))
                .sentryDsn(getString(properties, SENTRY_DSN_KEY))
                .resourcePackUrl(getResourcePackUrl(properties))
                .resourcePackSha1(getResourcePackSha1(properties))
                .pageProximityEnabled(getBoolean(properties, "pageProximityEnabled", internal.pageProximityEnabled()))
                .pageProximityRange(getInt(properties, "pageProximityRange", internal.pageProximityRange()))
                .pageProximityInterval(getInt(properties, "pageProximityInterval", internal.pageProximityInterval()))
                .pageProximitySound(getSound(properties, PAGE_PROXIMITY_SOUND_KEY, internal.pageProximitySound()))
                .pageProximityVolumeFactor(getFloat(properties, "pageProximityVolumeFactor", internal.pageProximityVolumeFactor()))
                .damageSoundEnabled(getBoolean(properties, "damageSoundEnabled", internal.damageSoundEnabled()))
                .damageSoundCooldown(getInt(properties, "damageSoundCooldown", internal.damageSoundCooldown()))
                .damageSound(getSound(properties, DAMAGE_SOUND_KEY, internal.damageSound()))
                .glitchRange(getInt(properties, "glitchRange", internal.glitchRange()))
                .glitchCloseRange(getInt(properties, "glitchCloseRange", internal.glitchCloseRange()))
                .glitchViewAngle(getInt(properties, "glitchViewAngle", internal.glitchViewAngle()))
                .slenderStaticEnabled(getBoolean(properties, "slenderStaticEnabled", internal.slenderStaticEnabled()))
                .slenderStaticSound(getSound(properties, SLENDER_STATIC_SOUND_KEY, internal.slenderStaticSound()))
                .slenderStaticQuietInterval(getInt(properties, "slenderStaticQuietInterval", internal.slenderStaticQuietInterval()))
                .slenderStaticFranticInterval(getInt(properties, "slenderStaticFranticInterval", internal.slenderStaticFranticInterval()))
                .slenderStaticMinVolume(getFloat(properties, "slenderStaticMinVolume", internal.slenderStaticMinVolume()))
                .slenderStaticMaxVolume(getFloat(properties, "slenderStaticMaxVolume", internal.slenderStaticMaxVolume()));

        return configBuilder.build();
    }

    private int getInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
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
        } catch (NumberFormatException exception) {
            CONFIG_LOGGER.warn("Failed to parse decimal config value for key '{}': '{}'. Falling back to default: {}", key, value, defaultValue);
            return defaultValue;
        }
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
