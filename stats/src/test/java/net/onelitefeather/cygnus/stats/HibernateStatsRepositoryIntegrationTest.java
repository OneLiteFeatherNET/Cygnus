package net.onelitefeather.cygnus.stats;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import net.onelitefeather.cygnus.stats.RoundStats.Outcome;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import org.flywaydb.core.Flyway;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the repository against H2 in PostgreSQL mode with the real Flyway migration and Hibernate in
 * {@code validate} mode. Every test gets a database of its own and writes on the calling thread, so
 * nothing waits and no test sees another one's rows.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class HibernateStatsRepositoryIntegrationTest {

    private static final Instant FINISHED = Instant.parse("2026-06-01T12:15:00Z");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID SLENDER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private DataSource dataSource;
    private SessionFactory sessionFactory;
    private InMemoryMetricReader reader;
    private SdkMeterProvider meters;
    private HibernateStatsRepository repository;
    private AtomicInteger attempts;

    @BeforeEach
    void setUp() {
        this.dataSource = freshDatabase();
        migrate(this.dataSource);
        this.sessionFactory = HibernateStatsRepository.buildSessionFactory(this.dataSource);
        this.reader = InMemoryMetricReader.create();
        this.meters = SdkMeterProvider.builder().registerMetricReader(this.reader).build();
        this.attempts = new AtomicInteger();
        this.repository = repository(Runnable::run, () -> this.attempts.incrementAndGet());
    }

    @AfterEach
    void tearDown() throws SQLException {
        this.repository.close();
        this.meters.close();
        try (Connection connection = this.dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    private HibernateStatsRepository repository(java.util.concurrent.Executor executor, Runnable afterYearRowsLoaded) {
        return new HibernateStatsRepository(this.sessionFactory, executor,
                StatsMetrics.of(this.meters.get("test")), () -> {
        }, afterYearRowsLoaded);
    }

    private static DataSource freshDatabase() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        return source;
    }

    private static void migrate(DataSource source) {
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    }

    private static RoundStats survivor(UUID player, Outcome outcome, FinishReason reason, int pages, boolean died, long timeSurvived) {
        return new RoundStats(player, Role.SURVIVOR, outcome, reason, pages, 0, died, !died && outcome != Outcome.ABANDONED,
                false, 600L, timeSurvived, 0.0D, 0, 0);
    }

    private static RoundStats slender(UUID player, Outcome outcome, FinishReason reason, int kills, long playTime) {
        return new RoundStats(player, Role.SLENDER, outcome, reason, 0, kills, false, false, false, playTime, 0L, 0.0D, 0, 3);
    }

    private static FinishedRound round(UUID roundId, int year, RoundStats... players) {
        return new FinishedRound(roundId, FINISHED, year, List.of(players));
    }

    private long number(String sql, Object... parameters) {
        try (Connection connection = this.dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "no row for " + sql);
                return result.getLong(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    private long yearValue(String column, UUID player, int year) {
        return number("select " + column + " from player_year_stats where player_uuid = ? and stats_year = ?", player, year);
    }

    private long failures() {
        return this.reader.collectAllMetrics().stream()
                .filter(metric -> metric.getName().equals(StatsMetrics.WRITE_FAILURES))
                .flatMap(metric -> metric.getLongSumData().getPoints().stream())
                .mapToLong(LongPointData::getValue)
                .sum();
    }

    /** Plays the other server: bumps the Slender's year row behind the repository's back. */
    private void otherServerAddsKills(int kills) {
        try (Connection connection = this.dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("update player_year_stats set kills = kills + " + kills + ", version = version + 1, rounds_played = rounds_played + 1"
                    + " where player_uuid = '" + SLENDER + "'");
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    private void otherServerCreatesYearRow(int year, int kills) {
        try (Connection connection = this.dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into player_year_stats values ('" + SLENDER + "', " + year + ", 1, 1, 0, 0, 0, 1, 0, 0, 600,"
                    + " 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0, " + kills + ", " + kills + ", null, 0, 0)");
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    @DisplayName("A finished round writes one row per participant under the same round id")
    void roundRowsAreWritten() {
        UUID roundId = UUID.randomUUID();
        this.repository.saveRound(round(roundId, 2026,
                slender(SLENDER, Outcome.LOSS, FinishReason.TIME_OVER, 0, 900),
                survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 2, false, 900),
                survivor(BOB, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900)));

        assertEquals(3, number("select count(*) from player_round_stats where round_id = ?", roundId));
        assertEquals(2, number("select pages_found from player_round_stats where round_id = ? and player_uuid = ?", roundId, ALICE));
    }

    @Test
    @DisplayName("The first round of a year creates the aggregate with one round played")
    void firstRoundCreatesTheAggregate() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 2, false, 900)));

        assertEquals(1, yearValue("rounds_played", ALICE, 2026));
        assertEquals(1, yearValue("rounds_survivor", ALICE, 2026));
        assertEquals(2, yearValue("pages_found", ALICE, 2026));
    }

    @Test
    @DisplayName("Further rounds add up and the best round keeps the maximum")
    void furtherRoundsAddUp() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 3, 500)));
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 2, 400)));

        assertEquals(5, yearValue("kills", SLENDER, 2026));
        assertEquals(3, yearValue("best_kills_round", SLENDER, 2026), "the best round must not drop to the later, smaller one");
        assertEquals(2, yearValue("rounds_played", SLENDER, 2026));
    }

    @Test
    @DisplayName("The fastest win keeps the minimum and ignores a slower later win")
    void fastestWinKeepsTheMinimum() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 1, 500)));
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.SURVIVOR_LEFT, 1, 700)));
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 1, 300)));

        assertEquals(300, yearValue("fastest_win_seconds", SLENDER, 2026));
    }

    @Test
    @DisplayName("A loss and an abandoned round never set the fastest win")
    void lossDoesNotSetTheFastestWin() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.LOSS, FinishReason.TIME_OVER, 0, 900)));

        assertEquals(1, number("select count(*) from player_year_stats where player_uuid = ? and fastest_win_seconds is null", SLENDER),
                "fastest_win_seconds stays null without a win");
    }

    @Test
    @DisplayName("Wins, losses, abandoned rounds and wins by reason are counted per role")
    void outcomesAreCounted() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.WIN, FinishReason.ALL_PAGES_FOUND, 3, false, 900)));
        this.repository.saveRound(round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.LOSS, FinishReason.ALL_SURVIVOR_DEAD, 1, true, 300)));
        this.repository.saveRound(round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.ABANDONED, FinishReason.TIME_OVER, 0, false, 120)));

        assertEquals(1, yearValue("wins_survivor", ALICE, 2026));
        assertEquals(1, yearValue("losses_survivor", ALICE, 2026));
        assertEquals(1, yearValue("abandoned", ALICE, 2026));
        assertEquals(1, yearValue("wins_all_pages_found", ALICE, 2026));
        assertEquals(0, yearValue("wins_time_over", ALICE, 2026), "an abandoned round is no win by time over");
        assertEquals(1, yearValue("all_pages_contributions", ALICE, 2026));
        assertEquals(1, yearValue("deaths", ALICE, 2026));
        assertEquals(1, yearValue("rounds_survived", ALICE, 2026));
        assertEquals(900, yearValue("longest_survived_seconds", ALICE, 2026));
    }

    @Test
    @DisplayName("Each year has its own aggregate")
    void yearsAreSeparate() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900)));
        this.repository.saveRound(round(UUID.randomUUID(), 2027, survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900)));

        assertEquals(1, yearValue("rounds_played", ALICE, 2026));
        assertEquals(1, yearValue("rounds_played", ALICE, 2027));
    }

    @Test
    @DisplayName("A lost optimistic-lock race is retried and both rounds end up in the aggregate")
    void optimisticLockRaceIsRetried() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 2, 500)));
        AtomicInteger calls = new AtomicInteger();
        HibernateStatsRepository racing = repository(Runnable::run, () -> {
            if (calls.incrementAndGet() == 1) {
                otherServerAddsKills(10);
            }
        });

        racing.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 3, 500)));

        assertEquals(2, calls.get(), "the first attempt lost the race and the second one won it");
        assertEquals(15, yearValue("kills", SLENDER, 2026), "2 earlier + 10 from the other server + 3 of this round");
        assertEquals(3, yearValue("rounds_played", SLENDER, 2026));
        assertEquals(0, failures(), "a retried write is no failure");
    }

    @Test
    @DisplayName("A year row another server created first is merged on the retry")
    void yearRowCreatedByAnotherServerIsMerged() {
        AtomicInteger calls = new AtomicInteger();
        HibernateStatsRepository racing = repository(Runnable::run, () -> {
            if (calls.incrementAndGet() == 1) {
                otherServerCreatesYearRow(2026, 4);
            }
        });

        racing.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 3, 500)));

        assertEquals(1, number("select count(*) from player_year_stats where player_uuid = ?", SLENDER), "exactly one aggregate must exist");
        assertEquals(7, yearValue("kills", SLENDER, 2026), "4 from the other server + 3 of this round");
        assertEquals(1, number("select count(*) from player_round_stats where player_uuid = ?", SLENDER),
                "the rolled-back first attempt must not leave a round row behind");
        assertEquals(0, failures());
    }

    @Test
    @DisplayName("A write that keeps losing the race is counted as failed after three attempts and does not throw")
    void exhaustedRetriesAreCounted() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 2, 500)));
        AtomicInteger calls = new AtomicInteger();
        HibernateStatsRepository racing = repository(Runnable::run, () -> {
            calls.incrementAndGet();
            otherServerAddsKills(1);
        });

        racing.saveRound(round(UUID.randomUUID(), 2026, slender(SLENDER, Outcome.WIN, FinishReason.ALL_SURVIVOR_DEAD, 3, 500)));

        assertEquals(HibernateStatsRepository.MAX_ATTEMPTS, calls.get());
        assertEquals(1, failures());
    }

    @Test
    @DisplayName("Saving the same round twice fails the second time and does not count it twice")
    void duplicateRoundIsRejected() {
        FinishedRound round = round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900));
        this.repository.saveRound(round);

        this.repository.saveRound(round);

        assertEquals(1, yearValue("rounds_played", ALICE, 2026), "the duplicate must roll back completely");
        assertEquals(1, failures());
    }

    @Test
    @DisplayName("A closed database is a counted failure, not an exception in the game")
    void unavailableDatabaseIsCounted() {
        this.sessionFactory.close();

        this.repository.saveRound(round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900)));

        assertEquals(1, failures());
    }

    @Test
    @DisplayName("The write is queued on the executor and the caller returns before it ran")
    void writeRunsOnTheExecutor() {
        Queue<Runnable> queue = new ArrayDeque<>();
        HibernateStatsRepository queued = repository(queue::add, () -> {
        });

        queued.saveRound(round(UUID.randomUUID(), 2026, survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900)));

        assertEquals(1, queue.size(), "the write must be queued, not run by the caller");
        assertEquals(0, number("select count(*) from player_round_stats"), "nothing is written before the executor runs");
        queue.remove().run();
        assertEquals(1, number("select count(*) from player_round_stats"));
    }

    @Test
    @DisplayName("Deleting a player removes their round rows and year rows and nobody else's")
    void deleteByPlayerRemovesOnlyThatPlayer() {
        this.repository.saveRound(round(UUID.randomUUID(), 2026,
                survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900),
                survivor(BOB, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900)));
        this.repository.saveRound(round(UUID.randomUUID(), 2027, survivor(ALICE, Outcome.WIN, FinishReason.TIME_OVER, 1, false, 900)));

        int removed = this.repository.deleteByPlayer(ALICE);

        assertEquals(4, removed, "two round rows and two year rows");
        assertEquals(0, number("select count(*) from player_round_stats where player_uuid = ?", ALICE));
        assertEquals(0, number("select count(*) from player_year_stats where player_uuid = ?", ALICE));
        assertEquals(1, number("select count(*) from player_round_stats where player_uuid = ?", BOB));
        assertEquals(1, number("select count(*) from player_year_stats where player_uuid = ?", BOB));
    }

    @Test
    @DisplayName("Hibernate refuses to start against a database without the migration")
    void missingMigrationFailsValidation() {
        DataSource empty = freshDatabase();

        assertThrows(RuntimeException.class, () -> HibernateStatsRepository.buildSessionFactory(empty).close(),
                "hbm2ddl.auto=validate must fail at startup, not at the first write");
    }

    @Test
    @DisplayName("The migration creates exactly the two statistics tables")
    void migrationCreatesTheTables() {
        List<String> tables = new ArrayList<>();
        try (Connection connection = this.dataSource.getConnection();
             ResultSet result = connection.createStatement().executeQuery(
                     "select table_name from information_schema.tables where table_name like 'PLAYER_%'")) {
            while (result.next()) {
                tables.add(result.getString(1));
            }
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }

        assertEquals(List.of("PLAYER_ROUND_STATS", "PLAYER_YEAR_STATS"), tables.stream().sorted().toList());
    }
}
