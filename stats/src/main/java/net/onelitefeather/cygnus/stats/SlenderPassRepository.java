package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;

import java.time.YearMonth;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Stores the slender passes of the players: one balance per player and an append-only ledger of every change.
 * <p>
 * Every method returns at once and completes later, so the game calls it from the tick thread and applies the
 * result on a later tick. A balance is never negative, and every real change writes exactly one ledger row in
 * the same transaction as the balance change.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public interface SlenderPassRepository {

    /**
     * Tells whether passes are stored at all. A repository that is not available keeps nothing, and the game
     * hides the feature.
     *
     * @return {@code true} when passes are stored
     */
    default boolean available() {
        return true;
    }

    /**
     * Reads the balance of a player.
     *
     * @param player the player
     * @return the balance, {@code 0} for a player without a row
     */
    CompletableFuture<Integer> balance(UUID player);

    /**
     * Spends one pass for a round with one conditional update: the balance drops only while it is above zero.
     *
     * @param player  the player who plays the Slender
     * @param roundId the round the pass is spent for
     * @return {@code true} when a pass was spent, {@code false} when the balance was already zero
     */
    CompletableFuture<Boolean> redeem(UUID player, @Nullable UUID roundId);

    /**
     * Adds passes to a balance.
     *
     * @param player the player
     * @param amount the passes to add, zero or more
     * @return the new balance and the delta
     * @throws IllegalArgumentException if the amount is negative
     */
    CompletableFuture<Adjustment> give(UUID player, int amount);

    /**
     * Removes passes from a balance, never below zero. The delta is what was really removed.
     *
     * @param player the player
     * @param amount the passes to remove, zero or more
     * @return the new balance and the delta; a delta of zero writes no ledger row
     * @throws IllegalArgumentException if the amount is negative
     */
    CompletableFuture<Adjustment> take(UUID player, int amount);

    /**
     * Sets a balance to the given value. The ledger row holds the difference.
     *
     * @param player the player
     * @param amount the new balance, zero or more
     * @return the new balance and the delta; setting the current value writes no ledger row
     * @throws IllegalArgumentException if the amount is negative
     */
    CompletableFuture<Adjustment> set(UUID player, int amount);

    /**
     * Applies one finished round to the pity counters of its players, in one transaction. A Survivor's counter goes up
     * by one; when it reaches the threshold, it goes back to zero and one pass is credited, capped by the maximum
     * balance. A Slender's counter goes back to zero. A credit that the cap reduces to nothing writes no ledger row and
     * is not in the result.
     *
     * @param survivors  the players who played the round to the end as Survivors
     * @param slenders   the players who were the Slender in the round, by any path
     * @param pityRounds the Survivor rounds until one pass is earned, at least 1
     * @param maxBalance the cap for earned passes, zero or more
     * @return the players who received a pass, with their new balance and the delta
     * @throws IllegalArgumentException if the threshold is below 1 or the cap is negative
     * @since 2.21.0
     */
    CompletableFuture<Map<UUID, Adjustment>> recordRounds(Collection<UUID> survivors, Collection<UUID> slenders,
                                                          int pityRounds, int maxBalance);

    /**
     * Credits the monthly quota of a player once per calendar month. The first call in a month marks the month and
     * credits up to the quota within the cap; a later call in the same or an earlier month is not a new month, so it
     * credits nothing and writes nothing. A quota of zero marks the month as well. Missed months are not paid back.
     *
     * @param player     the player
     * @param month      the calendar month (UTC) of the join
     * @param amount     the quota of the player's rank, zero or more
     * @param maxBalance the cap for earned passes, zero or more
     * @return the new balance and the delta; a delta of zero writes no ledger row
     * @throws IllegalArgumentException if the amount or the cap is negative
     * @since 2.21.0
     */
    CompletableFuture<Adjustment> grantQuota(UUID player, YearMonth month, int amount, int maxBalance);

    /**
     * The outcome of a balance change.
     *
     * @param balance the balance after the change
     * @param delta   the change that was really written, zero when nothing changed
     */
    record Adjustment(int balance, int delta) {
    }
}
