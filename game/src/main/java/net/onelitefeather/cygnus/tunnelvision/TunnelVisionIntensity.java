package net.onelitefeather.cygnus.tunnelvision;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.util.Helper;

/**
 * Turns the two sources of dread — a draining stamina bar and an approaching slender — into a
 * single intensity in {@code [0, 1]} that drives how far the survivor's view narrows.
 * <p>
 * The calculation is deliberately free of any server state so it can be exercised without a
 * running instance.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class TunnelVisionIntensity {

    /** Share of the stamina bar below which the view starts to narrow. */
    private static final double STAMINA_THRESHOLD = 0.5D;

    /** Distance at which the slender starts to weigh on the survivor. */
    private static final double SLENDER_OUTER_RADIUS = 25.0D;

    /** Distance at which the slender's presence peaks. */
    private static final double SLENDER_INNER_RADIUS = 6.0D;

    /** Weight of the slender's presence while he is out of sight. */
    private static final double VIEW_BASE = 0.6D;

    /** Additional weight granted while the survivor looks straight at him. */
    private static final double VIEW_BONUS = 1.0D - VIEW_BASE;

    /** Below this distance the direction towards the slender is no longer meaningful. */
    private static final double DIRECTION_EPSILON = 1.0E-6D;

    private TunnelVisionIntensity() {
    }

    /**
     * Calculates the share contributed by the survivor's stamina.
     * <p>
     * Nothing happens above half a bar; below it the curve accelerates quadratically, so the last
     * few percent feel far more dramatic than crossing the halfway mark.
     * </p>
     *
     * @param normalizedStamina the remaining stamina as a share of a full bar
     * @return the intensity share in {@code [0, 1]}
     */
    public static double fromStamina(double normalizedStamina) {
        if (normalizedStamina >= STAMINA_THRESHOLD) return 0.0D;
        double drained = (STAMINA_THRESHOLD - normalizedStamina) / STAMINA_THRESHOLD;
        return Helper.clamp(drained * drained, 0.0D, 1.0D);
    }

    /**
     * Calculates the share contributed by the slender's presence.
     * <p>
     * The share rises from the outer to the inner radius and is dampened while the survivor looks
     * away from him — being watched is worse than being followed, but never by much.
     * </p>
     *
     * @param survivor the survivor's position, whose yaw and pitch supply the view direction
     * @param slender  the slender's position
     * @return the intensity share in {@code [0, 1]}
     */
    public static double fromSlender(Pos survivor, Pos slender) {
        double distance = survivor.distance(slender);
        double span = SLENDER_OUTER_RADIUS - SLENDER_INNER_RADIUS;
        double proximity = Helper.clamp((SLENDER_OUTER_RADIUS - distance) / span, 0.0D, 1.0D);
        if (proximity == 0.0D) return 0.0D;
        return proximity * viewFactor(survivor, slender, distance);
    }

    /**
     * Merges both shares into the intensity the renderer works with.
     * <p>
     * The shares add up noticeably but saturate at {@code 1.0} instead of clamping hard, so
     * neither source can mask the other.
     * </p>
     *
     * @param stamina the share from {@link #fromStamina(double)}
     * @param slender the share from {@link #fromSlender(Pos, Pos)}
     * @return the combined intensity in {@code [0, 1]}
     */
    public static double combine(double stamina, double slender) {
        double staminaShare = Helper.clamp(stamina, 0.0D, 1.0D);
        double slenderShare = Helper.clamp(slender, 0.0D, 1.0D);
        return Helper.clamp(1.0D - (1.0D - staminaShare) * (1.0D - slenderShare), 0.0D, 1.0D);
    }

    /**
     * Calculates how much the survivor's viewing direction amplifies the slender's presence.
     *
     * @param survivor the survivor's position including yaw and pitch
     * @param slender  the slender's position
     * @param distance the distance between both, to avoid computing it twice
     * @return the factor between {@link #VIEW_BASE} and {@code 1.0}
     */
    private static double viewFactor(Pos survivor, Pos slender, double distance) {
        if (distance < DIRECTION_EPSILON) return 1.0D;
        Vec towardsSlender = new Vec(
                slender.x() - survivor.x(),
                slender.y() - survivor.y(),
                slender.z() - survivor.z()
        ).div(distance);
        double alignment = Math.max(0.0D, survivor.direction().dot(towardsSlender));
        return VIEW_BASE + VIEW_BONUS * alignment;
    }
}
