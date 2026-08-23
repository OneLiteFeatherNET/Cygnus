package net.onelitefeather.cygnus.tunnelvision;


/**
 * Turns a draining stamina bar into an intensity in {@code [0, 1]} that drives how far the
 * survivor's view narrows.
 * <p>
 * The slender used to feed into this intensity as well; he now speaks through
 * {@code gaze.SlenderGazeService} instead, which tears the view independently rather than adding
 * to this gauge.
 * </p>
 * <p>
 * The calculation is deliberately free of any server state so it can be exercised without a
 * running instance.
 * </p>
 *
 * @author TheMeinerLP
 * @version 2.0.1
 * @since 2.7.0
 */
public final class TunnelVisionIntensity {

    /** Share of the stamina bar below which the view starts to narrow. */
    private static final double STAMINA_THRESHOLD = 0.5D;

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
        return Math.clamp(drained * drained, 0.0D, 1.0D);
    }
}
