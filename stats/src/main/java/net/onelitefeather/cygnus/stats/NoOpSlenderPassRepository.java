package net.onelitefeather.cygnus.stats;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The repository used while no statistics database is configured: it keeps nothing and reports the feature as
 * unavailable.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class NoOpSlenderPassRepository implements SlenderPassRepository {

    /**
     * Reports that passes are not stored.
     *
     * @return always {@code false}
     */
    @Override
    public boolean available() {
        return false;
    }

    /**
     * Knows no balance.
     *
     * @param player the player
     * @return an already completed {@code 0}
     */
    @Override
    public CompletableFuture<Integer> balance(UUID player) {
        return CompletableFuture.completedFuture(0);
    }

    /**
     * Spends nothing.
     *
     * @param player  the player
     * @param roundId the round
     * @return an already completed {@code false}
     */
    @Override
    public CompletableFuture<Boolean> redeem(UUID player, UUID roundId) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Stores nothing.
     *
     * @param player the player
     * @param amount the passes
     * @return an already completed, empty adjustment
     */
    @Override
    public CompletableFuture<Adjustment> give(UUID player, int amount) {
        return CompletableFuture.completedFuture(new Adjustment(0, 0));
    }

    /**
     * Stores nothing.
     *
     * @param player the player
     * @param amount the passes
     * @return an already completed, empty adjustment
     */
    @Override
    public CompletableFuture<Adjustment> take(UUID player, int amount) {
        return CompletableFuture.completedFuture(new Adjustment(0, 0));
    }

    /**
     * Stores nothing.
     *
     * @param player the player
     * @param amount the balance
     * @return an already completed, empty adjustment
     */
    @Override
    public CompletableFuture<Adjustment> set(UUID player, int amount) {
        return CompletableFuture.completedFuture(new Adjustment(0, 0));
    }
}
