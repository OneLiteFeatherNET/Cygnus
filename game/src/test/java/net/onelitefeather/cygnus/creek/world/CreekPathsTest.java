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

        assertEquals(List.of(new CreekPaths.End(1, true), new CreekPaths.End(2, true)),
                paths.links(new CreekPaths.End(0, false)));
        assertEquals(List.of(new CreekPaths.End(0, false)), paths.links(new CreekPaths.End(1, true)));
        assertTrue(paths.links(new CreekPaths.End(3, true)).isEmpty());
    }

    @Test
    @DisplayName("The two ends of one route never link to each other")
    void ownEndsNeverLink() {
        CreekPaths paths = CreekPaths.of(List.of(route("Short", new Vec(0, 0, 0), new Vec(1, 0, 0))), 3.0D);

        assertTrue(paths.links(new CreekPaths.End(0, true)).isEmpty());
    }

    @Test
    @DisplayName("Points and positions come out as Pos")
    void pointsAndPositions() {
        CreekPaths paths = CreekPaths.of(ROUTES, 3.0D);

        assertEquals(4, paths.size());
        assertEquals(new Pos(12, 0, 0), paths.point(1, 0));
        assertEquals(new Pos(20, 0, 0), paths.position(new CreekPaths.End(1, false)));
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
