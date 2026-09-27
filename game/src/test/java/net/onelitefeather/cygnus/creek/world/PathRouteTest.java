package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathRouteTest {

    private static final Predicate<Pos> ANYWHERE = _ -> true;

    private static final CreekRoute A = CreekRoute.ofPositions("A",
            List.of(new Vec(0, 40, 0), new Vec(10, 40, 0), new Vec(20, 40, 0)));
    /** Starts 2 blocks after the end of A, so the two are linked. */
    private static final CreekRoute B = CreekRoute.ofPositions("B",
            List.of(new Vec(22, 40, 0), new Vec(22, 40, 10)));

    private static PathRoute route(CreekRoute... routes) {
        return new PathRoute(CreekPaths.of(List.of(routes), 3.0D));
    }

    /** Always answers with the given index (capped to the bound) and forwards for booleans. */
    private static RandomGenerator fixed(int index) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(index, bound - 1);
            }

            @Override
            public boolean nextBoolean() {
                return true;
            }
        };
    }

    private static RouteStep step(PathRoute route, Pos current, RandomGenerator random) {
        return route.next(current, ANYWHERE, random).orElseThrow();
    }

    private static Pos next(PathRoute route, Pos current, RandomGenerator random) {
        return step(route, current, random).target();
    }

    private static CreekRoute paused(String name, int startPause, int endPause, Vec... positions) {
        List<CreekWaypoint> points = new ArrayList<>(CreekRoute.ofPositions(name, List.of(positions)).points());
        points.set(0, points.getFirst().withPause(startPause));
        points.set(points.size() - 1, points.getLast().withPause(endPause));
        return new CreekRoute(name, points);
    }

    @Test
    @DisplayName("Without a position it joins at the nearest point")
    void joinsAtTheNearestPoint() {
        PathRoute route = route(A);

        assertEquals(new Pos(10, 40, 0), next(route, new Pos(9, 41, 1), fixed(0)));
        assertEquals("A 2/3 →", route.describe());
    }

    @Test
    @DisplayName("It walks the points in order")
    void walksInOrder() {
        PathRoute route = route(A);
        Pos at = next(route, new Pos(0, 40, 0), fixed(0));

        at = next(route, at, fixed(0));
        assertEquals(new Pos(10, 40, 0), at);
        at = next(route, at, fixed(0));
        assertEquals(new Pos(20, 40, 0), at);
    }

    @Test
    @DisplayName("At an end without links it turns around")
    void turnsAroundWithoutLinks() {
        PathRoute route = route(A);
        Pos at = next(route, new Pos(0, 40, 0), fixed(0));
        at = next(route, at, fixed(0));
        at = next(route, at, fixed(0));

        assertEquals(new Pos(10, 40, 0), next(route, at, fixed(0)));
        assertEquals("A 2/3 ←", route.describe());
    }

    @Test
    @DisplayName("At a linked end it can switch to the other route")
    void takesALink() {
        PathRoute route = route(A, B);
        Pos at = next(route, new Pos(10, 40, 0), fixed(0));
        at = next(route, at, fixed(0));

        at = next(route, at, fixed(0));
        assertEquals(new Pos(22, 40, 0), at, "the linked end is the stepping stone");
        assertEquals(new Pos(22, 40, 10), next(route, at, fixed(0)));
    }

    @Test
    @DisplayName("At a linked end it can also turn around")
    void canStillTurnAround() {
        PathRoute route = route(A, B);
        Pos at = next(route, new Pos(10, 40, 0), fixed(0));
        at = next(route, at, fixed(0));

        assertEquals(new Pos(10, 40, 0), next(route, at, fixed(1)));
    }

    @Test
    @DisplayName("After a teleport it rejoins where it is now")
    void rejoinsAfterATeleport() {
        PathRoute route = route(A, B);
        next(route, new Pos(0, 40, 0), fixed(0));

        assertEquals(new Pos(22, 40, 10), next(route, new Pos(22, 41, 12), fixed(0)));
    }

    @Test
    @DisplayName("A blocked point makes it turn around")
    void turnsAroundWhenBlocked() {
        PathRoute route = route(A);
        Pos at = next(route, new Pos(10, 40, 0), fixed(0));

        Optional<Pos> next = route.next(at, point -> point.x() < 15, fixed(0)).map(RouteStep::target);

        assertEquals(Optional.of(new Pos(0, 40, 0)), next);
    }

    @Test
    @DisplayName("With both ways blocked it stays put, and moves on once a way is free")
    void waitsUntilAWayIsFree() {
        PathRoute route = route(A);
        Pos at = next(route, new Pos(10, 40, 0), fixed(0));

        assertTrue(route.next(at, point -> point.x() == 10, fixed(0)).isEmpty());
        assertEquals(new Pos(20, 40, 0), next(route, at, fixed(0)));
    }

    @Test
    @DisplayName("With no point allowed at all it waits, then joins")
    void joinsOnceAPointIsAllowed() {
        PathRoute route = route(A);

        assertTrue(route.next(new Pos(0, 40, 0), _ -> false, fixed(0)).isEmpty());
        assertEquals(new Pos(0, 40, 0), next(route, new Pos(0, 40, 0), fixed(0)));
    }

    @Test
    @DisplayName("Before it joined there is nothing to describe")
    void emptyDescriptionBeforeJoining() {
        assertEquals("", route(A).describe());
        assertEquals(5, route(A, B).points().size());
    }

    @Test
    @DisplayName("Reaching an end in walking direction carries its pause")
    void endsInWalkingDirectionPause() {
        PathRoute route = route(paused("A", 1000, 2000, new Vec(0, 40, 0), new Vec(10, 40, 0), new Vec(20, 40, 0)));

        assertEquals(0, step(route, new Pos(0, 40, 0), fixed(0)).pauseMillis(), "it walks away from the start");
        assertEquals(0, step(route, new Pos(0, 40, 0), fixed(0)).pauseMillis());
        RouteStep end = step(route, new Pos(10, 40, 0), fixed(0));
        assertEquals(new RouteStep(new Pos(20, 40, 0), 2000, true), end);
        assertEquals(0, step(route, end.target(), fixed(0)).pauseMillis());
        assertEquals(new RouteStep(new Pos(0, 40, 0), 1000, true), step(route, new Pos(10, 40, 0), fixed(0)));
    }

    @Test
    @DisplayName("Heading for an end without links is a dead end, heading anywhere else is not")
    void marksDeadEnds() {
        PathRoute route = route(A);

        assertFalse(step(route, new Pos(0, 40, 0), fixed(0)).deadEnd(), "joining at the start walks away from it");
        assertFalse(step(route, new Pos(0, 40, 0), fixed(0)).deadEnd(), "the middle is no end");
        assertTrue(step(route, new Pos(10, 40, 0), fixed(0)).deadEnd());
        assertFalse(step(route, new Pos(20, 40, 0), fixed(0)).deadEnd(), "turning around leaves the end");
        assertTrue(step(route, new Pos(10, 40, 0), fixed(0)).deadEnd(), "the start is a dead end too");
    }

    @Test
    @DisplayName("A linked end is no dead end")
    void linkedEndIsNoDeadEnd() {
        PathRoute route = route(A, B);
        next(route, new Pos(10, 40, 0), fixed(0));

        assertFalse(step(route, new Pos(10, 40, 0), fixed(0)).deadEnd());
    }

    /** X runs along the x axis; Y starts right next to X's middle point and runs off along z. */
    private static final CreekRoute X = CreekRoute.ofPositions("X",
            List.of(new Vec(0, 40, 0), new Vec(10, 40, 0), new Vec(20, 40, 0)));
    private static final CreekRoute Y = CreekRoute.ofPositions("Y",
            List.of(new Vec(10, 40, 2), new Vec(10, 40, 12), new Vec(10, 40, 22)));

    @Test
    @DisplayName("At a crossing in the middle it can turn onto the other route")
    void turnsAtACrossing() {
        PathRoute route = route(X, Y);
        Pos at = next(route, new Pos(0, 40, 0), fixed(0));
        at = next(route, at, fixed(0));
        assertEquals(new Pos(10, 40, 0), at);

        RouteStep turn = step(route, at, fixed(0));
        assertEquals(new RouteStep(new Pos(10, 40, 2), 0), turn, "the other route's end is the stepping stone");
        assertEquals(new Pos(10, 40, 12), next(route, turn.target(), fixed(0)));
    }

    @Test
    @DisplayName("At a crossing in the middle it can also walk on")
    void walksOnAtACrossing() {
        PathRoute route = route(X, Y);
        Pos at = next(route, new Pos(0, 40, 0), fixed(0));
        at = next(route, at, fixed(0));

        assertEquals(new Pos(20, 40, 0), next(route, at, fixed(1)));
    }

    @Test
    @DisplayName("Right after taking a link it does not jump straight back")
    void noJumpBackAfterALink() {
        PathRoute route = route(X, Y);
        Pos at = next(route, new Pos(0, 40, 0), fixed(0));
        at = next(route, at, fixed(0));
        at = next(route, at, fixed(0));
        assertEquals(new Pos(10, 40, 2), at);

        assertEquals(new Pos(10, 40, 12), next(route, at, fixed(0)));
    }

    @Test
    @DisplayName("From an end it can join the middle of another route and walk on from there")
    void joinsAMiddleFromAnEnd() {
        PathRoute route = route(X, Y);
        next(route, new Pos(10, 40, 22), fixed(0));
        Pos at = next(route, new Pos(10, 40, 22), fixed(0));
        RouteStep start = step(route, at, fixed(0));
        assertEquals(new Pos(10, 40, 2), start.target());
        assertFalse(start.deadEnd(), "an end on another route's middle is no dead end");

        RouteStep join = step(route, start.target(), fixed(0));
        assertEquals(new RouteStep(new Pos(10, 40, 0), 0), join, "no second stop where the routes meet");
        assertEquals(new Pos(20, 40, 0), next(route, join.target(), fixed(0)), "it walks on, not back");
    }

    @Test
    @DisplayName("The start of a linked route is no second stop")
    void noPauseAtTheSteppingStone() {
        PathRoute route = route(
                paused("A", 0, 2000, new Vec(0, 40, 0), new Vec(10, 40, 0), new Vec(20, 40, 0)),
                paused("B", 3000, 0, new Vec(22, 40, 0), new Vec(22, 40, 10)));
        next(route, new Pos(10, 40, 0), fixed(0));

        assertEquals(2000, step(route, new Pos(10, 40, 0), fixed(0)).pauseMillis());
        assertEquals(new RouteStep(new Pos(22, 40, 0), 0), step(route, new Pos(20, 40, 0), fixed(0)));
    }

    @Test
    @DisplayName("Rejoining at an end does not stop there")
    void noPauseWhenRejoiningAtAnEnd() {
        PathRoute route = route(paused("A", 1000, 2000, new Vec(0, 40, 0), new Vec(10, 40, 0), new Vec(20, 40, 0)));

        assertEquals(new RouteStep(new Pos(20, 40, 0), 0), step(route, new Pos(21, 40, 0), fixed(0)));
    }

    @Test
    @DisplayName("A point in the middle keeps its pause")
    void middlePointKeepsItsPause() {
        PathRoute route = route(new CreekRoute("M", List.of(
                CreekWaypoint.of(new Vec(0, 40, 0)),
                CreekWaypoint.of(new Vec(10, 40, 0)).withPause(1500),
                CreekWaypoint.of(new Vec(20, 40, 0)))));
        next(route, new Pos(0, 40, 0), fixed(0));

        assertEquals(new RouteStep(new Pos(10, 40, 0), 1500), step(route, new Pos(0, 40, 0), fixed(0)));
    }
}
