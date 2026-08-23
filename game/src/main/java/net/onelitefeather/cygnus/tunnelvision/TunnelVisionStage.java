package net.onelitefeather.cygnus.tunnelvision;


/**
 * Holds the overlay state of a single survivor: which of the discrete stages is currently shown,
 * and where the heartbeat that modulates it stands.
 * <p>
 * Two mechanisms sit between the continuous intensity and the rendered stage. Hysteresis keeps the
 * quantised base stage still while distance and stamina jitter around a boundary, and the pulse is
 * added on top of the stabilised value — reversed, the hysteresis would damp out exactly the
 * pulsing it exists to allow.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.1
 * @since 2.7.0
 */
public final class TunnelVisionStage {

    /**
     * Number of stages the overlay is quantised to; stage {@code 0} means no overlay.
     * <p>
     * These double as the frames of the heartbeat: Minecraft cannot animate an overlay texture, so
     * the animation is the server walking through the stages. Thirty-two of them make the view
     * close smoothly; at sixteen the steps were visible as the tunnel narrowed.
     * </p>
     */
    public static final int MAX_STAGE = 32;

    /** Interval the service updates at, which is also the sampling rate of the heartbeat. */
    public static final int TICK_MILLIS = 100;

    /** Distance in stages the intensity has to travel before the base stage follows. */
    private static final double HYSTERESIS = 0.6D;

    /**
     * Depth of the heartbeat in stages at full intensity, as a fraction of the whole scale so it
     * stays equally visible whatever {@link #MAX_STAGE} is.
     */
    private static final double PULSE_DEPTH = MAX_STAGE / 16.0D;

    /** Heartbeat frequency in hertz while the survivor is barely threatened. */
    private static final double BASE_FREQUENCY = 1.0D;

    /** Additional heartbeat frequency in hertz at full intensity. */
    private static final double FREQUENCY_GAIN = 1.5D;

    private static final double TICK_SECONDS = TICK_MILLIS / 1000.0D;

    /** Negative until the first update, so the first intensity is adopted without hysteresis. */
    private int baseStage = -1;

    private double elapsedSeconds;

    /**
     * Advances the heartbeat by one tick and reports the stage to render.
     *
     * @param combined the combined intensity from {@link TunnelVisionIntensity}
     * @return the stage to render, between {@code 0} and {@link #MAX_STAGE}
     */
    public int update(double combined) {
        double exactStage = combined * MAX_STAGE;
        if (this.baseStage < 0 || Math.abs(exactStage - this.baseStage) > HYSTERESIS) {
            this.baseStage = (int) Math.round(exactStage);
        }

        this.elapsedSeconds += TICK_SECONDS;
        double frequency = BASE_FREQUENCY + FREQUENCY_GAIN * combined;
        double depth = PULSE_DEPTH * combined;
        // The heartbeat only ever opens the view up, never beyond the base stage: at full
        // intensity the base stage is the maximum, and a symmetric pulse would be clipped away
        // exactly where it matters most.
        double pulse = depth * (Math.sin(2.0D * Math.PI * frequency * this.elapsedSeconds) - 1.0D);

        int rendered = (int) Math.round(this.baseStage + pulse);
        return Math.clamp(rendered, 0, MAX_STAGE);
    }
}
