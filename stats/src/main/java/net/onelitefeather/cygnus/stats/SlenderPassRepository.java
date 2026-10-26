package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;

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
     * The outcome of a balance change.
     *
     * @param balance the balance after the change
     * @param delta   the change that was really written, zero when nothing changed
     */
    record Adjustment(int balance, int delta) {
    }
}
