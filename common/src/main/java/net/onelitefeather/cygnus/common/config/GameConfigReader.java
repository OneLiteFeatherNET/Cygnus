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
 *     <li>possession.* (see {@link PossessionConfig})</li>
 *     <li>telemetry.* (see {@link TelemetryConfig})</li>
 *     <li>minimap.mode (see {@link MinimapConfig})</li>
 * </ul>
 * <p>
 * If a property can not be found in the file, the default value will be used.
 * The default values are defined in {@link GameConfig#DEFAULT}.
 *
 * @author theEvilReaper
 * @version 1.12.0
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

        return GameConfig.read(ConfigSection.root(properties));
    }
}
