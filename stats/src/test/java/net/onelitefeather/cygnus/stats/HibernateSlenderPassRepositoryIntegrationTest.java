package net.onelitefeather.cygnus.stats;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the slender pass repository against H2 in PostgreSQL mode with the real Flyway migrations and Hibernate in
 * {@code validate} mode. Every test gets a database of its own and runs on the calling thread.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class HibernateSlenderPassRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ROUND = UUID.fromString("00000000-0000-0000-0000-00000000f001");

    private DataSource dataSource;
    private SessionFactory sessionFactory;
    private HibernateStatsRepository statistics;
    private HibernateSlenderPassRepository passes;

    @BeforeEach
    void setUp() {
        this.dataSource = freshDatabase();
        Flyway.configure().dataSource(this.dataSource).locations("classpath:db/migration").load().migrate();
        this.sessionFactory = HibernateStatsRepository.buildSessionFactory(this.dataSource);
        this.statistics = new HibernateStatsRepository(this.sessionFactory, Runnable::run, StatsMetrics.NONE, () -> {
        });
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
    void giveWritesTheBalanceAndOneLedgerRow() {
        SlenderPassRepository.Adjustment result = this.passes.give(ALICE, 2).join();

        assertEquals(2, result.balance());
        assertEquals(2, this.passes.balance(ALICE).join());
        List<SlenderPassEntry> ledger = ledger();
        assertEquals(1, ledger.size());
        assertEquals(2, ledger.get(0).delta());
        assertEquals(LedgerReason.ADMIN_GIVE, ledger.get(0).reason());
        assertEquals(NOW, ledger.get(0).createdAt(), "the ledger time comes from the injected clock");
    }

    @Test
    void redeemSpendsAPassOnlyWhileTheBalanceIsAboveZero() {
        this.passes.give(ALICE, 1).join();

        assertTrue(this.passes.redeem(ALICE, ROUND).join());
        assertFalse(this.passes.redeem(ALICE, ROUND).join(), "the second redemption finds no pass");

        assertEquals(0, this.passes.balance(ALICE).join(), "the balance never goes below zero");
        assertEquals(2, ledger().size(), "only the successful redemption writes a row");
    }

    @Test
    void redeemWritesARedeemRowWithTheRound() {
        this.passes.give(ALICE, 1).join();

        this.passes.redeem(ALICE, ROUND).join();

        SlenderPassEntry row = ledger().get(1);
        assertEquals(LedgerReason.REDEEM, row.reason());
        assertEquals(-1, row.delta());
        assertEquals(ROUND, row.roundId());
    }

    @Test
    void redeemWithoutAnyRowSpendsNothing() {
        assertFalse(this.passes.redeem(ALICE, ROUND).join());
        assertTrue(ledger().isEmpty());
    }

    @Test
    void takeIsClampedAndTheLedgerHoldsTheActualDelta() {
        this.passes.give(ALICE, 1).join();

        SlenderPassRepository.Adjustment result = this.passes.take(ALICE, 3).join();

        assertEquals(0, result.balance());
        assertEquals(-1, ledger().get(1).delta());
        assertEquals(LedgerReason.ADMIN_TAKE, ledger().get(1).reason());
    }

    @Test
    void setWritesTheDifference() {
        this.passes.give(ALICE, 1).join();

        this.passes.set(ALICE, 4).join();

        assertEquals(4, this.passes.balance(ALICE).join());
        assertEquals(3, ledger().get(1).delta());
        assertEquals(LedgerReason.ADMIN_SET, ledger().get(1).reason());
    }

    @Test
    void deletingAPlayerRemovesTheBalanceAndTheLedger() {
        this.passes.give(ALICE, 2).join();
        this.passes.redeem(ALICE, ROUND).join();

        int removed = this.statistics.deleteByPlayer(ALICE);

        assertEquals(0, this.passes.balance(ALICE).join());
        assertTrue(ledger().isEmpty(), "the GDPR deletion must remove every ledger row of the player");
        assertEquals(3, removed, "one balance row and two ledger rows");
    }

    private List<SlenderPassEntry> ledger() {
        return this.sessionFactory.fromSession(session -> session
                .createSelectionQuery("from SlenderPassEntry e where e.playerUuid = :player order by e.id", SlenderPassEntry.class)
                .setParameter("player", ALICE)
                .getResultList());
    }

    private static DataSource freshDatabase() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        return source;
    }
}
