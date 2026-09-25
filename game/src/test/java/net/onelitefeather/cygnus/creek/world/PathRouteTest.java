package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathRouteTest {

    private static final Predicate<Pos> ANYWHERE = _ -> true;

    private static final CreekRoute A = new CreekRoute("A",
            List.of(new Vec(0, 40, 0), new Vec(10, 40, 0), new Vec(20, 40, 0)));
    /** Starts 2 blocks after the end of A, so the two are linked. */
    private static final CreekRoute B = new CreekRoute("B",
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

    private static Pos next(PathRoute route, Pos current, RandomGenerator random) {
        return route.next(current, ANYWHERE, random).orElseThrow();
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

        Optional<Pos> next = route.next(at, point -> point.x() < 15, fixed(0));

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
}
