package net.onelitefeather.cygnus.stats;

import java.util.UUID;

/**
 * Where finished rounds are stored.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public interface StatsRepository extends AutoCloseable {

    /**
     * Stores a finished round. Implementations do the I/O off the calling thread and never throw: a
     * failed write is logged and counted, it must not touch the round end.
     *
     * @param round the finished round
     */
    void saveRound(FinishedRound round);

    /**
     * Deletes every round record and yearly aggregate of a player in one transaction. Blocks until
     * done, so call it off the tick thread.
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
