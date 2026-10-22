package net.onelitefeather.cygnus.stats;

import net.onelitefeather.cygnus.stats.RoundStats.Outcome;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the statistics living in a schema of their own, next to objects the database's operator keeps in
 * {@code public}. Runs on H2 in PostgreSQL mode with lower-case identifiers, as PostgreSQL has them; every
 * test gets a database of its own.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.2
 */
class StatsSchemaIntegrationTest {

    private static final String SCHEMA = "cygnus_stats";
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        this.dataSource = source;
    }

    @AfterEach
    void tearDown() throws SQLException {
        execute("SHUTDOWN");
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = this.dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String sql) throws SQLException {
        try (Connection connection = this.dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next(), "no row for " + sql);
            return result.getLong(1);
        }
    }

    private long tablesIn(String schema, String... names) throws SQLException {
        String in = String.join("','", names);
        return count("select count(*) from information_schema.tables where table_schema = '" + schema
                + "' and table_name in ('" + in + "')");
    }

    private static FinishedRound round() {
        RoundStats stats = new RoundStats(ALICE, Role.SURVIVOR, Outcome.WIN, FinishReason.TIME_OVER, 2, 0, false, true,
                false, 600L, 600L, 0.0D, 0, 0, 3, false);
        return new FinishedRound(UUID.randomUUID(), Instant.parse("2026-06-01T12:15:00Z"), 2026, List.of(stats));
    }

    @Test
    @DisplayName("The migration works although the public schema already holds a foreign table")
    void migratesNextToForeignObjectInPublic() throws SQLException {
        execute("CREATE TABLE public.foreign_thing (id INTEGER PRIMARY KEY)");

        StatsRepositories.migrate(this.dataSource, SCHEMA);

        assertEquals(3, tablesIn(SCHEMA, "player_round_stats", "player_year_stats", "player_names"),
                "the statistics tables must exist");
    }

    @Test
    @DisplayName("A round is written and read back when public holds a foreign table")
    void writesRoundNextToForeignObjectInPublic() throws SQLException {
        execute("CREATE TABLE public.foreign_thing (id INTEGER PRIMARY KEY)");
        StatsRepositories.migrate(this.dataSource, SCHEMA);
        SessionFactory factory = HibernateStatsRepository.buildSessionFactory(this.dataSource, SCHEMA);
        HibernateStatsRepository repository = new HibernateStatsRepository(factory, Runnable::run,
                StatsMetrics.NONE, () -> {
        }, () -> {
        });

        repository.saveRound(round()).join();
        repository.close();

        assertEquals(1, count("select count(*) from " + SCHEMA + ".player_round_stats"), "the round row");
        assertEquals(1, count("select rounds_played from " + SCHEMA + ".player_year_stats"), "the year row");
    }

    @Test
    @DisplayName("The tables and Flyway's history end up in the configured schema, not in public")
    void tablesAndHistoryLiveInTheConfiguredSchema() throws SQLException {
        StatsRepositories.migrate(this.dataSource, SCHEMA);

        assertEquals(1, tablesIn(SCHEMA, "flyway_schema_history"), "history in the own schema");
        assertEquals(0, tablesIn("public", "flyway_schema_history", "player_round_stats", "player_year_stats",
                "player_names"), "nothing of ours in public");
    }

    @Test
    @DisplayName("A custom schema name is used")
    void customSchemaIsUsed() throws SQLException {
        StatsRepositories.migrate(this.dataSource, "slender_stats");

        assertEquals(3, tablesIn("slender_stats", "player_round_stats", "player_year_stats", "player_names"));
        assertEquals(0, tablesIn(SCHEMA, "player_round_stats"), "the default schema stays untouched");
    }
}
