package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Settings for the yearly player statistics, read from the {@code stats.} keys of the game's
 * {@code config.properties}.
 * <p>
 * The statistics are opt-in: without a {@code stats.jdbc-url} nothing connects to a database and the
 * game runs as it did before. The password can stay out of the file and come from the environment
 * variable {@value #PASSWORD_ENV} instead. Values that cannot be read, or that break a rule, fall back
 * to the disabled settings instead of taking the service down.
 * </p>
 *
 * @param jdbcUrl  the JDBC URL of the shared database, or {@code null} to keep the statistics off
 * @param username the database user, empty for none
 * @param password the database password, empty for none
 * @param poolSize how many connections the pool may hold, at least 1
 * @param schema   the database schema that holds the statistics tables and Flyway's history, a plain lower-case SQL identifier
 * @param timeZone the zone whose calendar decides which year a finished round counts for
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public record StatsConfig(@Nullable String jdbcUrl, String username, String password, int poolSize, String schema,
                          ZoneId timeZone) {

    /** The environment variable that supplies the password when the file has none. */
    public static final String PASSWORD_ENV = "CYGNUS_STATS_PASSWORD";

    /** Four connections: one writer per round end plus room for an occasional deletion. */
    public static final int DEFAULT_POOL_SIZE = 4;

    /**
     * Their own schema, never {@code public}: that one belongs to the database's operator (for example the
     * objects a managed PostgreSQL puts there) and Flyway refuses to start in a non-empty schema.
     */
    public static final String DEFAULT_SCHEMA = "cygnus_stats";

    /** PostgreSQL cuts identifiers at 63 bytes. */
    private static final int MAX_SCHEMA_LENGTH = 63;

    private static final Pattern SCHEMA_PATTERN = Pattern.compile("[a-z_][a-z0-9_]*");

    /** The players' calendar, so New Year's Eve at 00:30 local time counts for the new year. */
    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Europe/Berlin");

    /** Statistics off. */
    public static final StatsConfig DISABLED = new StatsConfig(null, "", "", DEFAULT_POOL_SIZE, DEFAULT_SCHEMA, DEFAULT_TIME_ZONE);

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsConfig.class);
    private static final String PREFIX = "stats.";

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public StatsConfig {
        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize (" + poolSize + ") must be at least 1");
        }
        if (!validSchema(schema)) {
            throw new IllegalArgumentException("schema (" + schema + ") must match [a-z_][a-z0-9_]* and have at most "
                    + MAX_SCHEMA_LENGTH + " characters");
        }
    }

    /**
     * Tells whether statistics are written at all.
     *
     * @return {@code true} if a JDBC URL is configured
     */
    public boolean enabled() {
        return this.jdbcUrl != null;
    }

    /**
     * Loads the settings from the {@code config.properties} in the given directory. A missing or
     * unreadable file means statistics off.
     *
     * @param directory the root directory of the game
     * @param env       looks up an environment variable, {@code null} for an unset one
     * @return the statistics settings
     */
    public static StatsConfig load(Path directory, Function<String, @Nullable String> env) {
        Path file = directory.resolve("config.properties");
        if (!Files.exists(file)) {
            return DISABLED;
        }
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(file)) {
            properties.load(stream);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.error("Failed to load config file for the statistics, they stay off", exception);
            return DISABLED;
        }
        return read(properties, env);
    }

    /**
     * Reads the {@code stats.} keys.
     *
     * @param properties the loaded config file
     * @param env        looks up an environment variable, {@code null} for an unset one
     * @return the statistics settings
     */
    public static StatsConfig read(Properties properties, Function<String, @Nullable String> env) {
        String url = string(properties, "jdbc-url");
        if (url == null) {
            return DISABLED;
        }
        try {
            String password = string(properties, "password");
            if (password == null) {
                password = env.apply(PASSWORD_ENV);
            }
            return new StatsConfig(
                    url,
                    orEmpty(string(properties, "username")),
                    orEmpty(password),
                    integer(properties, "pool-size", DEFAULT_POOL_SIZE),
                    schema(properties),
                    zone(properties));
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid values for stats: {}. Falling back to its defaults", exception.getMessage());
            return DISABLED;
        }
    }

    private static @Nullable String string(Properties properties, String key) {
        String value = properties.getProperty(PREFIX + key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int integer(Properties properties, String key, int fallback) {
        String value = string(properties, key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            LOGGER.warn("Failed to parse integer config value for key '{}{}': '{}'. Falling back to default: {}",
                    PREFIX, key, value, fallback);
            return fallback;
        }
    }

    private static boolean validSchema(String schema) {
        return schema.length() <= MAX_SCHEMA_LENGTH && SCHEMA_PATTERN.matcher(schema).matches();
    }

    private static String schema(Properties properties) {
        String schema = string(properties, "schema");
        if (schema == null) {
            return DEFAULT_SCHEMA;
        }
        if (!validSchema(schema)) {
            LOGGER.warn("Invalid value for key '{}schema': '{}'. It must be a lower-case SQL identifier"
                    + " ([a-z_][a-z0-9_]*, at most {} characters). Falling back to default: {}",
                    PREFIX, schema, MAX_SCHEMA_LENGTH, DEFAULT_SCHEMA);
            return DEFAULT_SCHEMA;
        }
        return schema;
    }

    private static ZoneId zone(Properties properties) {
        String zone = string(properties, "time-zone");
        if (zone == null) {
            return DEFAULT_TIME_ZONE;
        }
        try {
            return ZoneId.of(zone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("time-zone (" + zone + ") is not a known zone", exception);
        }
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    /**
     * Describes the settings without the password.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "StatsConfig[jdbcUrl=" + this.jdbcUrl + ", username=" + this.username + ", poolSize=" + this.poolSize
                + ", schema=" + this.schema
                + ", timeZone=" + this.timeZone + "]";
    }
}
