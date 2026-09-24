package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekSightTest {

    /** Eyes of a survivor standing at the origin and looking towards positive Z. */
    private static final Pos EYES = new Pos(0, 41.62, 0, 0, 0);
    private static final CreekSight SIGHT = new CreekSight(48, 35);

    @Test
    @DisplayName("Straight ahead he is seen")
    void straightAheadIsSeen() {
        assertTrue(SIGHT.inView(EYES, new Vec(0, 41.62, 20)));
    }

    @Test
    @DisplayName("Behind the survivor he is not")
    void behindIsNotSeen() {
        assertFalse(SIGHT.inView(EYES, new Vec(0, 41.62, -20)));
    }

    @Test
    @DisplayName("Beyond the range he is not")
    void beyondRangeIsNotSeen() {
        assertFalse(SIGHT.inView(EYES, new Vec(0, 41.62, 60)));
    }

    @Test
    @DisplayName("Beside the cone he is not")
    void besideTheConeIsNotSeen() {
        double radians = Math.toRadians(45);
        assertFalse(SIGHT.inView(EYES, new Vec(-Math.sin(radians) * 10, 41.62, Math.cos(radians) * 10)));
    }
}
