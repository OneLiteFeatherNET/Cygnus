package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
