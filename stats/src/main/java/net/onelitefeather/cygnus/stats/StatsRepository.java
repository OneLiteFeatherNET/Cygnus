package net.onelitefeather.cygnus.stats;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Where finished rounds are stored and read back from.
 * <p>
 * Every method that returns a {@link CompletableFuture} does its I/O on the repository's executor and
 * never on the calling thread; the future completes on that executor, so a caller that needs the tick
 * thread has to hop back itself.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public interface StatsRepository extends AutoCloseable {

    /**
     * Tells whether statistics are stored at all.
     *
     * @return {@code false} for the repository that keeps nothing
     */
    default boolean enabled() {
        return true;
    }

    /**
     * Stores a finished round. Implementations do the I/O off the calling thread and never throw: a
     * failed write is logged and counted, it must not touch the round end.
     *
     * @param round the finished round
     * @return completes after the commit with the merged year row of every participant, or exceptionally
     * when the write failed
     */
    CompletableFuture<Map<UUID, YearStatsView>> saveRound(FinishedRound round);

    /**
     * Reads one year of a player.
     *
     * @param playerUuid the player
     * @param year       the calendar year
     * @return the year, empty when the player has no rounds in it
     */
    CompletableFuture<Optional<YearStatsView>> findYear(UUID playerUuid, int year);

    /**
     * Finds a player by the last name they were seen with, ignoring case. When several players have
     * carried the name, the one whose name was seen last wins.
     *
     * @param name the name to look for
     * @return the player, empty when no player was seen with that name
     */
    CompletableFuture<Optional<KnownPlayer>> findPlayerByName(String name);

    /**
     * Lists the years a player has statistics for.
     *
     * @param playerUuid the player
     * @return the years, newest first, empty for a player without rounds
     */
    CompletableFuture<List<Integer>> availableYears(UUID playerUuid);

    /**
     * Deletes every round record, yearly aggregate and the stored name of a player in one transaction.
     * Blocks until done, so call it off the tick thread.
     *
     * @param playerUuid the player
     * @return how many rows were removed in total
     */
    int deleteByPlayer(UUID playerUuid);

    /**
     * Writes what is still queued and releases the connections.
     */
    @Override
    void close();
}
