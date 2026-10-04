package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.onelitefeather.cygnus.common.config.TelemetryConfig;

import java.time.Clock;
import java.time.Instant;

/**
 * Reports server ticks that took too long, and nothing else.
 * <p>
 * A span per tick would be twenty a second per service; a slow tick is the exception worth a trace.
 * Minestom has already measured the tick by the time it fires {@link ServerTickMonitorEvent}, so the
 * span is created <em>retroactively</em>: its start is set to the end minus the measured duration.
 * That keeps the hot path free of any tracing work - for a tick under the threshold the cost is one
 * comparison and the reset of a few counters.
 * </p>
 * <p>
 * The services measured through {@link TickSections} appear as child spans of the slow tick. They
 * all start at the start of the tick and last as long as the service took in total: the position of
 * the work inside the tick is not known, only its share, so the children show who was in the tick, not
 * when. Shares below {@value #SECTION_MIN_NANOS} ns are left out.
 * </p>
 * <p>
 * The wall clock is read only for a slow tick, to place the span in time; durations come from
 * Minestom's own monotonic measurement.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class SlowTickTracer {

    /**
     * A service that took less than a millisecond of a slow tick is not worth a span of its own.
     */
    static final long SECTION_MIN_NANOS = 1_000_000L;

    private static final double NANOS_PER_MILLI = 1_000_000.0D;

    private final CygnusTracing tracing;
    private final long thresholdMillis;
    private final Clock clock;
    private final TickSections sections;

    /**
     * Creates the tracer.
     *
     * @param tracing  where the spans go
     * @param config   holds the threshold
     * @param clock    places a slow tick in time
     * @param sections the measured services, {@link TickSections#NONE} for none
     */
    public SlowTickTracer(CygnusTracing tracing, TelemetryConfig config, Clock clock, TickSections sections) {
        this.tracing = tracing;
        this.thresholdMillis = config.slowTickThresholdMillis();
        this.clock = clock;
        this.sections = sections;
    }

    /**
     * Handles a finished tick.
     *
     * @param tickMillis        how long the whole tick took
     * @param acquisitionMillis how much of it went into waiting for acquirable entities
     */
    public void onTick(double tickMillis, double acquisitionMillis) {
        if (tickMillis < this.thresholdMillis) {
            // The counters of a fast tick must not leak into the next slow one.
            for (int i = 0; i < this.sections.size(); i++) {
                this.sections.takeNanos(i);
            }
            return;
        }
        report(tickMillis, acquisitionMillis);
    }

    private void report(double tickMillis, double acquisitionMillis) {
        Instant end = this.clock.instant();
        Instant start = end.minusNanos((long) (tickMillis * NANOS_PER_MILLI));
        Span tick = this.tracing.tracer().spanBuilder(CygnusAttributes.SPAN_SLOW_TICK)
                .setNoParent()
                .setStartTimestamp(start)
                .startSpan();
        try {
            tick.setAttribute(CygnusAttributes.TICK_DURATION_MS, tickMillis);
            tick.setAttribute(CygnusAttributes.TICK_ACQUISITION_MS, acquisitionMillis);
            tick.setAttribute(CygnusAttributes.TICK_THRESHOLD_MS, this.thresholdMillis);
            reportSections(tick, start, end);
        } finally {
            tick.end(end);
        }
    }

    private void reportSections(Span tick, Instant start, Instant end) {
        Context parent = Context.root().with(tick);
        for (int i = 0; i < this.sections.size(); i++) {
            long nanos = this.sections.takeNanos(i);
            if (nanos < SECTION_MIN_NANOS) {
                continue;
            }
            Instant sectionEnd = start.plusNanos(nanos);
            if (sectionEnd.isAfter(end)) {
                sectionEnd = end;
            }
            Span section = this.tracing.tracer().spanBuilder(CygnusAttributes.SPAN_TICK_SECTION)
                    .setParent(parent)
                    .setStartTimestamp(start)
                    .startSpan();
            try {
                section.setAttribute(CygnusAttributes.TICK_SECTION_NAME, this.sections.name(i));
                section.setAttribute(CygnusAttributes.TICK_SECTION_DURATION_MS, nanos / NANOS_PER_MILLI);
            } finally {
                section.end(sectionEnd);
            }
        }
    }

    /**
     * Hooks the tick monitor onto this tracer.
     *
     * @param node the node to listen on
     */
    public void register(EventNode<? super Event> node) {
        node.addListener(ServerTickMonitorEvent.class, event ->
                onTick(event.getTickMonitor().getTickTime(), event.getTickMonitor().getAcquisitionTime()));
    }
}
