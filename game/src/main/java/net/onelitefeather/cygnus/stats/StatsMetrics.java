package net.onelitefeather.cygnus.stats;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import net.onelitefeather.cygnus.telemetry.CygnusAttributes;
import org.jetbrains.annotations.Nullable;

/**
 * The metric of the statistics: how many round writes failed for good.
 * <table>
 *   <caption>The instruments</caption>
 *   <tr><th>Name</th><th>Kind</th><th>Unit</th></tr>
 *   <tr><td>{@value #WRITE_FAILURES}</td><td>counter</td><td>{write}</td></tr>
 * </table>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class StatsMetrics {

    /** Counter of round writes that failed after every retry. */
    public static final String WRITE_FAILURES = "cygnus.stats.write.failures";

    /** Metrics that record nothing. */
    public static final StatsMetrics NONE = new StatsMetrics(null);

    private final @Nullable LongCounter failures;

    private StatsMetrics(@Nullable Meter meter) {
        this.failures = meter == null ? null : meter.counterBuilder(WRITE_FAILURES)
                .setDescription("Round statistics that could not be written to the database")
                .setUnit("{write}")
                .build();
    }

    /**
     * Builds the metrics from the global OpenTelemetry instance. Called once, by the composition root.
     *
     * @param version the version of the running service, reported with the instrumentation scope
     * @return the metrics
     */
    public static StatsMetrics fromGlobal(String version) {
        return of(GlobalOpenTelemetry.get().getMeterProvider()
                .meterBuilder(CygnusAttributes.INSTRUMENTATION_NAME)
                .setInstrumentationVersion(version)
                .build());
    }

    /**
     * Wraps a meter, for tests and for callers that already hold one.
     *
     * @param meter the meter the instrument is created on
     * @return the metrics
     */
    public static StatsMetrics of(Meter meter) {
        return new StatsMetrics(meter);
    }

    /**
     * Counts one failed round write.
     */
    void writeFailed() {
        if (this.failures != null) {
            this.failures.add(1L);
        }
    }
}
