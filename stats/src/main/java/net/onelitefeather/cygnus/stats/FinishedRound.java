package net.onelitefeather.cygnus.stats;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A finished round, ready to be persisted.
 *
 * @param roundId    the id of the round, the one the round telemetry uses
 * @param finishedAt when the round ended
 * @param year       the calendar year of {@code finishedAt} in the configured zone
 * @param players    one snapshot per participant
 * @param names      the name each participant had at the start, stored so offline players can be found by name;
 *                   a participant without an entry gets no name row
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record FinishedRound(UUID roundId, Instant finishedAt, int year, List<RoundStats> players,
                            Map<UUID, String> names) {

    /**
     * Copies the player list and the names so the round cannot change after the collector handed it over.
     */
    public FinishedRound {
        players = List.copyOf(players);
        names = Map.copyOf(names);
    }

    /**
     * Creates a round without names.
     *
     * @param roundId    the id of the round
     * @param finishedAt when the round ended
     * @param year       the calendar year of {@code finishedAt} in the configured zone
     * @param players    one snapshot per participant
     */
    public FinishedRound(UUID roundId, Instant finishedAt, int year, List<RoundStats> players) {
        this(roundId, finishedAt, year, players, Map.of());
    }
}
