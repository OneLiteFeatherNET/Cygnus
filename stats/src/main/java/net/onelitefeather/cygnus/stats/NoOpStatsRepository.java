package net.onelitefeather.cygnus.stats;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The repository used when no {@code stats.jdbc-url} is configured: it keeps nothing and opens no
 * connection.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class NoOpStatsRepository implements StatsRepository {

    /**
     * Reports that nothing is stored.
     *
     * @return always {@code false}
     */
    @Override
    public boolean enabled() {
        return false;
    }

    /**
     * Drops the round.
     *
     * @param round the finished round
     * @return an already completed future without any year row
     */
    @Override
    public CompletableFuture<Map<UUID, YearStatsView>> saveRound(FinishedRound round) {
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * Finds nothing.
     *
     * @param playerUuid the player
     * @param year       the calendar year
     * @return an already completed, empty result
     */
    @Override
    public CompletableFuture<Optional<YearStatsView>> findYear(UUID playerUuid, int year) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Knows nobody.
     *
     * @param name the name to look for
     * @return an already completed, empty result
     */
    @Override
    public CompletableFuture<Optional<KnownPlayer>> findPlayerByName(String name) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Has no years.
     *
     * @param playerUuid the player
     * @return an already completed, empty list
     */
    @Override
    public CompletableFuture<List<Integer>> availableYears(UUID playerUuid) {
        return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Deletes nothing.
     *
     * @param playerUuid the player
     * @return always {@code 0}
     */
    @Override
    public int deleteByPlayer(UUID playerUuid) {
        return 0;
    }

    /**
     * Releases nothing.
     */
    @Override
    public void close() {
        // nothing was opened
    }
}
