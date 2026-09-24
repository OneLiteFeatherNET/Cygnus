package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpotFinderTest {

    private static final CreekSight SIGHT = new CreekSight(48, 35);
    private static final SpotFinder SPOTS = new SpotFinder(SIGHT, Optional::of);
    private static final Pos TARGET = new Pos(0, 40, 0, 0, 0);
    private static final Pos TARGET_EYES = TARGET.add(0, 1.62, 0);

    @Test
    @DisplayName("A spot beside the view stays in the band and out of the cone")
    void besideStaysOutOfTheCone() {
        RandomGenerator random = new Random(1);
        for (int i = 0; i < 50; i++) {
            Pos spot = SPOTS.beside(TARGET, 20, 35, 40, 70, List.of(TARGET_EYES), 15, random).orElseThrow();
            double distance = spot.distance(TARGET);
            assertTrue(distance >= 20 - 1.0E-6 && distance <= 35 + 1.0E-6, "distance was " + distance);
            assertFalse(SIGHT.inView(TARGET_EYES, spot.add(0, SpotFinder.BODY_CENTRE, 0)));
        }
    }

    @Test
    @DisplayName("A spot straight ahead is not hidden")
    void aheadIsNotHidden() {
        assertFalse(SPOTS.isHidden(new Pos(0, 40, 20), List.of(TARGET_EYES), 15));
    }

    @Test
    @DisplayName("A spot too close is not hidden, even behind")
    void tooCloseIsNotHidden() {
        assertFalse(SPOTS.isHidden(new Pos(0, 40, -5), List.of(TARGET_EYES), 15));
    }

    @Test
    @DisplayName("A spot far behind is hidden")
    void farBehindIsHidden() {
        assertTrue(SPOTS.isHidden(new Pos(0, 40, -20), List.of(TARGET_EYES), 15));
    }

    @Test
    @DisplayName("Without ground there is no spot")
    void noGroundNoSpot() {
        SpotFinder nowhere = new SpotFinder(SIGHT, _ -> Optional.empty());
        assertTrue(nowhere.beside(TARGET, 20, 35, 40, 70, List.of(TARGET_EYES), 15, new Random(1)).isEmpty());
    }
}
