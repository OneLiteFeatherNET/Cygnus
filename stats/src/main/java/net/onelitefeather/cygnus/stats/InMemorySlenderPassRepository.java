package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps the balances and the ledger in memory, with the same rules as the database: no negative balance, one
 * ledger row per real change, the time from the injected clock. Used by the tests of the game.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class InMemorySlenderPassRepository implements SlenderPassRepository {

    private final Clock clock;
    private final Map<UUID, Integer> balances = new HashMap<>();
    private final Map<UUID, Integer> roundsSinceSlender = new HashMap<>();
    private final Map<UUID, YearMonth> quotaMonths = new HashMap<>();
    private final List<LedgerEntry> ledger = new ArrayList<>();

    /**
     * Creates an empty repository.
     *
     * @param clock the time of every ledger row
     */
    public InMemorySlenderPassRepository(Clock clock) {
        this.clock = clock;
    }

    /**
     * Reads the balance.
     *
     * @param player the player
     * @return the balance, {@code 0} for an unknown player
     */
    @Override
    public synchronized CompletableFuture<Integer> balance(UUID player) {
        return CompletableFuture.completedFuture(balanceOf(player));
    }

    /**
     * Spends one pass when the balance is above zero.
     *
     * @param player  the player
     * @param roundId the round
     * @return whether a pass was spent
     */
    @Override
    public synchronized CompletableFuture<Boolean> redeem(UUID player, @Nullable UUID roundId) {
        if (balanceOf(player) <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        record(player, -1, LedgerReason.REDEEM, roundId);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Adds passes.
     *
     * @param player the player
     * @param amount the passes, zero or more
     * @return the adjustment
     */
    @Override
    public synchronized CompletableFuture<Adjustment> give(UUID player, int amount) {
        requireNonNegative(amount);
        return CompletableFuture.completedFuture(record(player, amount, LedgerReason.ADMIN_GIVE, null));
    }

    /**
     * Removes passes, clamped at zero. Nothing removed writes no row.
     *
     * @param player the player
     * @param amount the passes, zero or more
     * @return the adjustment
     */
    @Override
    public synchronized CompletableFuture<Adjustment> take(UUID player, int amount) {
        requireNonNegative(amount);
        int current = balanceOf(player);
        int removed = Math.min(amount, current);
        if (removed == 0) {
            return CompletableFuture.completedFuture(new Adjustment(current, 0));
        }
        return CompletableFuture.completedFuture(record(player, -removed, LedgerReason.ADMIN_TAKE, null));
    }

    /**
     * Sets the balance. Setting the current value writes no row.
     *
     * @param player the player
     * @param amount the new balance, zero or more
     * @return the adjustment
     */
    @Override
    public synchronized CompletableFuture<Adjustment> set(UUID player, int amount) {
        requireNonNegative(amount);
        int delta = amount - balanceOf(player);
        if (delta == 0) {
            return CompletableFuture.completedFuture(new Adjustment(amount, 0));
        }
        return CompletableFuture.completedFuture(record(player, delta, LedgerReason.ADMIN_SET, null));
    }

    /**
     * Returns every ledger row in the order they were written.
     *
     * @return a copy of the ledger
     */
    public synchronized List<LedgerEntry> ledger() {
        return List.copyOf(this.ledger);
    }

    /**
     * Applies one finished round to the pity counters, with the rules of the database.
     *
     * @param survivors  the Survivors of the round
     * @param slenders   the Slender of the round
     * @param pityRounds the threshold, at least 1
     * @param maxBalance the cap, zero or more
     * @return the credits of the players who received a pass
     */
    @Override
    public synchronized CompletableFuture<Map<UUID, Adjustment>> recordRounds(Collection<UUID> survivors,
                                                                               Collection<UUID> slenders,
                                                                               int pityRounds, int maxBalance) {
        if (pityRounds < 1) {
            throw new IllegalArgumentException("The pity threshold must be at least 1: " + pityRounds);
        }
        requireNonNegative(maxBalance);
        slenders.forEach(this.roundsSinceSlender::remove);
        Map<UUID, Adjustment> credits = new HashMap<>();
        for (UUID player : survivors) {
            int counted = this.roundsSinceSlender.getOrDefault(player, 0) + 1;
            if (counted < pityRounds) {
                this.roundsSinceSlender.put(player, counted);
                continue;
            }
            this.roundsSinceSlender.remove(player);
            if (Math.min(1, Math.max(0, maxBalance - balanceOf(player))) > 0) {
                credits.put(player, record(player, 1, LedgerReason.EARNED_PITY, null));
            }
        }
        return CompletableFuture.completedFuture(Map.copyOf(credits));
    }

    /**
     * Credits the monthly quota once per month, with the rules of the database.
     *
     * @param player     the player
     * @param month      the month of the join
     * @param amount     the quota, zero or more
     * @param maxBalance the cap, zero or more
     * @return the adjustment
     */
    @Override
    public synchronized CompletableFuture<Adjustment> grantQuota(UUID player, YearMonth month, int amount, int maxBalance) {
        requireNonNegative(amount);
        requireNonNegative(maxBalance);
        YearMonth last = this.quotaMonths.get(player);
        if (last != null && !last.isBefore(month)) {
            return CompletableFuture.completedFuture(new Adjustment(balanceOf(player), 0));
        }
        this.quotaMonths.put(player, month);
        int delta = Math.min(amount, Math.max(0, maxBalance - balanceOf(player)));
        if (delta == 0) {
            return CompletableFuture.completedFuture(new Adjustment(balanceOf(player), 0));
        }
        return CompletableFuture.completedFuture(record(player, delta, LedgerReason.RANK_QUOTA, null));
    }

    private int balanceOf(UUID player) {
        return this.balances.getOrDefault(player, 0);
    }

    private Adjustment record(UUID player, int delta, LedgerReason reason, @Nullable UUID roundId) {
        int balance = balanceOf(player) + delta;
        this.balances.put(player, balance);
        this.ledger.add(new LedgerEntry(player, delta, reason, roundId, this.clock.instant()));
        return new Adjustment(balance, delta);
    }

    private static void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("A pass amount cannot be negative: " + amount);
        }
    }

    /**
     * One row of the ledger.
     *
     * @param player    the player
     * @param delta     the change that was written
     * @param reason    why it changed
     * @param roundId   the round for a {@link LedgerReason#REDEEM}, otherwise {@code null}
     * @param createdAt when the row was written
     */
    public record LedgerEntry(UUID player, int delta, LedgerReason reason, @Nullable UUID roundId, Instant createdAt) {
    }
}
