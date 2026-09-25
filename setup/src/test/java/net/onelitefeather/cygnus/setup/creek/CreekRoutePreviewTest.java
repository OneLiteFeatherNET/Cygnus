package net.onelitefeather.cygnus.setup.creek;

import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreekRoutePreviewTest {

    @Test
    @DisplayName("A line has a particle every step, ends excluded")
    void lineSteps() {
        List<Vec> line = CreekRoutePreview.line(new Vec(0, 80, 0), new Vec(2, 80, 0), 0.5D);

        assertEquals(List.of(new Vec(0.5, 80, 0), new Vec(1, 80, 0), new Vec(1.5, 80, 0)), line);
    }

    @Test
    @DisplayName("Only the ends of the edited route are marked, and only if another route's end is close")
    void linkedEndsOfTheEditedRoute() {
        CreekRoute a = CreekRoute.ofPositions("A", List.of(new Vec(0, 80, 0), new Vec(10, 80, 0)));
        CreekRoute b = CreekRoute.ofPositions("B", List.of(new Vec(12, 80, 0), new Vec(20, 80, 0)));
        CreekRoute c = CreekRoute.ofPositions("C", List.of(new Vec(50, 80, 0), new Vec(60, 80, 0)));
        List<CreekRoute> routes = List.of(a, b, c);

        assertEquals(Set.of(new Vec(10, 80, 0)), CreekRoutePreview.linkedEnds(a, routes, 3.0D));
        assertEquals(Set.of(new Vec(12, 80, 0)), CreekRoutePreview.linkedEnds(b, routes, 3.0D));
        assertEquals(Set.of(), CreekRoutePreview.linkedEnds(c, routes, 3.0D));
    }
}
