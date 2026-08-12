package net.onelitefeather.cygnus.blood;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies from which side the blood is thrown across the screen.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class BloodDirectionTest {

    /** A victim in the origin looking towards positive Z, which is a yaw of zero. */
    private static final Pos VICTIM = new Pos(0, 40, 0, 0, 0);

    @Test
    @DisplayName("A hit from straight ahead lands in front")
    void hitFromAheadIsFront() {
        assertEquals(BloodDirection.FRONT, BloodDirection.between(VICTIM, new Pos(0, 40, 6)));
    }

    @Test
    @DisplayName("A hit from behind lands in the back")
    void hitFromBehindIsBack() {
        assertEquals(BloodDirection.BACK, BloodDirection.between(VICTIM, new Pos(0, 40, -6)));
    }

    @Test
    @DisplayName("Looking south, a hit from the east lands on the left")
    void hitFromEastIsLeft() {
        assertEquals(BloodDirection.LEFT, BloodDirection.between(VICTIM, new Pos(6, 40, 0)));
    }

    @Test
    @DisplayName("Looking south, a hit from the west lands on the right")
    void hitFromWestIsRight() {
        assertEquals(BloodDirection.RIGHT, BloodDirection.between(VICTIM, new Pos(-6, 40, 0)));
    }

    @Test
    @DisplayName("The victim's own facing decides, not the world")
    void facingDecides() {
        Pos turned = new Pos(0, 40, 0, 180, 0);
        assertEquals(BloodDirection.BACK, BloodDirection.between(turned, new Pos(0, 40, 6)));
    }

    @Test
    @DisplayName("A hit from the exact same spot still picks a side")
    void hitFromTheSameSpotIsFront() {
        assertEquals(BloodDirection.FRONT, BloodDirection.between(VICTIM, new Pos(0, 40, 0)));
    }
}
