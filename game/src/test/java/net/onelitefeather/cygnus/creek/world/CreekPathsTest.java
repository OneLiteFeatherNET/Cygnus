package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekLinks;
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
    @DisplayName("Links come from CreekLinks, by route and point")
    void linksByNode() {
        CreekPaths paths = CreekPaths.of(ROUTES, 3.0D);

        assertEquals(List.of(new CreekLinks.Node(1, 0), new CreekLinks.Node(2, 0)),
                paths.links(new CreekLinks.Node(0, 1)));
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
