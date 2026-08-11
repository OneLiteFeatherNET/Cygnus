package net.onelitefeather.cygnus.gaze;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

/**
 * Works out how badly the sight of the slender tears a survivor's view apart.
 * <p>
 * This is about seeing him, not about him being there: standing behind a survivor does nothing at
 * all, however close he is. Only once he is inside their field of view does the picture start to
 * come apart, and it gets worse the nearer he is.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class SlenderGaze {

    /** Nothing to draw: he is out of range, or out of sight. */
    public static final int NONE = -1;

    /** How many degrees of tearing there are between just visible and right in front. */
    public static final int LEVELS = 4;

    /** Beyond this distance he is too far away to unsettle anything. */
    private static final double RANGE = 32.0D;

    /** The distance at which the tearing is at its worst. */
    private static final double CLOSE = 6.0D;

    /**
     * How far off the view direction he may stand and still count as seen. Roughly the horizontal
     * field of view of a default client — the effect belongs on the screen he is on.
     */
    private static final double FIELD_OF_VIEW = 0.55D;

    /** Below this distance the direction to him carries no meaning any more. */
    private static final double DISTANCE_EPSILON = 1.0E-6D;

    private SlenderGaze() {
    }

    /**
     * Works out the tearing a survivor gets from where the slender stands.
     *
     * @param survivor the survivor's position, whose yaw and pitch supply the view direction
     * @param slender  the slender's position
     * @return a level between {@code 0} and {@code LEVELS - 1}, or {@link #NONE}
     */
    public static int levelOf(Pos survivor, Pos slender) {
        double distance = survivor.distance(slender);
        if (distance > RANGE) return NONE;
        if (distance < DISTANCE_EPSILON) return LEVELS - 1;

        Vec towardsSlender = new Vec(
                slender.x() - survivor.x(),
                slender.y() - survivor.y(),
                slender.z() - survivor.z()
        ).div(distance);

        if (survivor.direction().dot(towardsSlender) < FIELD_OF_VIEW) return NONE;

        double nearness = (RANGE - distance) / (RANGE - CLOSE);
        double clamped = Math.min(1.0D, Math.max(0.0D, nearness));
        return (int) Math.round(clamped * (LEVELS - 1));
    }
}
