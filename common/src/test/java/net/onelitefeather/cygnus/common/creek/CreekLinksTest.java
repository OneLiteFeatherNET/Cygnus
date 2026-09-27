package net.onelitefeather.cygnus.common.creek;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekLinksTest {

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
        CreekLinks links = CreekLinks.of(ROUTES, 3.0D);

        assertEquals(List.of(new CreekLinks.Node(1, 0), new CreekLinks.Node(2, 0)),
                links.of(new CreekLinks.Node(0, 1)));
        assertEquals(List.of(new CreekLinks.Node(0, 1)), links.of(new CreekLinks.Node(1, 0)));
        assertTrue(links.of(new CreekLinks.Node(3, 0)).isEmpty());
    }

    @Test
    @DisplayName("An end on the middle of another route is linked to that point, both ways")
    void endOnAMiddleIsLinked() {
        CreekLinks links = CreekLinks.of(List.of(
                route("X", new Vec(0, 0, 0), new Vec(10, 0, 0), new Vec(20, 0, 0)),
                route("Y", new Vec(10, 0, 2), new Vec(10, 0, 20))), 3.0D);

        assertEquals(List.of(new CreekLinks.Node(0, 1)), links.of(new CreekLinks.Node(1, 0)));
        assertEquals(List.of(new CreekLinks.Node(1, 0)), links.of(new CreekLinks.Node(0, 1)));
    }

    @Test
    @DisplayName("Two middles close together are not linked")
    void middlesNeverLink() {
        CreekLinks links = CreekLinks.of(List.of(
                route("X", new Vec(0, 0, 0), new Vec(10, 0, 0), new Vec(20, 0, 0)),
                route("Z", new Vec(-5, 0, 1), new Vec(10, 0, 1), new Vec(25, 0, 1))), 3.0D);

        assertTrue(links.of(new CreekLinks.Node(0, 1)).isEmpty());
        assertTrue(links.of(new CreekLinks.Node(1, 1)).isEmpty());
    }

    @Test
    @DisplayName("An end links to the nearest point of another route only")
    void onlyTheNearestPoint() {
        CreekLinks links = CreekLinks.of(List.of(
                route("X", new Vec(-10, 0, 0), new Vec(1, 0, 0), new Vec(2, 0, 0), new Vec(20, 0, 0)),
                route("W", new Vec(1, 0, 1), new Vec(1, 0, 20))), 3.0D);

        assertEquals(List.of(new CreekLinks.Node(0, 1)), links.of(new CreekLinks.Node(1, 0)));
        assertTrue(links.of(new CreekLinks.Node(0, 2)).isEmpty(), "the second closest point stays unlinked");
    }

    @Test
    @DisplayName("The two ends of one route never link to each other")
    void ownEndsNeverLink() {
        CreekLinks links = CreekLinks.of(List.of(route("Short", new Vec(0, 0, 0), new Vec(1, 0, 0))), 3.0D);

        assertTrue(links.of(new CreekLinks.Node(0, 0)).isEmpty());
    }
}
