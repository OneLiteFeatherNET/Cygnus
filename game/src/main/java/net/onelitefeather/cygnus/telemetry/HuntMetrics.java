package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The metrics of the creek's hunts: how many there were and how long they lasted, by how they ended.
 * <p>
 * The attributes are the outcome and the map only. A player is deliberately not one: the UUID is the
 * span's job, on a metric it would make a series per player. The unit is milliseconds, like the tick
 * metrics in {@link TickMetrics}.
 * </p>
 * <table>
 *   <caption>The instruments</caption>
 *   <tr><th>Name</th><th>Kind</th><th>Unit</th><th>Attributes</th></tr>
 *   <tr><td>{@value #HUNT_DURATION}</td><td>histogram</td><td>ms</td>
 *       <td>{@code cygnus.creek.hunt.outcome}, {@code cygnus.map}</td></tr>
 *   <tr><td>{@value #HUNTS}</td><td>counter</td><td>{hunt}</td>
 *       <td>{@code cygnus.creek.hunt.outcome}, {@code cygnus.map}</td></tr>
 * </table>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class HuntMetrics {

    /** Histogram of how long a hunt lasted. */
    public static final String HUNT_DURATION = "cygnus.creek.hunt.duration";
    /** Counter of the hunts that ended. */
    public static final String HUNTS = "cygnus.creek.hunts";

    /** The value of the map attribute while no map is loaded. */
    public static final String NO_MAP = "none";

    /** Boundaries in milliseconds: a hunt lasts from a few seconds up to its configured maximum. */
    public static final List<Double> BUCKETS_MS = List.of(
            500.0D, 1_000.0D, 2_500.0D, 5_000.0D, 7_500.0D, 10_000.0D, 15_000.0D, 20_000.0D, 30_000.0D, 45_000.0D,
            60_000.0D, 90_000.0D, 120_000.0D);

    /** Metrics that record nothing. */
    public static final HuntMetrics NONE = new HuntMetrics(null);

    private final @Nullable DoubleHistogram duration;
    private final @Nullable LongCounter hunts;
    private final Map<String, Map<String, Attributes>> attributes = new ConcurrentHashMap<>();

    private HuntMetrics(@Nullable Meter meter) {
        if (meter == null) {
            this.duration = null;
            this.hunts = null;
            return;
        }
        this.duration = meter.histogramBuilder(HUNT_DURATION)
                .setDescription("How long a creek hunted a survivor, by how the hunt ended")
                .setUnit("ms")
                .setExplicitBucketBoundariesAdvice(BUCKETS_MS)
                .build();
        this.hunts = meter.counterBuilder(HUNTS)
                .setDescription("Hunts that ended, by how they ended")
                .setUnit("{hunt}")
                .build();
    }

    /**
     * Builds the metrics from the global OpenTelemetry instance. Called once, by the composition root.
     *
     * @param version the version of the running service, reported with the instrumentation scope
     * @return the metrics
     */
    public static HuntMetrics fromGlobal(String version) {
        return of(GlobalOpenTelemetry.get().getMeterProvider()
                .meterBuilder(CygnusAttributes.INSTRUMENTATION_NAME)
                .setInstrumentationVersion(version)
                .build());
    }

    /**
     * Wraps a meter, for tests and for callers that already hold one.
     *
     * @param meter the meter the instruments are created on
     * @return the metrics
     */
    public static HuntMetrics of(Meter meter) {
        return new HuntMetrics(meter);
    }

    /**
     * Records a hunt that ended.
     *
     * @param outcome how it ended, one of the {@code HUNT_*} values of {@link CygnusAttributes}
     * @param map     the map being played, or {@code null} when none is loaded
     * @param millis  how long it lasted
     */
    void record(String outcome, @Nullable String map, long millis) {
        if (this.duration == null || this.hunts == null) {
            return;
        }
        Attributes cached = attributes(outcome, map == null ? NO_MAP : map);
        this.duration.record(millis, cached);
        this.hunts.add(1L, cached);
    }

    /**
     * Returns the attributes of an outcome on a map. The same instance for the same pair, every time.
     */
    Attributes attributes(String outcome, String map) {
        Map<String, Attributes> byMap = this.attributes.computeIfAbsent(outcome, _ -> new ConcurrentHashMap<>());
        Attributes cached = byMap.get(map);
        if (cached != null) {
            return cached;
        }
        return byMap.computeIfAbsent(map, name -> Attributes.of(
                CygnusAttributes.HUNT_OUTCOME, outcome,
                CygnusAttributes.MAP, name));
    }
}
