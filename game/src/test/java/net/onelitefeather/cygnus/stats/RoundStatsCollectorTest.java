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

    private FakeStatsRepository repository;
    private List<FinishedRound> rounds;

    @BeforeEach
    void setUp() {
        this.repository = new FakeStatsRepository();
        this.rounds = this.repository.rounds;
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
        this.repository.saveThrows = new IllegalStateException("database is gone");
        RoundStatsCollector collector = collector(this.repository, ROUND_ID);
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

    @Test
    @DisplayName("The names of the participants are read at the start and stored with the round")
    void namesAreStoredWithTheRound() {
        RoundStatsCollector collector = new RoundStatsCollector(this.repository, CLOCK, ZoneOffset.UTC,
                () -> ROUND_ID, uuid -> uuid.equals(ALICE) ? "Alice" : null, _ -> {
        });
        collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));
        collector.gameFinished(GameFinishEvent.Reason.TIME_OVER);

        assertEquals(Map.of(ALICE, "Alice"), this.rounds.getFirst().names(), "the name must travel with the round");
    }

    @Test
    @DisplayName("The committed totals are handed on once the write is done")
    void committedTotalsAreHandedOn() {
        YearStatsView committed = YearStatsView.empty(ALICE, 2026);
        this.repository.totals = _ -> Map.of(ALICE, committed);
        List<Map<UUID, YearStatsView>> handed = new ArrayList<>();
        RoundStatsCollector collector = new RoundStatsCollector(this.repository, CLOCK, ZoneOffset.UTC,
                () -> ROUND_ID, _ -> null, handed::add);
        collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));

        collector.gameFinished(GameFinishEvent.Reason.TIME_OVER);

        assertEquals(List.of(Map.of(ALICE, committed)), handed, "the handler must get the totals the repository committed");
    }

    @Test
    @DisplayName("A write that fails hands nothing on and does not escape")
    void failedWriteHandsNothingOn() {
        this.repository.saveFails = new IllegalStateException("database is gone");
        List<Map<UUID, YearStatsView>> handed = new ArrayList<>();
        RoundStatsCollector collector = new RoundStatsCollector(this.repository, CLOCK, ZoneOffset.UTC,
                () -> ROUND_ID, _ -> null, handed::add);
        collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));

        assertDoesNotThrow(() -> collector.gameFinished(GameFinishEvent.Reason.TIME_OVER));

        assertTrue(handed.isEmpty(), "without a commit there are no totals to show");
    }

    @Test
    @DisplayName("A handler that throws does not escape into the game")
    void throwingHandlerDoesNotEscape() {
        RoundStatsCollector collector = new RoundStatsCollector(this.repository, CLOCK, ZoneOffset.UTC,
                () -> ROUND_ID, _ -> null, _ -> {
            throw new IllegalStateException("broken display");
        });
        collector.roundStarted(Map.of(ALICE, Role.SURVIVOR));

        assertDoesNotThrow(() -> collector.gameFinished(GameFinishEvent.Reason.TIME_OVER),
                "a display failure must never reach the game");
    }
}
