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
    @DisplayName("Seen until he moves out of sight, he tells that the target spotted him")
    void reportsBeingSpotted() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        List<UUID> spotted = new ArrayList<>();
        CreekActions actions = Contexts.actions(_ -> {}, _ -> {}, _ -> {}, (_, _) -> {}, _ -> {}, (_, _) -> {},
                spotted::add);

        state.tick(Contexts.context(0L, body, Contexts.route(), actions, new Random(7), target(0.3D, true)));
        state.tick(Contexts.context(699L, body, Contexts.route(), actions, new Random(7), target(0.3D, true)));
        assertTrue(spotted.isEmpty(), "a glance is not enough");

        state.tick(Contexts.context(700L, body, Contexts.route(), actions, new Random(7), target(0.3D, true)));

        assertEquals(List.of(TARGET), spotted);
    }

    /** Answers the first roll with the given value and every later one from a seeded source. */
    private static RandomGenerator firstRoll(double first) {
        Random rest = new Random(7);
        return new RandomGenerator() {
            private boolean rolled;

            @Override
            public long nextLong() {
                return rest.nextLong();
            }

            @Override
            public double nextDouble() {
                if (this.rolled) return rest.nextDouble();
                this.rolled = true;
                return first;
            }
        };
    }

    private static CreekContext looked(long now, RecordingBody body, RandomGenerator random, CreekMood mood,
                                       SurvivorView... survivors) {
        return Contexts.mood(Contexts.context(now, body, Contexts.route(), Contexts.NO_ACTIONS, random, survivors), mood);
    }

    /** Another survivor ten blocks in front of the target. */
    private static final SurvivorView TEAMMATE = new SurvivorView(OTHER, new Pos(0, 40, 10), 0.0D, false);

    @Test
    @DisplayName("The shadow, looked at long enough, turns up right behind a teammate in the target's view")
    void shadowStandsBehindATeammate() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        RandomGenerator random = firstRoll(0.0D);

        state.tick(looked(0L, body, random, CreekMood.SHADOW, target(0.3D, true), TEAMMATE));
        state.tick(looked(700L, body, random, CreekMood.SHADOW, target(0.3D, true), TEAMMATE));

        assertEquals(new Pos(0, 40, 12).asVec(), body.position.asVec());
        assertTrue(Contexts.SIGHT.inView(target(0.3D, true).eyes(), body.position.add(0, SpotFinder.BODY_CENTRE, 0)));
    }

    @Test
    @DisplayName("The shadow stands behind a teammate only once per stalk")
    void onlyOncePerStalk() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        RandomGenerator random = firstRoll(0.0D);
        state.tick(looked(0L, body, random, CreekMood.SHADOW, target(0.3D, true), TEAMMATE));
        state.tick(looked(700L, body, random, CreekMood.SHADOW, target(0.3D, true), TEAMMATE));

        state.tick(looked(800L, body, random, CreekMood.SHADOW, target(0.3D, true), TEAMMATE));
        state.tick(looked(1500L, body, random, CreekMood.SHADOW, target(0.3D, true), TEAMMATE));

        assertEquals(2, body.teleports.size());
        assertFalse(Contexts.SIGHT.inView(target(0.3D, true).eyes(), body.position.add(0, SpotFinder.BODY_CENTRE, 0)),
                "the second time he moves out of sight as usual");
    }

    @Test
    @DisplayName("Without a teammate nearby the shadow moves out of sight as usual, but comes back closer")
    void noTeammateRelocatesAsUsual() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        RandomGenerator random = firstRoll(0.0D);

        state.tick(looked(0L, body, random, CreekMood.SHADOW, target(0.3D, true)));
        state.tick(looked(700L, body, random, CreekMood.SHADOW, target(0.3D, true)));

        assertEquals(1, body.teleports.size());
        double distance = body.position.distance(target(0.3D, true).position());
        assertTrue(distance >= StalkState.END_MIN_DISTANCE - 1.0E-6 && distance <= StalkState.END_MAX_DISTANCE + 1.0E-6,
                "distance was " + distance);
        assertFalse(Contexts.SIGHT.inView(target(0.3D, true).eyes(), body.position.add(0, SpotFinder.BODY_CENTRE, 0)));
    }

    @Test
    @DisplayName("The normal creek ignores teammates and keeps its distance after a look")
    void normalUnchanged() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        RandomGenerator random = firstRoll(0.0D);

        state.tick(looked(0L, body, random, CreekMood.NORMAL, target(0.3D, true), TEAMMATE));
        state.tick(looked(700L, body, random, CreekMood.NORMAL, target(0.3D, true), TEAMMATE));

        double distance = body.position.distance(target(0.3D, true).position());
        assertTrue(distance >= 20 - 1.0E-6 && distance <= 35 + 1.0E-6, "distance was " + distance);
    }

    private static CreekContext decoying(long now, RecordingBody body, List<Pos> decoys, CreekMood mood) {
        CreekActions actions = Contexts.actions(_ -> {}, _ -> {}, _ -> {}, (_, _) -> {}, _ -> {}, (_, _) -> {}, _ -> {},
                (survivor, where) -> {
                    assertEquals(TARGET, survivor);
                    decoys.add(where);
                });
        return Contexts.mood(Contexts.context(now, body, Contexts.route(), actions, Contexts.rolling(0.0D),
                target(0.3D, false)), mood);
    }

    @Test
    @DisplayName("Moving unseen, the trickster once per stalk leaves a doppelganger in the target's view")
    void tricksterPlacesADecoy() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -10));
        StalkState state = new StalkState(TARGET, 60_000L);
        List<Pos> decoys = new ArrayList<>();

        state.tick(decoying(0L, body, decoys, CreekMood.TRICKSTER));

        assertEquals(1, decoys.size());
        Pos decoy = decoys.getFirst();
        double distance = decoy.distance(target(0.3D, false).position());
        assertTrue(distance >= StalkState.DECOY_MIN_DISTANCE - 1.0E-6 && distance <= StalkState.DECOY_MAX_DISTANCE + 1.0E-6,
                "distance was " + distance);
        assertTrue(Contexts.SIGHT.inView(target(0.3D, false).eyes(), decoy.add(0, SpotFinder.BODY_CENTRE, 0)));

        body.position = new Pos(0, 40, -10);
        state.tick(decoying(100L, body, decoys, CreekMood.TRICKSTER));
        assertEquals(1, decoys.size(), "only one per stalk");
    }

    @Test
    @DisplayName("The normal creek never leaves a doppelganger")
    void normalPlacesNoDecoy() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -10));
        List<Pos> decoys = new ArrayList<>();

        new StalkState(TARGET, 60_000L).tick(decoying(0L, body, decoys, CreekMood.NORMAL));

        assertTrue(decoys.isEmpty());
    }

    @Test
    @DisplayName("Late in a thinned out round he stays visible longer before moving out of sight")
    void lingersLongerLate() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);
        Calm calm = new Calm(1.0D);

        state.tick(Contexts.calm(at(0L, body, target(0.3D, true)), calm));
        state.tick(Contexts.calm(at(1049L, body, target(0.3D, true)), calm));
        assertTrue(body.teleports.isEmpty(), "the moment he lingers is half as long again");

        state.tick(Contexts.calm(at(1050L, body, target(0.3D, true)), calm));
        assertEquals(1, body.teleports.size());
    }

    @Test
    @DisplayName("Late in a thinned out round he lets the target get further away before following")
    void followsLaterLate() {
        // 45 blocks away: beyond the band of 35, but within one and a half times it.
        RecordingBody body = new RecordingBody(new Pos(0, 40, -45));
        new StalkState(TARGET, 60_000L).tick(Contexts.calm(at(0L, body, target(0.3D, false)), new Calm(1.0D)));

        assertTrue(body.teleports.isEmpty());
    }

    @Test
    @DisplayName("Late in a thinned out round the breather before the next hunt lasts longer")
    void huntCooldownGrowsLate() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        HuntCooldowns hunts = new HuntCooldowns(45_000L);
        hunts.ended(TARGET, 0L);
        StalkState state = new StalkState(TARGET, 120_000L);
        Calm calm = new Calm(1.0D);
        state.enter(Contexts.calm(Contexts.hunting(0L, body, hunts, new ArrayList<>(), target(0.9D, false)), calm));

        assertSame(state, state.tick(Contexts.calm(Contexts.hunting(67_499L, body, hunts, new ArrayList<>(), target(0.9D, false)), calm)));
        assertInstanceOf(HuntState.class,
                state.tick(Contexts.calm(Contexts.hunting(67_500L, body, hunts, new ArrayList<>(), target(0.9D, false)), calm)));
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

    @Test
    @DisplayName("A spared target ends the stalk")
    void sparedTargetEndsTheStalk() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        SurvivorView spared = new SurvivorView(TARGET, new Pos(0, 40, 0, 0, 0), 0.3D, false, true, true);

        assertSame(DoneState.INSTANCE, new StalkState(TARGET, 60_000L).tick(at(0L, body, spared)));
    }
}
