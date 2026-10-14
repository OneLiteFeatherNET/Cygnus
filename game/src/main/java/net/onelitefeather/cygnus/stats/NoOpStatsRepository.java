package net.onelitefeather.cygnus.stats;

import java.util.UUID;

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
     * Drops the round.
     *
     * @param round the finished round
     */
    @Override
    public void saveRound(FinishedRound round) {
        // statistics are off
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
