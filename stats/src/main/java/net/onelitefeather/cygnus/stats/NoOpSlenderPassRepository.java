package net.onelitefeather.cygnus.stats;

import java.time.YearMonth;
import java.util.Collection;
import java.util.Map;
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

    /**
     * Keeps no counter and credits nothing.
     *
     * @param survivors  the Survivors
     * @param slenders   the Slender
     * @param pityRounds the threshold
     * @param maxBalance the cap
     * @return an already completed, empty map
     * @since 2.21.0
     */
    @Override
    public CompletableFuture<Map<UUID, Adjustment>> recordRounds(Collection<UUID> survivors, Collection<UUID> slenders,
                                                                 int pityRounds, int maxBalance) {
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * Credits nothing.
     *
     * @param player     the player
     * @param month      the month
     * @param amount     the quota
     * @param maxBalance the cap
     * @return an already completed, empty adjustment
     * @since 2.21.0
     */
    @Override
    public CompletableFuture<Adjustment> grantQuota(UUID player, YearMonth month, int amount, int maxBalance) {
        return CompletableFuture.completedFuture(new Adjustment(0, 0));
    }
}
