package net.onelitefeather.cygnus.stats;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the earning rules of the slender pass against H2 in PostgreSQL mode, with the real migrations {@code V1} to
 * {@code V3} and Hibernate in {@code validate} mode. Every test gets a database of its own and runs on the calling
 * thread.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class HibernateSlenderPassEarningIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final int PITY_ROUNDS = 3;
    private static final int MAX_BALANCE = 5;

    private DataSource dataSource;
    private SessionFactory sessionFactory;
    private HibernateSlenderPassRepository passes;

    @BeforeEach
    void setUp() {
        this.dataSource = freshDatabase();
        Flyway.configure().dataSource(this.dataSource).locations("classpath:db/migration").load().migrate();
        this.sessionFactory = HibernateStatsRepository.buildSessionFactory(this.dataSource);
        this.passes = new HibernateSlenderPassRepository(this.sessionFactory, Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() throws SQLException {
        this.sessionFactory.close();
        try (Connection connection = this.dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    @Test
    void theRoundThatReachesThePityThresholdCreditsOnePassAndResetsTheCounter() {
        recordSurvivorRounds(ALICE, PITY_ROUNDS - 1);

        Map<UUID, SlenderPassRepository.Adjustment> credits = this.passes.recordRounds(List.of(ALICE), List.of(),
                PITY_ROUNDS, MAX_BALANCE).join();

        assertEquals(new SlenderPassRepository.Adjustment(1, 1), credits.get(ALICE));
        assertEquals(1, this.passes.balance(ALICE).join());
        assertEquals(0, counter(ALICE), "the threshold resets the counter");
        assertEquals(LedgerReason.EARNED_PITY, ledger(ALICE).get(0).reason());
        assertEquals(1, ledger(ALICE).get(0).delta());
    }

    @Test
    void aRoundBelowTheThresholdCountsButCreditsNothing() {
        Map<UUID, SlenderPassRepository.Adjustment> credits = this.passes.recordRounds(List.of(ALICE), List.of(),
                PITY_ROUNDS, MAX_BALANCE).join();

        assertTrue(credits.isEmpty());
        assertEquals(1, counter(ALICE));
        assertTrue(ledger(ALICE).isEmpty(), "no credit, no ledger row");
    }

    @Test
    void aSlenderRoundResetsTheCounter() {
        recordSurvivorRounds(ALICE, PITY_ROUNDS - 1);

        this.passes.recordRounds(List.of(), List.of(ALICE), PITY_ROUNDS, MAX_BALANCE).join();

        assertEquals(0, counter(ALICE));
        assertTrue(this.passes.recordRounds(List.of(ALICE), List.of(), PITY_ROUNDS, MAX_BALANCE).join().isEmpty(),
                "the rounds after a Slender round start from zero again");
    }

    @Test
    void aPityCreditIsReducedToTheCap() {
        this.passes.give(ALICE, MAX_BALANCE - 1).join();
        recordSurvivorRounds(ALICE, PITY_ROUNDS - 1);

        this.passes.recordRounds(List.of(ALICE), List.of(), PITY_ROUNDS, MAX_BALANCE).join();

        assertEquals(MAX_BALANCE, this.passes.balance(ALICE).join());
        assertEquals(1, ledger(ALICE).get(1).delta());
    }

    @Test
    void aPityCreditAtTheCapWritesNoLedgerRowButStillResetsTheCounter() {
        this.passes.give(ALICE, MAX_BALANCE).join();
        recordSurvivorRounds(ALICE, PITY_ROUNDS - 1);

        Map<UUID, SlenderPassRepository.Adjustment> credits = this.passes.recordRounds(List.of(ALICE), List.of(),
                PITY_ROUNDS, MAX_BALANCE).join();

        assertTrue(credits.isEmpty());
        assertEquals(MAX_BALANCE, this.passes.balance(ALICE).join());
        assertEquals(1, ledger(ALICE).size(), "only the administrator row");
        assertEquals(0, counter(ALICE), "the cap discards the pass, it does not reopen the counter");
    }

    @Test
    void aQuotaIsGrantedOnceInAMonth() {
        YearMonth october = YearMonth.of(2026, 10);

        SlenderPassRepository.Adjustment first = this.passes.grantQuota(ALICE, october, 2, MAX_BALANCE).join();
        SlenderPassRepository.Adjustment second = this.passes.grantQuota(ALICE, october, 2, MAX_BALANCE).join();

        assertEquals(new SlenderPassRepository.Adjustment(2, 2), first);
        assertEquals(new SlenderPassRepository.Adjustment(2, 0), second);
        assertEquals(1, ledger(ALICE).size(), "the second call in the month writes no row");
        assertEquals(LedgerReason.RANK_QUOTA, ledger(ALICE).get(0).reason());
    }

    @Test
    void missedMonthsAreNotPaidBack() {
        this.passes.grantQuota(ALICE, YearMonth.of(2026, 3), 2, MAX_BALANCE).join();

        SlenderPassRepository.Adjustment result = this.passes.grantQuota(ALICE, YearMonth.of(2026, 10), 2, 10).join();

        assertEquals(new SlenderPassRepository.Adjustment(4, 2), result, "one quota for October, none for April to September");
    }

    @Test
    void aQuotaOfZeroSetsTheMonthAndWritesNothing() {
        SlenderPassRepository.Adjustment zero = this.passes.grantQuota(ALICE, YearMonth.of(2026, 10), 0, MAX_BALANCE).join();
        SlenderPassRepository.Adjustment next = this.passes.grantQuota(ALICE, YearMonth.of(2026, 10), 2, MAX_BALANCE).join();

        assertEquals(new SlenderPassRepository.Adjustment(0, 0), zero);
        assertEquals(new SlenderPassRepository.Adjustment(0, 0), next, "October is already marked, so no quota this month");
        assertTrue(ledger(ALICE).isEmpty());
    }

    @Test
    void aQuotaIsCappedAtTheMaximumBalance() {
        this.passes.give(ALICE, 4).join();

        SlenderPassRepository.Adjustment result = this.passes.grantQuota(ALICE, YearMonth.of(2026, 10), 3, MAX_BALANCE).join();

        assertEquals(new SlenderPassRepository.Adjustment(5, 1), result);
        assertEquals(1, ledger(ALICE).get(1).delta());
    }

    @Test
    void theLedgerStillRejectsUnknownReasons() throws SQLException {
        try (Connection connection = this.dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.execute("INSERT INTO slender_pass_transaction "
                    + "(player_uuid, delta, reason, created_at) VALUES ('" + ALICE + "', 1, 'BOGUS', '2026-10-26 12:00:00+00')"));
        }
    }

    @Test
    void theEarnedReasonsAreAcceptedByTheLedger() {
        this.passes.give(ALICE, 1).join();
        this.passes.recordRounds(List.of(BOB), List.of(), 1, MAX_BALANCE).join();
        this.passes.grantQuota(ALICE, YearMonth.of(2026, 10), 1, MAX_BALANCE).join();

        assertEquals(LedgerReason.RANK_QUOTA, ledger(ALICE).get(1).reason());
        assertEquals(LedgerReason.EARNED_PITY, ledger(BOB).get(0).reason());
        assertNull(ledger(BOB).get(0).roundId());
    }

    @Test
    void theV3MigrationKeepsTheLedgerRowsAndTheirIds() throws SQLException {
        DataSource fresh = freshDatabase();
        Flyway.configure().dataSource(fresh).locations("classpath:db/migration").target("2").load().migrate();
        long givenId;
        long redeemId;
        try (Connection connection = fresh.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO player_slender_pass (player_uuid, balance, version) VALUES ('" + ALICE + "', 1, 0)");
            statement.execute("INSERT INTO slender_pass_transaction (player_uuid, delta, reason, created_at) "
                    + "VALUES ('" + ALICE + "', 2, 'ADMIN_GIVE', '2026-10-01 10:00:00+00')");
            statement.execute("INSERT INTO slender_pass_transaction (player_uuid, delta, reason, round_id, created_at) "
                    + "VALUES ('" + ALICE + "', -1, 'REDEEM', '00000000-0000-0000-0000-00000000f001', '2026-10-02 10:00:00+00')");
            givenId = ledgerId(statement, "ADMIN_GIVE");
            redeemId = ledgerId(statement, "REDEEM");
        }

        Flyway.configure().dataSource(fresh).locations("classpath:db/migration").load().migrate();

        try (Connection connection = fresh.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT id, reason, delta, round_id FROM slender_pass_transaction ORDER BY id")) {
                assertTrue(rows.next());
                assertEquals(givenId, rows.getLong("id"), "the migration keeps the id of the first ledger row");
                assertEquals("ADMIN_GIVE", rows.getString("reason"));
                assertEquals(2, rows.getInt("delta"));
                assertTrue(rows.next());
                assertEquals(redeemId, rows.getLong("id"), "the migration keeps the id of the second ledger row");
                assertEquals("REDEEM", rows.getString("reason"));
                assertEquals(-1, rows.getInt("delta"));
                assertEquals("00000000-0000-0000-0000-00000000f001", rows.getString("round_id"));
                assertFalse(rows.next());
            }
            statement.execute("INSERT INTO slender_pass_transaction (player_uuid, delta, reason, created_at) "
                    + "VALUES ('" + ALICE + "', 1, 'EARNED_PITY', '2026-10-03 10:00:00+00')");
            assertTrue(ledgerId(statement, "EARNED_PITY") > redeemId, "new rows get ids after the migrated ones");
            try (ResultSet balance = statement.executeQuery("SELECT balance, rounds_since_slender FROM player_slender_pass")) {
                assertTrue(balance.next());
                assertEquals(1, balance.getInt("balance"));
                assertEquals(0, balance.getInt("rounds_since_slender"), "existing balances start with a zero counter");
            }
        } finally {
            try (Connection connection = fresh.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    private static long ledgerId(Statement statement, String reason) throws SQLException {
        try (ResultSet row = statement.executeQuery("SELECT id FROM slender_pass_transaction WHERE reason = '" + reason + "'")) {
            assertTrue(row.next(), "a ledger row with reason " + reason);
            return row.getLong("id");
        }
    }

    private void recordSurvivorRounds(UUID player, int rounds) {
        for (int round = 0; round < rounds; round++) {
            this.passes.recordRounds(List.of(player), List.of(), PITY_ROUNDS, MAX_BALANCE).join();
        }
    }

    private int counter(UUID player) {
        return this.sessionFactory.fromSession(session -> session.find(SlenderPassBalance.class, player).roundsSinceSlender());
    }

    private List<SlenderPassEntry> ledger(UUID player) {
        return this.sessionFactory.fromSession(session -> session
                .createSelectionQuery("from SlenderPassEntry e where e.playerUuid = :player order by e.id", SlenderPassEntry.class)
                .setParameter("player", player)
                .getResultList());
    }

    private static DataSource freshDatabase() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        return source;
    }
}
