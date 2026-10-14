package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers reading the {@code stats.} keys. The environment is a function handed in, never the real one.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class StatsConfigTest {

    private static final Function<String, String> NO_ENV = _ -> null;

    private static StatsConfig read(Map<String, String> entries, Function<String, String> env) {
        Properties properties = new Properties();
        entries.forEach(properties::setProperty);
        return StatsConfig.read(ConfigSection.root(properties).section("stats."), env);
    }

    @Test
    @DisplayName("Without a JDBC URL the statistics are disabled")
    void missingUrlDisablesStatistics() {
        StatsConfig config = read(Map.of(), NO_ENV);

        assertFalse(config.enabled(), "no stats.jdbc-url must mean no database");
        assertEquals(StatsConfig.DISABLED, config);
    }

    @Test
    @DisplayName("A blank JDBC URL counts as missing")
    void blankUrlDisablesStatistics() {
        assertFalse(read(Map.of("stats.jdbc-url", "   "), NO_ENV).enabled());
    }

    @Test
    @DisplayName("A JDBC URL enables the statistics with the documented defaults")
    void urlEnablesStatisticsWithDefaults() {
        StatsConfig config = read(Map.of("stats.jdbc-url", "jdbc:postgresql://db/cygnus"), NO_ENV);

        assertTrue(config.enabled());
        assertEquals(StatsConfig.DEFAULT_POOL_SIZE, config.poolSize());
        assertEquals(ZoneId.of("Europe/Berlin"), config.timeZone());
    }

    @Test
    @DisplayName("The password falls back to CYGNUS_STATS_PASSWORD")
    void passwordFallsBackToEnvironment() {
        StatsConfig config = read(Map.of("stats.jdbc-url", "jdbc:postgresql://db/cygnus"),
                name -> StatsConfig.PASSWORD_ENV.equals(name) ? "from-env" : null);

        assertEquals("from-env", config.password());
    }

    @Test
    @DisplayName("A password in the file wins over the environment")
    void passwordInFileWinsOverEnvironment() {
        StatsConfig config = read(Map.of("stats.jdbc-url", "jdbc:postgresql://db/cygnus", "stats.password", "from-file"),
                _ -> "from-env");

        assertEquals("from-file", config.password());
    }

    @Test
    @DisplayName("Username, pool size and time zone are read")
    void valuesAreRead() {
        StatsConfig config = read(Map.of(
                "stats.jdbc-url", "jdbc:postgresql://db/cygnus",
                "stats.username", "cygnus",
                "stats.pool-size", "7",
                "stats.time-zone", "UTC"), NO_ENV);

        assertEquals("cygnus", config.username());
        assertEquals(7, config.poolSize());
        assertEquals(ZoneId.of("UTC"), config.timeZone());
    }

    @Test
    @DisplayName("A pool size below 1 is rejected and names the field")
    void poolSizeBelowOneIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new StatsConfig("jdbc:h2:mem:x", "", "", 0, ZoneId.of("UTC")));

        assertTrue(exception.getMessage().contains("poolSize"), exception.getMessage());
    }

    @Test
    @DisplayName("An unknown time zone falls back to the disabled defaults instead of failing the start")
    void unknownTimeZoneFallsBack() {
        StatsConfig config = read(Map.of("stats.jdbc-url", "jdbc:postgresql://db/cygnus", "stats.time-zone", "Mars/Olympus"), NO_ENV);

        assertEquals(StatsConfig.DISABLED, config);
    }

    @Test
    @DisplayName("The password never shows up in toString")
    void toStringHidesPassword() {
        StatsConfig config = new StatsConfig("jdbc:h2:mem:x", "cygnus", "secret", 4, ZoneId.of("UTC"));

        assertFalse(config.toString().contains("secret"), config.toString());
    }

    @Test
    @DisplayName("The whole config reads the stats section")
    void gameConfigReadsStatsSection() {
        Properties properties = new Properties();
        properties.setProperty("stats.jdbc-url", "jdbc:postgresql://db/cygnus");

        GameConfig config = GameConfig.read(ConfigSection.root(properties));

        assertTrue(config.stats().enabled());
    }
}
