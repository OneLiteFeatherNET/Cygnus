package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomPointRouteTest {

    private static final Pos A = new Pos(10, 40, 0);
    private static final Pos B = new Pos(-10, 40, 0);
    private static final Pos NEAR = new Pos(1, 40, 0);

    @Test
    @DisplayName("Only allowed points are picked")
    void onlyAllowedPoints() {
        RandomPointRoute route = new RandomPointRoute(() -> List.of(A, B));
        for (int i = 0; i < 20; i++) {
            assertEquals(B, route.next(Pos.ZERO, point -> point.x() < 0, new Random(i)).orElseThrow());
        }
    }

    @Test
    @DisplayName("A point right next to him is no step at all")
    void skipsPointsNextToHim() {
        RandomPointRoute route = new RandomPointRoute(() -> List.of(NEAR, A));
        for (int i = 0; i < 20; i++) {
            assertEquals(A, route.next(Pos.ZERO, _ -> true, new Random(i)).orElseThrow());
        }
    }

    @Test
    @DisplayName("Nowhere allowed means nowhere to go")
    void nothingAllowed() {
        RandomPointRoute route = new RandomPointRoute(() -> List.of(A, B));
        assertTrue(route.next(Pos.ZERO, _ -> false, new Random(1)).isEmpty());
    }
}
