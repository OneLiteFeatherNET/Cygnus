package net.onelitefeather.cygnus.common.config;

import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.function.Function;

/**
 * Settings for the yearly player statistics.
 * <p>
 * The statistics are opt-in: without a {@code stats.jdbc-url} nothing connects to a database and the
 * game runs as it did before. The password can stay out of the file and come from the environment
 * variable {@value #PASSWORD_ENV} instead.
 * </p>
 *
 * @param jdbcUrl  the JDBC URL of the shared database, or {@code null} to keep the statistics off
 * @param username the database user, empty for none
 * @param password the database password, empty for none
 * @param poolSize how many connections the pool may hold, at least 1
 * @param timeZone the zone whose calendar decides which year a finished round counts for
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record StatsConfig(@Nullable String jdbcUrl, String username, String password, int poolSize, ZoneId timeZone) {

    /** The environment variable that supplies the password when the file has none. */
    public static final String PASSWORD_ENV = "CYGNUS_STATS_PASSWORD";

    /** Four connections: one writer per round end plus room for an occasional deletion. */
    public static final int DEFAULT_POOL_SIZE = 4;

    /** The players' calendar, so New Year's Eve at 00:30 local time counts for the new year. */
    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Europe/Berlin");

    /** Statistics off. */
    public static final StatsConfig DISABLED = new StatsConfig(null, "", "", DEFAULT_POOL_SIZE, DEFAULT_TIME_ZONE);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public StatsConfig {
        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize (" + poolSize + ") must be at least 1");
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
     * Reads the statistics settings. The section carries the {@code stats.} prefix.
     *
     * @param section the {@code stats.} part of the config
     * @param env     looks up an environment variable, {@code null} for an unset one
     * @return the statistics settings
     */
    public static StatsConfig read(ConfigSection section, Function<String, @Nullable String> env) {
        String url = section.getString("jdbc-url");
        if (url == null) {
            return DISABLED;
        }
        return section.orDefault("stats", () -> {
            String password = section.getString("password");
            if (password == null) {
                password = env.apply(PASSWORD_ENV);
            }
            return new StatsConfig(
                    url,
                    orEmpty(section.getString("username")),
                    orEmpty(password),
                    section.getInt("pool-size", DEFAULT_POOL_SIZE),
                    readZone(section));
        }, DISABLED);
    }

    private static ZoneId readZone(ConfigSection section) {
        String zone = section.getString("time-zone");
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
                + ", timeZone=" + this.timeZone + "]";
    }
}
