package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.creek.world.CreekPaths;
import net.onelitefeather.cygnus.creek.world.PathRoute;
import net.onelitefeather.cygnus.creek.world.RouteProvider;
import net.onelitefeather.cygnus.creek.world.RouteStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatrolStateTest {

    private static final UUID FIRST = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();
    private static final Pos A = new Pos(30, 40, 0);
    private static final Pos B = new Pos(-30, 40, 0);

    private static SurvivorView far(UUID id, double dread, boolean sees) {
        return new SurvivorView(id, new Pos(0, 40, -60, 0, 0), dread, sees);
    }

    private static SurvivorView at(UUID id, double z) {
        return new SurvivorView(id, new Pos(0, 40, z), 0.0D, false);
    }

    // ---- walking, carried over from the wander behaviour ----

    @Test
    @DisplayName("Entering shows him to every survivor")
    void enterShowsHimToEveryone() {
        RecordingBody body = new RecordingBody(Pos.ZERO);
        new PatrolState().enter(Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(),
                far(FIRST, 0.0D, false), new SurvivorView(SECOND, new Pos(40, 40, 40), 0.0D, false)));

        assertEquals(Set.of(FIRST, SECOND), body.viewers);
    }

    @Test
    @DisplayName("He walks to the next point")
    void walksToTheNextPoint() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.0D, false));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
        assertEquals(A, body.goal);
        assertEquals(Contexts.CONFIG.wanderSpeed(), body.speed);
    }

    @Test
    @DisplayName("Spotted from afar, he stops and looks back for a moment")
    void pausesWhenSpotted() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
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
    @DisplayName("A survivor close by does not make him vanish")
    void doesNotVanishWhenApproached() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), at(FIRST, 10));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
        assertEquals(A, body.goal);
    }

    @Test
    @DisplayName("A high dread does not make him stalk")
    void neverStalks() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.9D, false));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
        assertTrue(body.teleports.isEmpty());
    }

    @Test
    @DisplayName("Making no progress, he picks another point")
    void picksAnotherPointWhenStuck() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        AtomicInteger calls = new AtomicInteger();
        RouteProvider cycling = (_, _, _) -> Optional.of(new RouteStep(calls.getAndIncrement() % 2 == 0 ? A : B, 0));
        PatrolState state = new PatrolState();
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
        PatrolState state = new PatrolState();
        CreekContext ctx = Contexts.context(0L, body, Contexts.route(), new ArrayList<>(), far(FIRST, 0.0D, false));
        state.enter(ctx);

        assertSame(state, state.tick(ctx));
        assertNull(body.goal);
        assertTrue(body.stops > 0);
    }

    @Test
    @DisplayName("Someone who leaves the survivors stops seeing him")
    void dropsViewersWhoLeaveTheSurvivors() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        SurvivorView second = new SurvivorView(SECOND, new Pos(40, 40, 40), 0.0D, false);
        state.enter(Contexts.context(0L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.0D, false), second));

        state.tick(Contexts.context(100L, body, Contexts.route(A), new ArrayList<>(), far(FIRST, 0.0D, false)));

        assertEquals(Set.of(FIRST), body.viewers);
    }

    /** Hands out A, then B, then A again, each with the given pause. */
    private static RouteProvider alternating(int pauseMillis) {
        AtomicInteger calls = new AtomicInteger();
        return (_, _, _) -> Optional.of(new RouteStep(calls.getAndIncrement() % 2 == 0 ? A : B, pauseMillis));
    }

    private static CreekContext at(long now, RecordingBody body, RouteProvider route, CreekConfig config) {
        return Contexts.context(now, body, route, config, far(FIRST, 0.0D, false));
    }

    @Test
    @DisplayName("Arriving at a point with a pause, it rests there")
    void restsAtAPointWithAPause() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        RouteProvider route = alternating(2000);
        PatrolState state = new PatrolState();
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
        PatrolState state = new PatrolState();
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
        PatrolState state = new PatrolState();
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
        PatrolState state = new PatrolState();
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
        PatrolState state = new PatrolState();
        state.enter(at(0L, body, route, Contexts.CONFIG));
        state.tick(at(0L, body, route, Contexts.CONFIG));

        body.position = A;
        state.tick(at(100L, body, route, Contexts.CONFIG));

        assertEquals(B, body.goal);
    }

    // ---- selecting ----

    @Test
    @DisplayName("Within the radius he selects the nearest survivor and stares at them")
    void selectsTheNearestWithinTheRadius() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        List<UUID> selected = new ArrayList<>();
        SurvivorView first = at(FIRST, 3);
        SurvivorView second = at(SECOND, 2);
        state.enter(Contexts.selecting(0L, body, Contexts.route(A), selected, first, second));

        state.tick(Contexts.selecting(0L, body, Contexts.route(A), selected, first, second));

        assertEquals(Optional.of(SECOND), state.staring());
        assertEquals(second.eyes(), body.lookedAt);
        assertNull(body.goal);
        assertTrue(selected.isEmpty(), "the consequence waits for the end of the stare");
    }

    @Test
    @DisplayName("Nobody within the radius, nobody is selected")
    void selectsNobodyOutsideTheRadius() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        List<UUID> selected = new ArrayList<>();
        state.enter(Contexts.selecting(0L, body, Contexts.route(A), selected, at(FIRST, 5)));

        state.tick(Contexts.selecting(0L, body, Contexts.route(A), selected, at(FIRST, 5)));

        assertEquals(Optional.empty(), state.staring());
        assertEquals(A, body.goal);
    }

    @Test
    @DisplayName("After staring for a second he applies the consequence and walks on")
    void staresThenAppliesAndWalksOn() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        List<UUID> selected = new ArrayList<>();
        state.enter(Contexts.selecting(0L, body, Contexts.route(A), selected, at(SECOND, 2)));
        state.tick(Contexts.selecting(0L, body, Contexts.route(A), selected, at(SECOND, 2)));

        state.tick(Contexts.selecting(999L, body, Contexts.route(A), selected, at(SECOND, 2)));
        assertTrue(selected.isEmpty());
        assertNull(body.goal);

        state.tick(Contexts.selecting(1000L, body, Contexts.route(A), selected, at(SECOND, 2)));
        assertEquals(List.of(SECOND), selected);
        assertEquals(A, body.goal);
        assertEquals(Optional.empty(), state.staring());
        assertEquals(11_000L, state.selectAllowedAt());
    }

    @Test
    @DisplayName("During the cooldown he selects nobody")
    void selectsNobodyDuringTheCooldown() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        List<UUID> selected = new ArrayList<>();
        state.enter(Contexts.selecting(0L, body, Contexts.route(A), selected, at(SECOND, 2)));
        state.tick(Contexts.selecting(0L, body, Contexts.route(A), selected, at(SECOND, 2)));
        state.tick(Contexts.selecting(1000L, body, Contexts.route(A), selected, at(SECOND, 2)));

        state.tick(Contexts.selecting(10_999L, body, Contexts.route(A), selected, at(SECOND, 2)));
        assertEquals(Optional.empty(), state.staring());
        assertEquals(A, body.goal);

        state.tick(Contexts.selecting(11_000L, body, Contexts.route(A), selected, at(SECOND, 2)));
        assertEquals(Optional.of(SECOND), state.staring());
    }

    @Test
    @DisplayName("If the selected survivor leaves during the stare, nothing happens and no cooldown starts")
    void dropsTheSelectionWhenTheSurvivorLeaves() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        List<UUID> selected = new ArrayList<>();
        state.enter(Contexts.selecting(0L, body, Contexts.route(A), selected, at(SECOND, 2), far(FIRST, 0.0D, false)));
        state.tick(Contexts.selecting(0L, body, Contexts.route(A), selected, at(SECOND, 2), far(FIRST, 0.0D, false)));

        state.tick(Contexts.selecting(500L, body, Contexts.route(A), selected, far(FIRST, 0.0D, false)));
        assertTrue(selected.isEmpty());
        assertEquals(Optional.empty(), state.staring());

        state.tick(Contexts.selecting(600L, body, Contexts.route(A), selected, at(SECOND, 2), far(FIRST, 0.0D, false)));
        assertEquals(Optional.of(SECOND), state.staring());
    }

    @Test
    @DisplayName("After looking back he walks on to the point he was heading for")
    void keepsHisPointAfterAPause() {
        PathRoute route = new PathRoute(CreekPaths.of(List.of(CreekRoute.ofPositions("R",
                List.of(new Vec(0, 40, 0), new Vec(20, 40, 0), new Vec(40, 40, 0)))), 3.0D));
        RecordingBody body = new RecordingBody(new Pos(0, 40, 0));
        PatrolState state = new PatrolState();
        SurvivorView calm = far(FIRST, 0.0D, false);
        state.enter(Contexts.context(0L, body, route, new ArrayList<>(), calm));
        state.tick(Contexts.context(0L, body, route, new ArrayList<>(), calm));
        state.tick(Contexts.context(100L, body, route, new ArrayList<>(), calm));
        assertEquals(new Pos(20, 40, 0), body.goal);

        body.position = new Pos(10, 40, 0);
        SurvivorView watcher = far(FIRST, 0.0D, true);
        state.tick(Contexts.context(200L, body, route, new ArrayList<>(), watcher));
        assertNull(body.goal, "he stops to look back");

        state.tick(Contexts.context(1700L, body, route, new ArrayList<>(), watcher));
        assertEquals(new Pos(20, 40, 0), body.goal, "he goes on to the point he was heading for");
    }
}
