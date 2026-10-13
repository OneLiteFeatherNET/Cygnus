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
import java.util.function.Supplier;

/**
 * The metrics of the server tick: how long every tick and every measured section took, per phase.
 * <p>
 * Spans only exist for a slow tick, so a trace cannot answer "how long does a lobby tick take
 * usually". These metrics can: they are recorded for <em>every</em> tick, which is what makes a
 * percentile per phase possible.
 * </p>
 * <table>
 *   <caption>The instruments</caption>
 *   <tr><th>Name</th><th>Kind</th><th>Unit</th><th>Attributes</th></tr>
 *   <tr><td>{@value #TICK_DURATION}</td><td>histogram</td><td>ms</td><td>{@code cygnus.phase.name}</td></tr>
 *   <tr><td>{@value #SECTION_DURATION}</td><td>histogram</td><td>ms</td>
 *       <td>{@code cygnus.tick.section.name}, {@code cygnus.phase.name}</td></tr>
 *   <tr><td>{@value #SLOW_TICKS}</td><td>counter</td><td>{tick}</td><td>{@code cygnus.phase.name}</td></tr>
 * </table>
 * <p>
 * The unit is milliseconds rather than the seconds the semantic conventions prefer: Minestom's budget
 * (50 ms), the slow tick threshold and the span attributes are all in milliseconds, and a bucket
 * boundary of {@code 50} should read as 50.
 * </p>
 * <p>
 * The tick thread calls this sixty times a second, so the hot path allocates nothing of its own: the
 * {@link Attributes} for every phase and every section/phase pair are built the first time they are
 * seen and reused afterwards, and values are recorded as primitives. Cardinality is bounded by the
 * four phases and the fixed section names of {@link TickSectionNames}.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TickMetrics {

    /** Histogram of the duration of every server tick. */
    public static final String TICK_DURATION = "cygnus.tick.duration";
    /** Histogram of the duration of a measured section, per tick it ran in. */
    public static final String SECTION_DURATION = "cygnus.tick.section.duration";
    /** Counter of the ticks at or above the slow tick threshold. */
    public static final String SLOW_TICKS = "cygnus.tick.slow";

    /**
     * The explicit bucket boundaries in milliseconds, from a hundredth of the budget up to four
     * times it. The default boundaries of the SDK start at 5 and would put the whole lobby in the
     * first bucket.
     */
    public static final List<Double> BUCKETS_MS = List.of(
            0.1D, 0.25D, 0.5D, 1.0D, 2.5D, 5.0D, 10.0D, 15.0D, 20.0D, 25.0D, 30.0D, 40.0D, 50.0D, 75.0D, 100.0D,
            150.0D, 200.0D);

    /** Metrics that record nothing. */
    public static final TickMetrics NONE = new TickMetrics(null, () -> RoundTracer.PHASE_NONE);

    private final @Nullable DoubleHistogram tickDuration;
    private final @Nullable DoubleHistogram sectionDuration;
    private final @Nullable LongCounter slowTicks;
    private final Supplier<String> phase;
    private final Map<String, Attributes> byPhase = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Attributes>> bySection = new ConcurrentHashMap<>();

    private TickMetrics(@Nullable Meter meter, Supplier<String> phase) {
        this.phase = phase;
        if (meter == null) {
            this.tickDuration = null;
            this.sectionDuration = null;
            this.slowTicks = null;
            return;
        }
        this.tickDuration = meter.histogramBuilder(TICK_DURATION)
                .setDescription("How long a server tick took, per phase")
                .setUnit("ms")
                .setExplicitBucketBoundariesAdvice(BUCKETS_MS)
                .build();
        this.sectionDuration = meter.histogramBuilder(SECTION_DURATION)
                .setDescription("How long a measured service took in a tick it ran in, per phase")
                .setUnit("ms")
                .setExplicitBucketBoundariesAdvice(BUCKETS_MS)
                .build();
        this.slowTicks = meter.counterBuilder(SLOW_TICKS)
                .setDescription("Server ticks that took at least the slow tick threshold, per phase")
                .setUnit("{tick}")
                .build();
    }

    /**
     * Builds the metrics from the global OpenTelemetry instance. Called once, by the composition root.
     * Without the javaagent the global instance is a no-op and so is everything recorded here.
     *
     * @param version the version of the running service, reported with the instrumentation scope
     * @param phase   supplies the label of the running phase, see {@link RoundTracer#phaseLabel()}
     * @return the metrics
     */
    public static TickMetrics fromGlobal(String version, Supplier<String> phase) {
        return of(GlobalOpenTelemetry.get().getMeterProvider()
                .meterBuilder(CygnusAttributes.INSTRUMENTATION_NAME)
                .setInstrumentationVersion(version)
                .build(), phase);
    }

    /**
     * Wraps a meter, for tests and for callers that already hold one.
     *
     * @param meter the meter the instruments are created on
     * @param phase supplies the label of the running phase
     * @return the metrics
     */
    public static TickMetrics of(Meter meter, Supplier<String> phase) {
        return new TickMetrics(meter, phase);
    }

    /**
     * Records a finished tick.
     *
     * @param tickMillis how long the tick took
     * @param slow       whether it counts as slow
     */
    void recordTick(double tickMillis, boolean slow) {
        DoubleHistogram duration = this.tickDuration;
        if (duration == null) {
            return;
        }
        Attributes attributes = phaseAttributes(this.phase.get());
        duration.record(tickMillis, attributes);
        if (slow && this.slowTicks != null) {
            this.slowTicks.add(1L, attributes);
        }
    }

    /**
     * Records the time one section took in a tick it ran in.
     *
     * @param section the section name
     * @param millis  how long it took in total in this tick
     */
    void recordSection(String section, double millis) {
        DoubleHistogram duration = this.sectionDuration;
        if (duration == null) {
            return;
        }
        duration.record(millis, sectionAttributes(section, this.phase.get()));
    }

    /**
     * Returns the attributes of a phase. The same instance for the same phase, every time.
     */
    Attributes phaseAttributes(String phaseLabel) {
        Attributes cached = this.byPhase.get(phaseLabel);
        if (cached != null) {
            return cached;
        }
        return this.byPhase.computeIfAbsent(phaseLabel,
                label -> Attributes.of(CygnusAttributes.PHASE_NAME, label));
    }

    /**
     * Returns the attributes of a section in a phase. The same instance for the same pair, every time.
     */
    Attributes sectionAttributes(String section, String phaseLabel) {
        Map<String, Attributes> phases = this.bySection.get(section);
        if (phases == null) {
            phases = this.bySection.computeIfAbsent(section, _ -> new ConcurrentHashMap<>());
        }
        Attributes cached = phases.get(phaseLabel);
        if (cached != null) {
            return cached;
        }
        return phases.computeIfAbsent(phaseLabel, label -> Attributes.of(
                CygnusAttributes.TICK_SECTION_NAME, section,
                CygnusAttributes.PHASE_NAME, label));
    }
}
