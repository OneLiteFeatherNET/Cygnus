package net.onelitefeather.cygnus.stats;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A finished round, ready to be persisted.
 *
 * @param roundId    the id of the round, the one the round telemetry uses
 * @param finishedAt when the round ended
 * @param year       the calendar year of {@code finishedAt} in the configured zone
 * @param players    one snapshot per participant
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record FinishedRound(UUID roundId, Instant finishedAt, int year, List<RoundStats> players) {

    /**
     * Copies the player list so the round cannot change after the collector handed it over.
     */
    public FinishedRound {
        players = List.copyOf(players);
    }
}
