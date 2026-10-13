package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import net.onelitefeather.cygnus.common.config.TelemetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the tick metrics against a private in-memory SDK that is never registered globally. The
 * phase is the real one of a {@link RoundTracer}, time is a hand-driven counter, nothing waits.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class TickMetricsTest {

    private static final long MILLI = 1_000_000L;
    private static final long THRESHOLD_MS = 50;

    private TestTelemetry telemetry;
    private InMemoryMetricReader reader;
    private SdkMeterProvider meters;
    private RoundTracer rounds;
    private AtomicLong nanos;
    private TickSections sections;
    private TickMetrics metrics;
    private SlowTickTracer tracer;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.reader = InMemoryMetricReader.create();
        this.meters = SdkMeterProvider.builder().registerMetricReader(this.reader).build();
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.nanos = new AtomicLong();
        this.sections = TickSections.measuring(this.nanos::get);
        this.metrics = TickMetrics.of(this.meters.get("test"), this.rounds::phaseLabel);
        this.tracer = new SlowTickTracer(this.telemetry.tracing(), this.rounds, new TelemetryConfig((int) THRESHOLD_MS),
                Clock.fixed(Instant.parse("2026-01-01T00:00:10Z"), ZoneOffset.UTC), this.sections, this.metrics);
    }

    @AfterEach
    void tearDown() {
        this.meters.close();
        this.telemetry.close();
    }

    private MetricData metric(String name) {
        return this.reader.collectAllMetrics().stream()
                .filter(data -> data.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no metric '" + name + "' was recorded"));
    }

    private HistogramPointData histogramPoint(String name, Attributes attributes) {
        return metric(name).getHistogramData().getPoints().stream()
                .filter(point -> point.getAttributes().equals(attributes))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no point " + attributes + " in " + name));
    }

    private static Attributes phase(String label) {
        return Attributes.of(CygnusAttributes.PHASE_NAME, label);
    }

    private void runFor(String name, long millis) {
        this.sections.wrap(name, () -> this.nanos.addAndGet(millis * MILLI)).run();
    }

    private void startPhase(String name) {
        this.rounds.roundStarted();
        this.rounds.phaseStarted(name);
    }

    @Test
    @DisplayName("Every tick is recorded, not only the slow ones")
    void everyTickIsRecorded() {
        startPhase("Lobby");

        this.tracer.onTick(1.5D, 0.0D);
        this.tracer.onTick(2.5D, 0.0D);
        this.tracer.onTick(70.0D, 0.0D);

        HistogramPointData lobby = histogramPoint(TickMetrics.TICK_DURATION, phase("lobby"));
        assertEquals(3, lobby.getCount());
        assertEquals(74.0D, lobby.getSum(), 1e-9);
    }

    @Test
    @DisplayName("A tick is recorded under the phase it ran in")
    void tickIsRecordedPerPhase() {
        startPhase("Lobby");
        this.tracer.onTick(3.0D, 0.0D);
        this.rounds.phaseEnded();
        this.rounds.phaseStarted("GamePhase");
        this.tracer.onTick(7.0D, 0.0D);
        this.tracer.onTick(9.0D, 0.0D);

        assertEquals(1, histogramPoint(TickMetrics.TICK_DURATION, phase("lobby")).getCount());
        HistogramPointData game = histogramPoint(TickMetrics.TICK_DURATION, phase("gamephase"));
        assertEquals(2, game.getCount());
        assertEquals(16.0D, game.getSum(), 1e-9);
    }

    @Test
    @DisplayName("A tick with no phase running is recorded as none")
    void tickWithoutAPhaseIsNone() {
        this.tracer.onTick(4.0D, 0.0D);

        assertEquals(1, histogramPoint(TickMetrics.TICK_DURATION, phase(RoundTracer.PHASE_NONE)).getCount());
    }

    @Test
    @DisplayName("Between two phases and after the round the label is none again")
    void labelFallsBackToNone() {
        startPhase("Waiting");
        assertEquals("waiting", this.rounds.phaseLabel());

        this.rounds.phaseEnded();
        assertEquals(RoundTracer.PHASE_NONE, this.rounds.phaseLabel());

        this.rounds.phaseStarted("Restart");
        this.rounds.roundEnded();
        assertEquals(RoundTracer.PHASE_NONE, this.rounds.phaseLabel());
    }

    @Test
    @DisplayName("A section is recorded per tick it ran in, with its name and the phase")
    void sectionIsRecordedPerPhase() {
        startPhase("Lobby");
        runFor(TickSectionNames.LOBBY_WAITING, 2);
        this.tracer.onTick(5.0D, 0.0D);
        runFor(TickSectionNames.LOBBY_WAITING, 3);
        this.tracer.onTick(5.0D, 0.0D);
        this.rounds.phaseEnded();
        this.rounds.phaseStarted("GamePhase");
        runFor(TickSectionNames.CREEK, 4);
        this.tracer.onTick(5.0D, 0.0D);

        HistogramPointData waiting = histogramPoint(TickMetrics.SECTION_DURATION, Attributes.of(
                CygnusAttributes.TICK_SECTION_NAME, TickSectionNames.LOBBY_WAITING,
                CygnusAttributes.PHASE_NAME, "lobby"));
        assertEquals(2, waiting.getCount());
        assertEquals(5.0D, waiting.getSum(), 1e-9);
        HistogramPointData creek = histogramPoint(TickMetrics.SECTION_DURATION, Attributes.of(
                CygnusAttributes.TICK_SECTION_NAME, TickSectionNames.CREEK,
                CygnusAttributes.PHASE_NAME, "gamephase"));
        assertEquals(1, creek.getCount());
        assertEquals(4.0D, creek.getSum(), 1e-9);
    }

    @Test
    @DisplayName("A section that did not run in a tick is not recorded for it")
    void idleSectionIsNotRecorded() {
        startPhase("Lobby");
        this.sections.wrap(TickSectionNames.CREEK, () -> {
        });
        runFor(TickSectionNames.AMBIENT, 1);

        this.tracer.onTick(5.0D, 0.0D);

        List<String> recorded = metric(TickMetrics.SECTION_DURATION).getHistogramData().getPoints().stream()
                .map(point -> point.getAttributes().get(CygnusAttributes.TICK_SECTION_NAME)).toList();
        assertEquals(List.of(TickSectionNames.AMBIENT), recorded);
    }

    @Test
    @DisplayName("The time of a fast tick is recorded once and does not show up in the next tick")
    void sectionTimeDoesNotLeakIntoTheNextTick() {
        startPhase("Lobby");
        runFor(TickSectionNames.AMBIENT, 6);
        this.tracer.onTick(5.0D, 0.0D);
        this.tracer.onTick(5.0D, 0.0D);

        HistogramPointData ambient = histogramPoint(TickMetrics.SECTION_DURATION, Attributes.of(
                CygnusAttributes.TICK_SECTION_NAME, TickSectionNames.AMBIENT,
                CygnusAttributes.PHASE_NAME, "lobby"));
        assertEquals(1, ambient.getCount());
    }

    @Test
    @DisplayName("The slow counter counts the ticks at or above the threshold and no others")
    void slowCounterOnlyCountsSlowTicks() {
        startPhase("Lobby");

        this.tracer.onTick(49.9D, 0.0D);
        this.tracer.onTick(50.0D, 0.0D);
        this.tracer.onTick(120.0D, 0.0D);

        LongPointData point = metric(TickMetrics.SLOW_TICKS).getLongSumData().getPoints().iterator().next();
        assertEquals(2L, point.getValue());
        assertEquals(phase("lobby"), point.getAttributes());
    }

    @Test
    @DisplayName("Without a slow tick the counter has no point at all")
    void noSlowTickNoPoint() {
        this.tracer.onTick(10.0D, 0.0D);

        assertTrue(this.reader.collectAllMetrics().stream()
                .noneMatch(data -> data.getName().equals(TickMetrics.SLOW_TICKS) && !data.getLongSumData().getPoints().isEmpty()));
    }

    @Test
    @DisplayName("The histograms are in milliseconds with boundaries that resolve a tenth of a millisecond")
    void histogramsUseMillisecondsAndFineBuckets() {
        this.tracer.onTick(1.0D, 0.0D);

        MetricData data = metric(TickMetrics.TICK_DURATION);
        assertEquals("ms", data.getUnit());
        assertEquals(TickMetrics.BUCKETS_MS, data.getHistogramData().getPoints().iterator().next().getBoundaries());
        assertEquals(0.1D, TickMetrics.BUCKETS_MS.getFirst());
        assertEquals(200.0D, TickMetrics.BUCKETS_MS.getLast());
    }

    @Test
    @DisplayName("The attributes of a phase and of a section are built once and reused")
    void attributesAreReused() {
        assertSame(this.metrics.phaseAttributes("lobby"), this.metrics.phaseAttributes("lobby"));
        assertSame(this.metrics.sectionAttributes("creek", "gamephase"), this.metrics.sectionAttributes("creek", "gamephase"));
        assertTrue(this.metrics.phaseAttributes("lobby") != this.metrics.phaseAttributes("waiting"));
        assertTrue(this.metrics.sectionAttributes("creek", "gamephase") != this.metrics.sectionAttributes("creek", "lobby"));
    }

    @Test
    @DisplayName("Metrics that record nothing leave the tracer working")
    void noneRecordsNothing() {
        SlowTickTracer plain = new SlowTickTracer(this.telemetry.tracing(), this.rounds, new TelemetryConfig((int) THRESHOLD_MS),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), this.sections);

        plain.onTick(80.0D, 0.0D);

        this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK);
        assertTrue(this.reader.collectAllMetrics().isEmpty());
    }
}
