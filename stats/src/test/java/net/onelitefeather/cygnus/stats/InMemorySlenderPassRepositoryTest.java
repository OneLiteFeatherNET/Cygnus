package net.onelitefeather.cygnus.stats;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the rules of the in-memory pass repository: the balance never goes negative and every real change
 * writes exactly one ledger row with the actual delta.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class InMemorySlenderPassRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final UUID ROUND = UUID.fromString("00000000-0000-0000-0000-00000000f001");

    private static InMemorySlenderPassRepository repository() {
        return new InMemorySlenderPassRepository(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void giveWritesOneLedgerRowWithTheGivenDelta() {
        InMemorySlenderPassRepository repository = repository();
        UUID player = UUID.randomUUID();
        repository.give(player, 1).join();

        SlenderPassRepository.Adjustment result = repository.give(player, 2).join();

        assertEquals(3, result.balance(), "give adds to the existing balance");
        assertEquals(2, result.delta());
        List<InMemorySlenderPassRepository.LedgerEntry> ledger = repository.ledger();
        assertEquals(2, ledger.size());
        assertEquals(2, ledger.get(1).delta());
        assertEquals(LedgerReason.ADMIN_GIVE, ledger.get(1).reason());
    }

    @Test
    void ledgerRowCarriesTheTimeFromTheInjectedClock() {
        InMemorySlenderPassRepository repository = repository();

        repository.give(UUID.randomUUID(), 1).join();

        assertEquals(NOW, repository.ledger().get(0).createdAt());
    }

    @Test
    void takeIsClampedAtZeroAndWritesTheActualDelta() {
        InMemorySlenderPassRepository repository = repository();
        UUID player = UUID.randomUUID();
        repository.give(player, 1).join();

        SlenderPassRepository.Adjustment result = repository.take(player, 3).join();

        assertEquals(0, result.balance(), "take must never go below zero");
        assertEquals(-1, result.delta(), "the ledger must record what was really removed");
        assertEquals(-1, repository.ledger().get(1).delta());
        assertEquals(LedgerReason.ADMIN_TAKE, repository.ledger().get(1).reason());
    }

    @Test
    void takeFromEmptyBalanceWritesNoLedgerRow() {
        InMemorySlenderPassRepository repository = repository();
        UUID player = UUID.randomUUID();

        SlenderPassRepository.Adjustment result = repository.take(player, 3).join();

        assertEquals(0, result.balance());
        assertEquals(0, result.delta());
        assertTrue(repository.ledger().isEmpty(), "nothing was removed, so nothing is recorded");
    }

    @Test
    void setWritesTheDifferenceAsDelta() {
        InMemorySlenderPassRepository repository = repository();
        UUID player = UUID.randomUUID();
        repository.give(player, 1).join();

        SlenderPassRepository.Adjustment result = repository.set(player, 4).join();

        assertEquals(4, result.balance());
        assertEquals(3, result.delta());
        assertEquals(LedgerReason.ADMIN_SET, repository.ledger().get(1).reason());
        assertEquals(3, repository.ledger().get(1).delta());
    }

    @Test
    void setToTheCurrentValueWritesNoLedgerRow() {
        InMemorySlenderPassRepository repository = repository();
        UUID player = UUID.randomUUID();
        repository.give(player, 2).join();

        repository.set(player, 2).join();

        assertEquals(1, repository.ledger().size(), "only the give is recorded");
    }

    @Test
    void redeemSpendsOnePassAndWritesARedeemRowWithTheRound() {
        InMemorySlenderPassRepository repository = repository();
        UUID player = UUID.randomUUID();
        repository.give(player, 2).join();

        boolean spent = repository.redeem(player, ROUND).join();

        assertTrue(spent);
        assertEquals(1, repository.balance(player).join());
        InMemorySlenderPassRepository.LedgerEntry row = repository.ledger().get(1);
        assertEquals(-1, row.delta());
        assertEquals(LedgerReason.REDEEM, row.reason());
        assertEquals(ROUND, row.roundId());
    }

    @Test
    void redeemOnZeroBalanceSpendsNothing() {
        InMemorySlenderPassRepository repository = repository();
        UUID player = UUID.randomUUID();

        boolean spent = repository.redeem(player, ROUND).join();

        assertFalse(spent);
        assertEquals(0, repository.balance(player).join(), "the balance must stay at zero");
        assertTrue(repository.ledger().isEmpty());
    }

    @Test
    void giveRejectsNegativeAmounts() {
        InMemorySlenderPassRepository repository = repository();

        assertThrows(IllegalArgumentException.class, () -> repository.give(UUID.randomUUID(), -1));
    }

    @Test
    void setRejectsNegativeAmounts() {
        InMemorySlenderPassRepository repository = repository();

        assertThrows(IllegalArgumentException.class, () -> repository.set(UUID.randomUUID(), -1));
    }
}
