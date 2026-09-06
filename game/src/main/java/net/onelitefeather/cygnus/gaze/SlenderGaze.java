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
 * <p>
 * The three thresholds used to be constants. They are instance state now because they are the part
 * of the effect that has to be judged in the game rather than reasoned about - how near is near
 * enough, and how far off the centre of the screen still counts as looking at him. Reading them
 * from the configuration means answering that costs a restart instead of a rebuild.
 * </p>
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
public final class SlenderGaze {

    /** Nothing to draw: he is out of range, or out of sight. */
    public static final int NONE = -1;

    /** How many degrees of tearing there are between just visible and right in front. */
    public static final int LEVELS = 4;

    /** Below this distance the direction to him carries no meaning any more. */
    private static final double DISTANCE_EPSILON = 1.0E-6D;

    private final double range;
    private final double close;
    private final double fieldOfView;

    /**
     * Creates a gaze with the given thresholds.
     *
     * @param range     how close he has to be before anything happens, in blocks
     * @param close     the distance at which the tearing is at its worst, in blocks
     * @param viewAngle how far off the centre of the survivor's view he may stand and still count
     *                  as seen, in degrees
     * @throws IllegalArgumentException if {@code close} is not below {@code range}, which would
     *                                  make the slope between them run backwards or divide by zero
     */
    public SlenderGaze(int range, int close, int viewAngle) {
        if (close >= range) {
            throw new IllegalArgumentException(
                    "The close range (" + close + ") must be below the range (" + range + ")");
        }
        this.range = range;
        this.close = close;
        // Stored as the cosine because that is what the dot product below compares against. The
        // configuration speaks in degrees instead: 0.87 tells an operator nothing, 30 does.
        this.fieldOfView = Math.cos(Math.toRadians(viewAngle));
    }

    /**
     * Works out the tearing a survivor gets from where the slender stands.
     *
     * @param survivor the survivor's position, whose yaw and pitch supply the view direction
     * @param slender  the slender's position
     * @return a level between {@code 0} and {@code LEVELS - 1}, or {@link #NONE}
     */
    public int levelOf(Pos survivor, Pos slender) {
        double distance = survivor.distance(slender);
        if (distance > this.range) return NONE;
        if (distance < DISTANCE_EPSILON) return LEVELS - 1;

        Vec towardsSlender = new Vec(
                slender.x() - survivor.x(),
                slender.y() - survivor.y(),
                slender.z() - survivor.z()
        ).div(distance);

        if (survivor.direction().dot(towardsSlender) < this.fieldOfView) return NONE;

        double nearness = (this.range - distance) / (this.range - this.close);
        double clamped = Math.clamp(nearness, 0.0D, 1.0D);
        return (int) Math.round(clamped * (LEVELS - 1));
    }
}
