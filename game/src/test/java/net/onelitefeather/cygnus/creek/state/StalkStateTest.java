package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.creek.world.SpotFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StalkStateTest {

    private static final UUID TARGET = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();
    /** 25 blocks away, behind and to the side of a target looking towards positive Z. */
    private static final Pos IN_THE_BAND = new Pos(15, 40, -20);

    private static SurvivorView target(double dread, boolean sees) {
        return new SurvivorView(TARGET, new Pos(0, 40, 0, 0, 0), dread, sees);
    }

    private static CreekContext at(long now, RecordingBody body, SurvivorView... survivors) {
        return Contexts.context(now, body, Contexts.route(), new ArrayList<>(), survivors);
    }

    @Test
    @DisplayName("Entering shows him to the target alone")
    void enterShowsOnlyTheTarget() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        new StalkState(TARGET, 60_000L).enter(at(0L, body, target(0.3D, false),
                new SurvivorView(OTHER, new Pos(40, 40, 40), 0.0D, false)));

        assertEquals(Set.of(TARGET), body.viewers);
    }

    @Test
    @DisplayName("Without the target he is done")
    void vanishesWithoutTheTarget() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        assertSame(DoneState.INSTANCE, new StalkState(TARGET, 60_000L).tick(at(0L, body)));
    }

    @Test
    @DisplayName("When the time is up he is done")
    void vanishesWhenTheTimeIsUp() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        assertSame(DoneState.INSTANCE, new StalkState(TARGET, 1000L).tick(at(1000L, body, target(0.3D, false))));
    }

    @Test
    @DisplayName("At the hunt threshold the stalk turns into a hunt once it has run long enough")
    void turnsIntoAHunt() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        state.enter(at(0L, body, target(0.6D, false)));

        assertSame(state, state.tick(at(9_999L, body, target(0.6D, false))), "too early for a hunt");
        assertInstanceOf(HuntState.class, state.tick(at(10_000L, body, target(0.6D, false))));
    }

    @Test
    @DisplayName("Below the hunt threshold the stalk goes on, however long it runs")
    void staysAStalkBelowTheThreshold() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        state.enter(at(0L, body, target(0.59D, false)));

        assertSame(state, state.tick(at(30_000L, body, target(0.59D, false))));
    }

    @Test
    @DisplayName("Right after a hunt the survivor gets a breather before the next one")
    void huntCooldownHoldsTheNextHunt() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        HuntCooldowns hunts = new HuntCooldowns(45_000L);
        hunts.ended(TARGET, 0L);
        StalkState state = new StalkState(TARGET, 120_000L);
        state.enter(Contexts.hunting(0L, body, hunts, new ArrayList<>(), target(0.9D, false)));

        assertSame(state, state.tick(Contexts.hunting(44_999L, body, hunts, new ArrayList<>(), target(0.9D, false))));
        assertInstanceOf(HuntState.class,
                state.tick(Contexts.hunting(45_000L, body, hunts, new ArrayList<>(), target(0.9D, false))));
    }

    @Test
    @DisplayName("Another survivor's hunt does not hold this one")
    void huntCooldownIsPerSurvivor() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        HuntCooldowns hunts = new HuntCooldowns(45_000L);
        hunts.ended(OTHER, 0L);
        StalkState state = new StalkState(TARGET, 120_000L);
        state.enter(Contexts.hunting(0L, body, hunts, new ArrayList<>(), target(0.9D, false)));

        assertInstanceOf(HuntState.class,
                state.tick(Contexts.hunting(10_000L, body, hunts, new ArrayList<>(), target(0.9D, false))));
    }

    @Test
    @DisplayName("Seen, he lingers for a moment and then moves out of sight")
    void lingersThenMoves() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);

        state.tick(at(0L, body, target(0.3D, true)));
        state.tick(at(699L, body, target(0.3D, true)));
        assertTrue(body.teleports.isEmpty(), "he has to be seen for a moment first");

        state.tick(at(700L, body, target(0.3D, true)));

        assertEquals(1, body.teleports.size());
        Pos spot = body.position;
        double distance = spot.distance(target(0.3D, true).position());
        assertTrue(distance >= 20 - 1.0E-6 && distance <= 35 + 1.0E-6, "distance was " + distance);
        assertFalse(Contexts.SIGHT.inView(target(0.3D, true).eyes(), spot.add(0, SpotFinder.BODY_CENTRE, 0)));
    }

    @Test
    @DisplayName("When the target comes too close he moves back into the band")
    void relocatesWhenTooClose() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -10));
        new StalkState(TARGET, 60_000L).tick(at(0L, body, target(0.3D, false)));

        assertEquals(1, body.teleports.size());
    }

    @Test
    @DisplayName("Inside the band and unseen he just stares")
    void staresInsideTheBand() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);

        assertSame(state, state.tick(at(0L, body, target(0.3D, false))));
        assertTrue(body.teleports.isEmpty());
        assertEquals(target(0.3D, false).eyes(), body.lookedAt);
    }

    private static double distanceToTarget(RecordingBody body) {
        return body.position.distance(target(0.3D, false).position());
    }

    @Test
    @DisplayName("Halfway through the stalk the band has shrunk to about 14 to 25 blocks")
    void bandShrinksByHalfway() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -17));
        StalkState state = new StalkState(TARGET, 60_000L);
        state.enter(at(0L, body, target(0.3D, false)));

        state.tick(at(30_000L, body, target(0.3D, false)));

        assertTrue(body.teleports.isEmpty(), "17 blocks is inside the band halfway through");
    }

    @Test
    @DisplayName("Late in the stalk he closes in when he is too far away")
    void closesInLate() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -18));
        StalkState state = new StalkState(TARGET, 60_000L);
        state.enter(at(0L, body, target(0.3D, false)));

        state.tick(at(59_000L, body, target(0.3D, false)));

        assertEquals(1, body.teleports.size());
        double distance = distanceToTarget(body);
        assertTrue(distance >= 8.0D - 1.0E-6 && distance <= 15.5D, "distance was " + distance);
    }

    @Test
    @DisplayName("Seen late in the stalk he comes back closer")
    void comesBackCloserWhenSeenLate() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -12));
        StalkState state = new StalkState(TARGET, 60_000L);
        state.enter(at(0L, body, target(0.3D, false)));

        state.tick(at(58_000L, body, target(0.3D, true)));
        assertTrue(body.teleports.isEmpty(), "12 blocks is inside the late band");
        state.tick(at(58_700L, body, target(0.3D, true)));

        assertEquals(1, body.teleports.size());
        double distance = distanceToTarget(body);
        assertTrue(distance >= 8.0D - 1.0E-6 && distance <= 15.5D, "distance was " + distance);
    }

    // ---- sounds ----

    /** Writes down when he was heard and how far into the stalk. */
    private static final class SoundLog {
        private final List<Long> times = new ArrayList<>();
        private final List<Double> progress = new ArrayList<>();
        private long now;

        private CreekContext at(long now, RecordingBody body, RandomGenerator random, SurvivorView... survivors) {
            this.now = now;
            CreekActions actions = Contexts.actions(_ -> {}, _ -> {}, _ -> {}, (_, _) -> {}, _ -> {},
                    (survivor, progress) -> {
                        assertEquals(TARGET, survivor);
                        this.times.add(this.now);
                        this.progress.add(progress);
                    });
            return Contexts.context(now, body, Contexts.route(), actions, random, survivors);
        }
    }

    @Test
    @DisplayName("The gap between two sounds shrinks from 6 to 10 seconds down to 3 to 5")
    void soundGapShrinks() {
        assertEquals(6000L, StalkState.soundGapMillis(0.0D, 0.0D));
        assertEquals(10_000L, StalkState.soundGapMillis(0.0D, 1.0D));
        assertEquals(3000L, StalkState.soundGapMillis(1.0D, 0.0D));
        assertEquals(5000L, StalkState.soundGapMillis(1.0D, 1.0D));
        assertEquals(4500L, StalkState.soundGapMillis(0.5D, 0.0D));
    }

    @Test
    @DisplayName("Unseen in the band, he is heard once the first gap is over, and again after the next")
    void heardInIntervals() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 600_000L);
        SoundLog log = new SoundLog();
        RandomGenerator shortest = Contexts.rolling(0.0D);
        state.enter(log.at(0L, body, shortest, target(0.3D, false)));

        for (long now = 0L; now <= 12_000L; now += 100L) {
            state.tick(log.at(now, body, shortest, target(0.3D, false)));
        }

        assertTrue(body.teleports.isEmpty());
        assertEquals(List.of(6000L, 12_000L), log.times);
        assertEquals(0.01D, log.progress.getFirst(), 1.0E-9);
    }

    @Test
    @DisplayName("While the target looks at him he makes no sound")
    void silentWhileSeen() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 600_000L);
        SoundLog log = new SoundLog();
        state.enter(log.at(0L, body, new Random(7), target(0.3D, true)));

        for (long now = 0L; now <= 12_000L; now += 100L) {
            state.tick(log.at(now, body, new Random(7), target(0.3D, true)));
        }

        assertTrue(log.times.isEmpty());
    }

    @Test
    @DisplayName("Moving back into the band out of sight, he is heard at once")
    void heardWhenMovingUnseen() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -10));
        StalkState state = new StalkState(TARGET, 600_000L);
        SoundLog log = new SoundLog();
        state.enter(log.at(0L, body, new Random(7), target(0.3D, false)));

        state.tick(log.at(100L, body, new Random(7), target(0.3D, false)));

        assertEquals(1, body.teleports.size());
        assertEquals(List.of(100L), log.times);
    }
}
