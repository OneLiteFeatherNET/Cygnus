package net.onelitefeather.cygnus.common.config;

/**
 * Settings for the OpenTelemetry traces Cygnus creates.
 * <p>
 * Only the slow tick span is tunable: it is the one span that is emitted conditionally, so the
 * operator decides how bad a tick has to be before it is worth a trace. Everything else is
 * low-volume and always on. Without the OpenTelemetry javaagent attached, none of it does anything.
 * </p>
 *
 * @param slowTickThresholdMillis a server tick that takes at least this many milliseconds is
 *                                reported as a {@code cygnus.tick.slow} span, at least 1
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public record TelemetryConfig(int slowTickThresholdMillis) {

    /**
     * A Minestom tick has a budget of 50 ms. Reporting from there on flags every tick that missed
     * its budget, and nothing that stayed inside it.
     */
    public static final int DEFAULT_SLOW_TICK_THRESHOLD_MILLIS = 50;

    /**
     * The default settings.
     */
    public static final TelemetryConfig DEFAULT = new TelemetryConfig(DEFAULT_SLOW_TICK_THRESHOLD_MILLIS);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public TelemetryConfig {
        if (slowTickThresholdMillis < 1) {
            throw new IllegalArgumentException("slowTickThresholdMillis (" + slowTickThresholdMillis + ") must be at least 1");
        }
    }
}
