package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekPathsTest {

    private static CreekRoute route(String name, Vec... points) {
        return CreekRoute.ofPositions(name, List.of(points));
    }

    /** A ends at (10,0,0); B starts 2 blocks away, C exactly 3, D 3.1. */
    private static final List<CreekRoute> ROUTES = List.of(
            route("A", new Vec(0, 0, 0), new Vec(10, 0, 0)),
            route("B", new Vec(12, 0, 0), new Vec(20, 0, 0)),
            route("C", new Vec(10, 0, 3), new Vec(10, 0, 20)),
            route("D", new Vec(10, 0, -3.1), new Vec(10, 0, -20))
    );

    @Test
    @DisplayName("Ends within the link distance are linked, the one just outside is not")
    void linksFollowTheDistance() {
        CreekPaths paths = CreekPaths.of(ROUTES, 3.0D);

        assertEquals(List.of(new CreekPaths.Node(1, 0), new CreekPaths.Node(2, 0)),
                paths.links(new CreekPaths.Node(0, 1)));
        assertEquals(List.of(new CreekPaths.Node(0, 1)), paths.links(new CreekPaths.Node(1, 0)));
        assertTrue(paths.links(new CreekPaths.Node(3, 0)).isEmpty());
    }

    @Test
    @DisplayName("An end on the middle of another route is linked to that point, both ways")
    void endOnAMiddleIsLinked() {
        CreekPaths paths = CreekPaths.of(List.of(
                route("X", new Vec(0, 0, 0), new Vec(10, 0, 0), new Vec(20, 0, 0)),
                route("Y", new Vec(10, 0, 2), new Vec(10, 0, 20))), 3.0D);

        assertEquals(List.of(new CreekPaths.Node(0, 1)), paths.links(new CreekPaths.Node(1, 0)));
        assertEquals(List.of(new CreekPaths.Node(1, 0)), paths.links(new CreekPaths.Node(0, 1)));
    }

    @Test
    @DisplayName("Two middles close together are not linked")
    void middlesNeverLink() {
        CreekPaths paths = CreekPaths.of(List.of(
                route("X", new Vec(0, 0, 0), new Vec(10, 0, 0), new Vec(20, 0, 0)),
                route("Z", new Vec(-5, 0, 1), new Vec(10, 0, 1), new Vec(25, 0, 1))), 3.0D);

        assertTrue(paths.links(new CreekPaths.Node(0, 1)).isEmpty());
        assertTrue(paths.links(new CreekPaths.Node(1, 1)).isEmpty());
    }

    @Test
    @DisplayName("An end links to the nearest point of another route only")
    void onlyTheNearestPoint() {
        CreekPaths paths = CreekPaths.of(List.of(
                route("X", new Vec(-10, 0, 0), new Vec(1, 0, 0), new Vec(2, 0, 0), new Vec(20, 0, 0)),
                route("W", new Vec(1, 0, 1), new Vec(1, 0, 20))), 3.0D);

        assertEquals(List.of(new CreekPaths.Node(0, 1)), paths.links(new CreekPaths.Node(1, 0)));
        assertTrue(paths.links(new CreekPaths.Node(0, 2)).isEmpty(), "the second closest point stays unlinked");
    }

    @Test
    @DisplayName("The two ends of one route never link to each other")
    void ownEndsNeverLink() {
        CreekPaths paths = CreekPaths.of(List.of(route("Short", new Vec(0, 0, 0), new Vec(1, 0, 0))), 3.0D);

        assertTrue(paths.links(new CreekPaths.Node(0, 0)).isEmpty());
    }

    @Test
    @DisplayName("Points come out as Pos")
    void pointsAsPos() {
        CreekPaths paths = CreekPaths.of(ROUTES, 3.0D);

        assertEquals(4, paths.size());
        assertEquals(new Pos(12, 0, 0), paths.point(1, 0));
        assertEquals(new Pos(20, 0, 0), paths.point(1, 1));
        assertEquals(8, paths.allPoints().size());
        assertTrue(CreekPaths.of(List.of(), 3.0D).isEmpty());
    }

    @Test
    @DisplayName("Each point reports its pause")
    void pausesPerPoint() {
        CreekRoute route = new CreekRoute("P", List.of(
                CreekWaypoint.of(new Vec(0, 0, 0)).withPause(1000),
                CreekWaypoint.of(new Vec(10, 0, 0))));
        CreekPaths paths = CreekPaths.of(List.of(route), 3.0D);

        assertEquals(1000, paths.pauseMillis(0, 0));
        assertEquals(0, paths.pauseMillis(0, 1));
    }
}
