package net.onelitefeather.cygnus.stats;

import net.onelitefeather.cygnus.stats.RoundStats.Outcome;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the tracker against a hand-driven clock. The events are plain
 * method calls; nothing here waits or reads the system time.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class RoundStatsTrackerTest {

    private static final Instant START = Instant.parse("2026-06-01T12:00:00Z");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final UUID SLENDER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    private static final UUID ROUND_ID = UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff");

    private MutableClock clock;
    private final List<FinishedRound> rounds = new ArrayList<>();
    private RoundStatsTracker tracker;

    @BeforeEach
    void setUp() {
        this.clock = new MutableClock(START);
        this.tracker = new RoundStatsTracker(this.clock, BERLIN);
    }

    private void finish(RoundStatsTracker target, FinishReason reason) {
        target.finish(reason, () -> ROUND_ID).ifPresent(this.rounds::add);
    }

    private void startRound() {
        Map<UUID, Role> participants = new LinkedHashMap<>();
        participants.put(SLENDER, Role.SLENDER);
        participants.put(ALICE, Role.SURVIVOR);
        participants.put(BOB, Role.SURVIVOR);
        participants.put(CAROL, Role.SURVIVOR);
        this.tracker.roundStarted(participants);
    }

    private RoundStats statsOf(UUID player) {
        assertEquals(1, this.rounds.size(), "exactly one round must have been handed over");
        return this.rounds.getFirst().players().stream()
                .filter(stats -> stats.playerUuid().equals(player))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no statistics for " + player));
    }

    @Test
    @DisplayName("Every team member becomes a participant with the role they started in")
    void teamMembersBecomeParticipants() {
        startRound();
        finish(this.tracker, FinishReason.TIME_OVER);

        List<RoundStats> players = this.rounds.getFirst().players();
        assertEquals(4, players.size(), "one Slender and three Survivors are tracked");
        assertEquals(Role.SLENDER, statsOf(SLENDER).role());
        assertEquals(Role.SURVIVOR, statsOf(ALICE).role());
    }

    @Test
    @DisplayName("A player who is not a participant gets no statistics")
    void spectatorsAreNotTracked() {
        startRound();
        UUID spectator = UUID.randomUUID();
        this.tracker.pageFound(spectator, 1, 8);
        this.tracker.damageTaken(spectator, 3.0D);
        this.tracker.playerLeft(spectator);
        finish(this.tracker, FinishReason.TIME_OVER);

        assertTrue(this.rounds.getFirst().players().stream().noneMatch(stats -> stats.playerUuid().equals(spectator)),
                "a spectator must not get a row");
    }

    @Test
    @DisplayName("A found page counts for the finder")
    void pageFoundCountsForTheFinder() {
        startRound();
        this.tracker.pageFound(ALICE, 1, 8);
        this.tracker.pageFound(ALICE, 2, 8);
        this.tracker.pageFound(BOB, 3, 8);
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(2, statsOf(ALICE).pagesFound());
        assertEquals(1, statsOf(BOB).pagesFound());
        assertEquals(0, statsOf(CAROL).pagesFound());
    }

    @Test
    @DisplayName("The page that completes the round makes its finder the finisher")
    void lastPageMarksTheFinisher() {
        startRound();
        this.tracker.pageFound(ALICE, 7, 8);
        this.tracker.pageFound(BOB, 8, 8);
        finish(this.tracker, FinishReason.ALL_PAGES_FOUND);

        assertTrue(statsOf(BOB).finisher(), "the finder of page 8 of 8 is the finisher");
        assertFalse(statsOf(ALICE).finisher());
    }

    @Test
    @DisplayName("The Slender counts the pages the survivors found against them")
    void slenderCountsPagesAgainst() {
        startRound();
        this.tracker.pageFound(ALICE, 1, 8);
        this.tracker.pageFound(BOB, 2, 8);
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(2, statsOf(SLENDER).pagesAgainst());
        assertEquals(0, statsOf(ALICE).pagesAgainst(), "only the Slender carries this number");
    }

    @Test
    @DisplayName("A death records the victim's time survived and credits the kill to the Slender")
    void deathAndKill() {
        startRound();
        this.clock.advance(Duration.ofSeconds(90));
        this.tracker.playerDied(ALICE, SLENDER);
        this.clock.advance(Duration.ofSeconds(210));
        finish(this.tracker, FinishReason.TIME_OVER);

        RoundStats alice = statsOf(ALICE);
        assertTrue(alice.died());
        assertFalse(alice.survived(), "a dead survivor did not survive the round");
        assertEquals(90, alice.timeSurvivedSeconds());
        assertEquals(300, alice.playTimeSeconds(), "the play time runs to the round end");
        assertEquals(1, statsOf(SLENDER).kills());
    }

    @Test
    @DisplayName("A survivor alive at the end survived for the whole round")
    void survivorAliveAtTheEnd() {
        startRound();
        this.clock.advance(Duration.ofSeconds(300));
        finish(this.tracker, FinishReason.TIME_OVER);

        RoundStats bob = statsOf(BOB);
        assertTrue(bob.survived());
        assertEquals(300, bob.timeSurvivedSeconds());
    }

    @Test
    @DisplayName("A death without a known killer credits nobody")
    void deathWithoutKillerCreditsNobody() {
        startRound();
        this.tracker.playerDied(ALICE, null);
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(0, statsOf(SLENDER).kills());
        assertTrue(statsOf(ALICE).died());
    }

    @Test
    @DisplayName("Damage and painkillers add up per survivor")
    void damageAndPainkillers() {
        startRound();
        this.tracker.damageTaken(ALICE, 2.5D);
        this.tracker.damageTaken(ALICE, 1.5D);
        this.tracker.painkillerUsed(ALICE);
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(4.0D, statsOf(ALICE).damageTaken(), 1.0E-9);
        assertEquals(1, statsOf(ALICE).painkillersUsed());
        assertEquals(0, statsOf(BOB).painkillersUsed());
    }

    @Test
    @DisplayName("Survivors win on time over, all pages and a Slender who left; the Slender loses")
    void survivorsWinOnTheirReasons() {
        for (FinishReason reason : List.of(FinishReason.TIME_OVER, FinishReason.ALL_PAGES_FOUND, FinishReason.SLENDER_LEFT)) {
            this.rounds.clear();
            startRound();
            finish(this.tracker, reason);

            assertEquals(Outcome.WIN, statsOf(ALICE).outcome(), "survivor outcome for " + reason);
            assertEquals(Outcome.LOSS, statsOf(SLENDER).outcome(), "Slender outcome for " + reason);
        }
    }

    @Test
    @DisplayName("The Slender wins when all survivors are dead or the last one left; survivors lose")
    void slenderWinsOnItsReasons() {
        for (FinishReason reason : List.of(FinishReason.ALL_SURVIVOR_DEAD, FinishReason.SURVIVOR_LEFT)) {
            this.rounds.clear();
            startRound();
            finish(this.tracker, reason);

            assertEquals(Outcome.WIN, statsOf(SLENDER).outcome(), "Slender outcome for " + reason);
            assertEquals(Outcome.LOSS, statsOf(ALICE).outcome(), "survivor outcome for " + reason);
        }
    }

    @Test
    @DisplayName("A dead survivor still wins with the survivors")
    void deadSurvivorsShareTheWin() {
        startRound();
        this.tracker.playerDied(ALICE, SLENDER);
        finish(this.tracker, FinishReason.ALL_PAGES_FOUND);

        assertEquals(Outcome.WIN, statsOf(ALICE).outcome());
    }

    @Test
    @DisplayName("A player who quits is abandoned and their play time ends at the quit")
    void quitIsAbandoned() {
        startRound();
        this.clock.advance(Duration.ofSeconds(120));
        this.tracker.playerLeft(BOB);
        this.clock.advance(Duration.ofSeconds(180));
        finish(this.tracker, FinishReason.TIME_OVER);

        RoundStats bob = statsOf(BOB);
        assertEquals(Outcome.ABANDONED, bob.outcome());
        assertEquals(120, bob.playTimeSeconds());
        assertFalse(bob.survived(), "someone who quit did not survive to the end");
    }

    @Test
    @DisplayName("Pages found before quitting still count for the Slender's pages against")
    void abandonedPagesCountAgainstTheSlender() {
        startRound();
        this.tracker.pageFound(BOB, 1, 8);
        this.tracker.playerLeft(BOB);
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(1, statsOf(SLENDER).pagesAgainst());
        assertEquals(1, statsOf(BOB).pagesFound());
    }

    @Test
    @DisplayName("A round finished at 2026-12-31T23:30Z already counts for 2027 in Berlin")
    void turnOfTheYearUsesTheConfiguredZone() {
        this.clock.set(Instant.parse("2026-12-31T23:30:00Z"));
        startRound();
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(2027, this.rounds.getFirst().year());
    }

    @Test
    @DisplayName("The same instant counts for 2026 in a zone that is still in the old year")
    void sameInstantIsStillTheOldYearElsewhere() {
        RoundStatsTracker utc = new RoundStatsTracker(this.clock, ZoneOffset.UTC);
        this.clock.set(Instant.parse("2026-12-31T23:30:00Z"));
        utc.roundStarted(Map.of(ALICE, Role.SURVIVOR));
        finish(utc, FinishReason.TIME_OVER);

        assertEquals(2026, this.rounds.getFirst().year());
    }

    @Test
    @DisplayName("The finish time comes from the clock")
    void finishTimeComesFromTheClock() {
        startRound();
        this.clock.advance(Duration.ofMinutes(15));
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(START.plus(Duration.ofMinutes(15)), this.rounds.getFirst().finishedAt());
    }

    @Test
    @DisplayName("Finishing without a started round writes nothing")
    void finishWithoutRoundWritesNothing() {
        finish(this.tracker, FinishReason.TIME_OVER);

        assertTrue(this.rounds.isEmpty(), "no participants means no round");
    }

    @Test
    @DisplayName("The state is cleared after the round so a second finish writes nothing")
    void stateIsClearedAfterTheRound() {
        startRound();
        finish(this.tracker, FinishReason.TIME_OVER);
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(1, this.rounds.size(), "the second finish must not write the round again");
    }

    @Test
    @DisplayName("A new round starts from zero")
    void newRoundStartsFromZero() {
        startRound();
        this.tracker.pageFound(ALICE, 1, 8);
        finish(this.tracker, FinishReason.TIME_OVER);
        this.rounds.clear();

        startRound();
        finish(this.tracker, FinishReason.TIME_OVER);

        assertEquals(0, statsOf(ALICE).pagesFound(), "counters of the previous round must not leak");
    }

    /** A clock the test moves by hand. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }
}
