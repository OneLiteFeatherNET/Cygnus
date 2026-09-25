package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.world.RouteProvider;
import net.onelitefeather.cygnus.creek.world.RouteStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderStateTest {

    private static final UUID FIRST = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();
    private static final Pos A = new Pos(30, 40, 0);
    private static final Pos B = new Pos(-30, 40, 0);

    private static SurvivorView far(UUID id, double dread, boolean sees) {
        return new SurvivorView(id, new Pos(0, 40, -60, 0, 0), dread, sees);
    }

    @Test
    @DisplayName("Entering shows him to every survivor")
    void enterShowsHimToEveryone() {
        RecordingBody body = new RecordingBody(Pos.ZERO);
        new WanderState(0L).enter(Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(),
                far(FIRST, 0.0D, false), new SurvivorView(SECOND, new Pos(40, 40, 40), 0.0D, false)));

        assertEquals(Set.of(FIRST, SECOND), body.viewers);
    }

    @Test
    @DisplayName("He walks to the next point")
    void walksToTheNextPoint() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(Long.MAX_VALUE);
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.0D, false));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
        assertEquals(A, body.goal);
        assertEquals(Contexts.CONFIG.wanderSpeed(), body.speed);
    }

    @Test
    @DisplayName("Spotted, he stops and looks back for a moment")
    void pausesWhenSpotted() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(Long.MAX_VALUE);
        SurvivorView watcher = far(FIRST, 0.0D, true);
        state.enter(Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), watcher));

        state.tick(Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), watcher));
        assertNull(body.goal);
        assertEquals(watcher.eyes(), body.lookedAt);

        state.tick(Contexts.context(1499L, body, Contexts.route(A), new ArrayList<>(), watcher));
        assertNull(body.goal);

        state.tick(Contexts.context(1500L, body, Contexts.route(A), new ArrayList<>(), watcher));
        assertEquals(A, body.goal);
    }

    @Test
    @DisplayName("A survivor walking up to him makes him vanish")
    void vanishesWhenApproached() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(Long.MAX_VALUE);
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(),
                new SurvivorView(FIRST, new Pos(0, 40, 10), 0.0D, false));
        state.enter(ctx);

        assertInstanceOf(VanishState.class, state.tick(ctx));
    }

    @Test
    @DisplayName("Making no progress, he picks another point")
    void picksAnotherPointWhenStuck() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        AtomicInteger calls = new AtomicInteger();
        RouteProvider cycling = (_, _, _) -> Optional.of(new RouteStep(calls.getAndIncrement() % 2 == 0 ? A : B, 0));
        WanderState state = new WanderState(Long.MAX_VALUE);
        state.enter(Contexts.context(0L, body, cycling, new ArrayList<>(), far(FIRST, 0.0D, false)));

        state.tick(Contexts.context(0L, body, cycling, new ArrayList<>(), far(FIRST, 0.0D, false)));
        assertEquals(A, body.goal);

        state.tick(Contexts.context(2999L, body, cycling, new ArrayList<>(), far(FIRST, 0.0D, false)));
        assertEquals(A, body.goal);

        state.tick(Contexts.context(3000L, body, cycling, new ArrayList<>(), far(FIRST, 0.0D, false)));
        assertEquals(B, body.goal);
    }

    @Test
    @DisplayName("With nowhere to go he stands still")
    void standsStillWithoutAPoint() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(Long.MAX_VALUE);
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(), new ArrayList<>(), far(FIRST, 0.0D, false));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
        assertNull(body.goal);
        assertTrue(body.stops > 0);
    }

    @Test
    @DisplayName("Above the stalk threshold he starts stalking")
    void startsStalking() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(0L);
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.3D, false));
        state.enter(ctx);

        CreekState next = state.tick(ctx);

        assertInstanceOf(StalkState.class, next);
        assertEquals(FIRST, ((StalkState) next).target());
        assertEquals(1, body.teleports.size());
    }

    @Test
    @DisplayName("Before the cooldown he keeps wandering")
    void waitsForTheCooldown() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(5000L);
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.3D, false));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
    }

    @Test
    @DisplayName("Calm survivors are left alone")
    void leavesCalmSurvivorsAlone() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(0L);
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.1D, false));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
    }

    @Test
    @DisplayName("On a tie he picks the one who strayed from the group")
    void prefersTheLonelyOne() {
        UUID third = UUID.randomUUID();
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(0L);
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(),
                new SurvivorView(FIRST, new Pos(0, 40, -60, 0, 0), 0.3D, false),
                new SurvivorView(SECOND, new Pos(5, 40, -60, 0, 0), 0.3D, false),
                new SurvivorView(third, new Pos(0, 40, -160, 0, 0), 0.3D, false));
        state.enter(ctx);

        assertEquals(third, ((StalkState) state.tick(ctx)).target());
    }

    @Test
    @DisplayName("Someone who leaves the survivors stops seeing him while he wanders")
    void dropsViewersWhoLeaveTheSurvivors() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        WanderState state = new WanderState(Long.MAX_VALUE);
        SurvivorView second = new SurvivorView(SECOND, new Pos(40, 40, 40), 0.0D, false);
        state.enter(Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.0D, false), second));

        state.tick(Contexts.context(100L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.0D, false)));

        assertEquals(Set.of(FIRST), body.viewers, "a survivor turned slender or spectator must lose sight of him");
    }

    /** Hands out A, then B, then A again, each with the given pause. */
    private static RouteProvider alternating(int pauseMillis) {
        AtomicInteger calls = new AtomicInteger();
        return (_, _, _) -> Optional.of(new RouteStep(calls.getAndIncrement() % 2 == 0 ? A : B, pauseMillis));
    }

    private static CreekContext at(long now, RecordingBody body, RouteProvider route, CreekConfig config,
                                   SurvivorView... survivors) {
        return Contexts.context(now, body, route, config,
                survivors.length == 0 ? new SurvivorView[]{far(FIRST, 0.0D, false)} : survivors);
    }

    @Test
    @DisplayName("Arriving at a point with a pause, it rests there")
    void restsAtAPointWithAPause() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        RouteProvider route = alternating(2000);
        WanderState state = new WanderState(Long.MAX_VALUE);
        state.enter(at(0L, body, route, Contexts.CONFIG));
        state.tick(at(0L, body, route, Contexts.CONFIG));
        assertEquals(A, body.goal);

        body.position = A;
        state.tick(at(100L, body, route, Contexts.CONFIG));
        assertNull(body.goal);
        state.tick(at(2099L, body, route, Contexts.CONFIG));
        assertNull(body.goal);

        state.tick(at(2100L, body, route, Contexts.CONFIG));
        assertEquals(B, body.goal);
    }

    @Test
    @DisplayName("Getting stuck is no reason to rest")
    void noRestWhenStuck() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        RouteProvider route = alternating(5000);
        WanderState state = new WanderState(Long.MAX_VALUE);
        state.enter(at(0L, body, route, Contexts.CONFIG));
        state.tick(at(0L, body, route, Contexts.CONFIG));

        state.tick(at(3000L, body, route, Contexts.CONFIG));

        assertEquals(B, body.goal);
    }

    @Test
    @DisplayName("A random stop holds it at a point without a pause")
    void randomStop() {
        CreekConfig config = Contexts.randomStops(1.0D, 1000, 1000);
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        RouteProvider route = alternating(0);
        WanderState state = new WanderState(Long.MAX_VALUE);
        state.enter(at(0L, body, route, config));
        state.tick(at(0L, body, route, config));

        body.position = A;
        state.tick(at(100L, body, route, config));
        state.tick(at(1099L, body, route, config));
        assertNull(body.goal);

        state.tick(at(1100L, body, route, config));
        assertEquals(B, body.goal);
    }

    @Test
    @DisplayName("Pause and random stop do not add up, the longer one wins")
    void longerHaltWins() {
        CreekConfig config = Contexts.randomStops(1.0D, 1000, 1000);
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        RouteProvider route = alternating(3000);
        WanderState state = new WanderState(Long.MAX_VALUE);
        state.enter(at(0L, body, route, config));
        state.tick(at(0L, body, route, config));

        body.position = A;
        state.tick(at(100L, body, route, config));
        state.tick(at(3099L, body, route, config));
        assertNull(body.goal);

        state.tick(at(3100L, body, route, config));
        assertEquals(B, body.goal);
    }

    @Test
    @DisplayName("Without a chance and without a pause it walks on right away")
    void noHaltWithoutChanceOrPause() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        RouteProvider route = alternating(0);
        WanderState state = new WanderState(Long.MAX_VALUE);
        state.enter(at(0L, body, route, Contexts.CONFIG));
        state.tick(at(0L, body, route, Contexts.CONFIG));

        body.position = A;
        state.tick(at(100L, body, route, Contexts.CONFIG));

        assertEquals(B, body.goal);
    }

    @Test
    @DisplayName("Resting does not keep it from stalking")
    void stalksWhileResting() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        RouteProvider route = alternating(5000);
        WanderState state = new WanderState(1000L);
        state.enter(at(0L, body, route, Contexts.CONFIG));
        state.tick(at(0L, body, route, Contexts.CONFIG));
        body.position = A;
        state.tick(at(100L, body, route, Contexts.CONFIG));

        CreekState next = state.tick(at(1000L, body, route, Contexts.CONFIG, far(FIRST, 0.3D, false)));

        assertInstanceOf(StalkState.class, next);
    }
}
