package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.creek.dread.HuntEnd;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuntStateTest {

    private static final UUID TARGET = UUID.randomUUID();
    /** At the origin, looking towards negative Z - away from where he comes from. */
    private static final Pos TARGET_POS = new Pos(0, 40, 0, 180, 0);

    private static SurvivorView target(boolean sees) {
        return new SurvivorView(TARGET, TARGET_POS, 0.7D, sees);
    }

    private static CreekContext at(long now, RecordingBody body, List<UUID> caught, boolean sees) {
        return Contexts.context(now, body, Contexts.route(), caught, target(sees));
    }

    @Test
    @DisplayName("Entering shows him to the target and lights his eyes")
    void enterLightsTheEyes() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        new HuntState(TARGET, 30_000L).enter(at(0L, body, new ArrayList<>(), false));

        assertEquals(Set.of(TARGET), body.viewers);
        assertTrue(body.aggressive);
    }

    @Test
    @DisplayName("Seen, he freezes")
    void freezesWhenSeen() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        HuntState state = new HuntState(TARGET, 30_000L);

        assertSame(state, state.tick(at(0L, body, new ArrayList<>(), true)));
        assertTrue(body.frozen);
        assertNull(body.goal);
    }

    @Test
    @DisplayName("The hunter creeps on towards the target while it watches him")
    void hunterCreepsWhileWatched() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        CreekContext ctx = Contexts.mood(at(0L, body, new ArrayList<>(), true), CreekMood.HUNTER);

        new HuntState(TARGET, 30_000L).tick(ctx);

        assertFalse(body.frozen);
        assertEquals(TARGET_POS, body.goal);
        assertEquals(ctx.tuning().watchedHuntSpeed(), body.speed, 1.0E-9);
    }

    @Test
    @DisplayName("Watched, the hunter never catches, however close he gets, but he does once the target looks away")
    void creepingNeverCatchesWhileWatched() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 1));
        List<UUID> caught = new ArrayList<>();
        HuntState state = new HuntState(TARGET, 30_000L);

        assertSame(state, state.tick(Contexts.mood(at(0L, body, caught, true), CreekMood.HUNTER)));
        assertTrue(caught.isEmpty());

        assertSame(DoneState.INSTANCE, state.tick(Contexts.mood(at(100L, body, caught, false), CreekMood.HUNTER)));
        assertEquals(List.of(TARGET), caught);
    }

    @Test
    @DisplayName("Unseen, he closes in")
    void closesInWhenUnseen() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        new HuntState(TARGET, 30_000L).tick(at(0L, body, new ArrayList<>(), false));

        assertFalse(body.frozen);
        assertEquals(TARGET_POS, body.goal);
        assertEquals(Contexts.CONFIG.hunt().speed(), body.speed);
    }

    @Test
    @DisplayName("Close enough and unseen, he catches the target")
    void catchesTheTarget() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 1));
        List<UUID> caught = new ArrayList<>();

        CreekState next = new HuntState(TARGET, 30_000L).tick(at(0L, body, caught, false));

        assertEquals(List.of(TARGET), caught);
        assertSame(DoneState.INSTANCE, next);
    }

    @Test
    @DisplayName("Close enough but seen, he does not")
    void noCatchWhileSeen() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 1));
        List<UUID> caught = new ArrayList<>();

        new HuntState(TARGET, 30_000L).tick(at(0L, body, caught, true));

        assertTrue(caught.isEmpty());
    }

    @Test
    @DisplayName("Close enough but behind a wall, he does not")
    void noCatchBehindAWall() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 1));
        List<UUID> caught = new ArrayList<>();
        SurvivorView hidden = new SurvivorView(TARGET, TARGET_POS, 0.7D, false, false);

        CreekState next = new HuntState(TARGET, 30_000L)
                .tick(Contexts.context(0L, body, Contexts.route(), caught, hidden));

        assertTrue(caught.isEmpty());
        assertFalse(next instanceof DoneState);
    }

    @Test
    @DisplayName("When the time is up he gives up")
    void givesUp() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        assertSame(DoneState.INSTANCE, new HuntState(TARGET, 30_000L).tick(at(30_000L, body, new ArrayList<>(), false)));
    }

    @Test
    @DisplayName("Stuck, he takes a shortcut close to the target")
    void takesAShortcut() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        HuntState state = new HuntState(TARGET, 30_000L);
        state.enter(at(0L, body, new ArrayList<>(), false));

        state.tick(at(0L, body, new ArrayList<>(), false));
        state.tick(at(3000L, body, new ArrayList<>(), false));

        assertEquals(1, body.teleports.size());
        double distance = body.position.distance(TARGET_POS);
        assertTrue(distance >= HuntState.NEAR_MIN - 1.0E-6 && distance <= HuntState.NEAR_MAX + 1.0E-6, "distance was " + distance);
    }

    @Test
    @DisplayName("Late in a thinned out round he closes in more slowly")
    void closesInSlowerLate() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        new HuntState(TARGET, 30_000L).tick(Contexts.calm(at(0L, body, new ArrayList<>(), false), new Calm(1.0D)));

        assertEquals(Contexts.CONFIG.hunt().speed() * 0.8D, body.speed, 1.0E-9);
    }

    @Test
    @DisplayName("Late in a thinned out round he waits longer before taking a shortcut")
    void takesAShortcutLaterLate() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        HuntState state = new HuntState(TARGET, 30_000L);
        Calm calm = new Calm(1.0D);
        state.enter(Contexts.calm(at(0L, body, new ArrayList<>(), false), calm));

        state.tick(Contexts.calm(at(0L, body, new ArrayList<>(), false), calm));
        state.tick(Contexts.calm(at(3000L, body, new ArrayList<>(), false), calm));
        assertTrue(body.teleports.isEmpty(), "three seconds stuck are not enough any more");

        state.tick(Contexts.calm(at(4500L, body, new ArrayList<>(), false), calm));
        assertEquals(1, body.teleports.size());
    }

    @Test
    @DisplayName("A catch starts the survivor's breather")
    void catchStartsTheCooldown() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 1));
        HuntCooldowns hunts = new HuntCooldowns(45_000L);

        new HuntState(TARGET, 30_000L).tick(Contexts.hunting(5_000L, body, hunts, new ArrayList<>(), target(false)));

        assertFalse(hunts.ready(TARGET, 49_999L, new CreekTuning(Contexts.CONFIG, Calm.NONE)));
        assertTrue(hunts.ready(TARGET, 50_000L, new CreekTuning(Contexts.CONFIG, Calm.NONE)));
    }

    @Test
    @DisplayName("A hunt that runs out of time starts the breather too")
    void timeoutStartsTheCooldown() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        HuntCooldowns hunts = new HuntCooldowns(45_000L);

        CreekState next = new HuntState(TARGET, 30_000L)
                .tick(Contexts.hunting(30_000L, body, hunts, new ArrayList<>(), target(false)));

        assertSame(DoneState.INSTANCE, next);
        assertFalse(hunts.ready(TARGET, 74_999L, new CreekTuning(Contexts.CONFIG, Calm.NONE)));
    }

    @Test
    @DisplayName("While the hunt goes on, no breather starts")
    void runningHuntStartsNoCooldown() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        HuntCooldowns hunts = new HuntCooldowns(45_000L);

        new HuntState(TARGET, 30_000L).tick(Contexts.hunting(0L, body, hunts, new ArrayList<>(), target(false)));

        assertTrue(hunts.ready(TARGET, 0L, new CreekTuning(Contexts.CONFIG, Calm.NONE)));
    }

    @Test
    @DisplayName("A running hunt has no end yet")
    void runningHuntHasNoEnd() {
        HuntState hunt = new HuntState(TARGET, 30_000L);

        hunt.tick(at(0L, new RecordingBody(new Pos(0, 40, 10)), new ArrayList<>(), false));

        assertNull(hunt.end());
    }

    @Test
    @DisplayName("A catch ends the hunt as caught")
    void catchEndsAsCaught() {
        HuntState hunt = new HuntState(TARGET, 30_000L);

        hunt.tick(at(0L, new RecordingBody(new Pos(0, 40, 1)), new ArrayList<>(), false));

        assertEquals(HuntEnd.CAUGHT, hunt.end());
    }

    @Test
    @DisplayName("Running out of time ends the hunt as a timeout")
    void timeoutEndsAsTimeout() {
        HuntState hunt = new HuntState(TARGET, 30_000L);

        hunt.tick(at(30_000L, new RecordingBody(new Pos(0, 40, 10)), new ArrayList<>(), false));

        assertEquals(HuntEnd.TIMEOUT, hunt.end());
    }

    @Test
    @DisplayName("A target that is no longer among the survivors ends the hunt as gone")
    void missingTargetEndsAsGone() {
        HuntState hunt = new HuntState(TARGET, 30_000L);
        SurvivorView someoneElse = new SurvivorView(UUID.randomUUID(), TARGET_POS, 0.7D, false);

        hunt.tick(Contexts.context(0L, new RecordingBody(new Pos(0, 40, 10)), Contexts.route(), new ArrayList<>(), someoneElse));

        assertEquals(HuntEnd.GONE, hunt.end());
    }

    @Test
    @DisplayName("A spared target ends the hunt as gone")
    void sparedTargetEndsTheHunt() {
        HuntState hunt = new HuntState(TARGET, 30_000L);
        SurvivorView spared = new SurvivorView(TARGET, TARGET_POS, 0.7D, false, true, true);

        hunt.tick(Contexts.context(0L, new RecordingBody(new Pos(0, 40, 10)), Contexts.route(), new ArrayList<>(), spared));

        assertEquals(HuntEnd.GONE, hunt.end());
    }
}
