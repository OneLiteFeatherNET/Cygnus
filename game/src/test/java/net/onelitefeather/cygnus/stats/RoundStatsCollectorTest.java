package net.onelitefeather.cygnus.stats;

import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers what the collector adds on top of the tracker: the round id, the hand-over to the repository
 * and the mapping of the game's finish reasons. No Minestom is needed, the methods take plain values.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class RoundStatsCollectorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final String ROUND_ID = "6f9619ff-8b86-d011-b42d-00c04fc964ff";

    private List<FinishedRound> rounds;
    private StatsRepository repository;

    @BeforeEach
    void setUp() {
        this.rounds = new ArrayList<>();
        this.repository = new RepositoryStub(this.rounds, false);
    }

    private RoundStatsCollector collector(StatsRepository target, String roundId) {
        return new RoundStatsCollector(target, CLOCK, ZoneOffset.UTC, () -> roundId);
    }

    @Test
    @DisplayName("The round carries the id of the round telemetry")
    void roundCarriesTheTelemetryId() {
        RoundStatsCollector collector = collector(this.repository, ROUND_ID);
        collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));
        collector.gameFinished(GameFinishEvent.Reason.TIME_OVER);

        assertEquals(1, this.rounds.size(), "the finished round must reach the repository");
        assertEquals(UUID.fromString(ROUND_ID), this.rounds.getFirst().roundId());
    }

    @Test
    @DisplayName("An id that is not a UUID is replaced by a random one so the write still works")
    void unparsableRoundIdIsReplaced() {
        RoundStatsCollector collector = collector(this.repository, "round-1");
        collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));
        collector.gameFinished(GameFinishEvent.Reason.TIME_OVER);

        assertNotNull(this.rounds.getFirst().roundId(), "a random id must stand in");
    }

    @Test
    @DisplayName("Finishing without a started round writes nothing")
    void finishWithoutRoundWritesNothing() {
        collector(this.repository, ROUND_ID).gameFinished(GameFinishEvent.Reason.TIME_OVER);

        assertTrue(this.rounds.isEmpty(), "no participants means no round");
    }

    @Test
    @DisplayName("A failing repository does not escape into the game")
    void repositoryFailureDoesNotEscape() {
        RoundStatsCollector collector = collector(new RepositoryStub(this.rounds, true), ROUND_ID);
        collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));

        assertDoesNotThrow(() -> collector.gameFinished(GameFinishEvent.Reason.TIME_OVER),
                "a statistics failure must never reach the game");
    }

    @Test
    @DisplayName("Every finish reason of the game has a counterpart in the statistics")
    void everyGameReasonIsMapped() {
        for (GameFinishEvent.Reason reason : GameFinishEvent.Reason.values()) {
            RoundStatsCollector collector = collector(this.repository, ROUND_ID);
            collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));
            collector.gameFinished(reason);

            assertEquals(reason.name(), this.rounds.getLast().players().getFirst().finishReason().name(),
                    "the reason " + reason + " must arrive under the same name");
        }
    }

    /** Keeps the rounds it was given, or throws on every write. */
    private record RepositoryStub(List<FinishedRound> rounds, boolean failing) implements StatsRepository {

        @Override
        public void saveRound(FinishedRound round) {
            if (this.failing) {
                throw new IllegalStateException("database is gone");
            }
            this.rounds.add(round);
        }

        @Override
        public int deleteByPlayer(UUID playerUuid) {
            return 0;
        }

        @Override
        public void close() {
        }
    }
}
