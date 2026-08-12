package net.onelitefeather.cygnus.blood;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

/**
 * The side of the screen a splatter is thrown from, seen from the victim rather than from the
 * world — being hit from the east means something different depending on where you are looking.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public enum BloodDirection {

    FRONT,
    RIGHT,
    BACK,
    LEFT;

    /** Above this alignment with the view direction a hit counts as coming from straight ahead. */
    private static final double FORWARD_THRESHOLD = 0.5D;

    /** Below this distance the direction to the source carries no meaning any more. */
    private static final double DISTANCE_EPSILON = 1.0E-6D;

    /**
     * Works out which side a hit came from.
     *
     * @param victim the victim's position, whose yaw and pitch supply the view direction
     * @param source where the damage came from
     * @return the side to throw the splatter from
     */
    public static BloodDirection between(Pos victim, Point source) {
        double distance = victim.distance(source);
        if (distance < DISTANCE_EPSILON) return FRONT;

        Vec towardsSource = new Vec(
                source.x() - victim.x(),
                source.y() - victim.y(),
                source.z() - victim.z()
        ).div(distance);
        Vec facing = victim.direction();

        double alignment = facing.dot(towardsSource);
        if (alignment > FORWARD_THRESHOLD) return FRONT;
        if (alignment < -FORWARD_THRESHOLD) return BACK;

        // The cross product points up when the source sits on the side the victim's left hand is
        // on, which for a player looking south is the east.
        return facing.cross(towardsSource).y() > 0 ? LEFT : RIGHT;
    }
}
