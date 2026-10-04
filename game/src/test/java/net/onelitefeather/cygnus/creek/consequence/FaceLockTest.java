package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FaceLockTest {

    private static final Pos EYES = new Pos(0, 41.62, 0);
    private static final double DELTA = 1.0E-3D;

    private static Vec direction(Pos from, Pos to) {
        return to.sub(from).asVec().normalize();
    }

    @Test
    @DisplayName("Fully turned, he looks straight at the target")
    void fullyTurnedLooksAtTheTarget() {
        Pos target = new Pos(3, 43, 4);

        Pos point = FaceLock.lookPoint(EYES, target, 180.0F, 0.0F, 1.0D);

        Vec expected = direction(EYES, target);
        Vec actual = direction(EYES, point);
        assertEquals(expected.x(), actual.x(), DELTA);
        assertEquals(expected.y(), actual.y(), DELTA);
        assertEquals(expected.z(), actual.z(), DELTA);
    }

    @Test
    @DisplayName("Not turned yet, he keeps looking where he looked")
    void notTurnedKeepsTheView() {
        Pos point = FaceLock.lookPoint(EYES, new Pos(3, 43, 4), 90.0F, 20.0F, 0.0D);

        Vec expected = EYES.withView(90.0F, 20.0F).direction();
        Vec actual = direction(EYES, point);
        assertEquals(expected.x(), actual.x(), DELTA);
        assertEquals(expected.y(), actual.y(), DELTA);
        assertEquals(expected.z(), actual.z(), DELTA);
    }

    @Test
    @DisplayName("Half turned, he takes the short way round")
    void takesTheShortWayRound() {
        // Straight towards negative X is a yaw of 90. Starting at a yaw of 170, the short way
        // passes 130, not 300.
        Pos target = EYES.add(-10, 0, 0);

        Pos point = FaceLock.lookPoint(EYES, target, 170.0F, 0.0F, 0.5D);

        Vec expected = EYES.withView(130.0F, 0.0F).direction();
        Vec actual = direction(EYES, point);
        assertEquals(expected.x(), actual.x(), DELTA);
        assertEquals(expected.z(), actual.z(), DELTA);
    }
}
