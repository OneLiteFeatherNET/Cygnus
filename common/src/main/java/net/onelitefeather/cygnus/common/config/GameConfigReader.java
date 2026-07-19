package net.onelitefeather.cygnus.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
 * </ul>
 * <p>
 * If a property can not be found in the file, the default value will be used.
 * The default values are defined in the {@link InternalGameConfig} class.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @see GameConfig
 * @since 1.0.0
 */
public final class GameConfigReader {

    private static final Logger CONFIG_LOGGER = LoggerFactory.getLogger(GameConfigReader.class);
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
                .slenderTeamSize(getInt(properties, "slenderTeamSize", internal.slenderTeamSize()));

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
}
