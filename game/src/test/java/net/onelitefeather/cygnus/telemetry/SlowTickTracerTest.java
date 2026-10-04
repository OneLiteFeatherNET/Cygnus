package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import net.onelitefeather.cygnus.common.config.TelemetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the slow tick span. Time is a fixed clock and a hand-driven nanosecond counter, so no test
 * waits and none reads the real clock.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.15.0
 */
class SlowTickTracerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:10Z");
    private static final long THRESHOLD_MS = 50;
    private static final long MILLI = 1_000_000L;

    private TestTelemetry telemetry;
    private AtomicLong nanos;
    private TickSections sections;
    private SlowTickTracer tracer;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.nanos = new AtomicLong();
        this.sections = TickSections.measuring(this.nanos::get);
        this.tracer = new SlowTickTracer(this.telemetry.tracing(), new TelemetryConfig((int) THRESHOLD_MS),
                Clock.fixed(NOW, ZoneOffset.UTC), this.sections);
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    /** Runs a measured action that takes the given time on the hand-driven clock. */
    private void runFor(String name, long millis) {
        this.sections.wrap(name, () -> this.nanos.addAndGet(millis * MILLI)).run();
    }

    @Test
    @DisplayName("A tick under the threshold creates no span")
    void fastTickCreatesNoSpan() {
        this.tracer.onTick(49.9D, 0.0D);

        assertTrue(this.telemetry.spans().isEmpty());
    }

    @Test
    @DisplayName("A tick exactly at the threshold is reported")
    void tickAtTheThresholdIsReported() {
        this.tracer.onTick(50.0D, 0.0D);

        this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK);
    }

    @Test
    @DisplayName("A slow tick is a span of the measured duration that ends now")
    void slowTickHasTheMeasuredDuration() {
        this.tracer.onTick(120.0D, 3.5D);

        SpanData span = this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK);
        assertEquals(Duration.ofMillis(120), Duration.ofNanos(span.getEndEpochNanos() - span.getStartEpochNanos()));
        assertEquals(NOW.getEpochSecond() * 1_000_000_000L + NOW.getNano(), span.getEndEpochNanos());
    }

    @Test
    @DisplayName("A slow tick carries the tick time, the acquisition time and the threshold")
    void slowTickAttributes() {
        this.tracer.onTick(120.0D, 3.5D);

        SpanData span = this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK);
        assertEquals(120.0D, span.getAttributes().get(CygnusAttributes.TICK_DURATION_MS));
        assertEquals(3.5D, span.getAttributes().get(CygnusAttributes.TICK_ACQUISITION_MS));
        assertEquals(THRESHOLD_MS, span.getAttributes().get(CygnusAttributes.TICK_THRESHOLD_MS));
    }

    @Test
    @DisplayName("A service that took part of a slow tick is a child span with its share")
    void measuredServiceIsAChild() {
        runFor("creek", 30);

        this.tracer.onTick(80.0D, 0.0D);

        SpanData tick = this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK);
        SpanData section = this.telemetry.span(CygnusAttributes.SPAN_TICK_SECTION);
        assertEquals(tick.getSpanId(), section.getParentSpanId());
        assertEquals("creek", section.getAttributes().get(CygnusAttributes.TICK_SECTION_NAME));
        assertEquals(30.0D, section.getAttributes().get(CygnusAttributes.TICK_SECTION_DURATION_MS));
        assertEquals(Duration.ofMillis(30), Duration.ofNanos(section.getEndEpochNanos() - section.getStartEpochNanos()));
        assertEquals(tick.getStartEpochNanos(), section.getStartEpochNanos(), "a section starts with the tick");
    }

    @Test
    @DisplayName("A service that took a few hundred microseconds still gets its child span")
    void shortShareAboveTheFloorIsReported() {
        this.sections.wrap("ambient", () -> this.nanos.addAndGet(300_000L)).run();

        this.tracer.onTick(80.0D, 0.0D);

        assertEquals(0.3D, this.telemetry.span(CygnusAttributes.SPAN_TICK_SECTION)
                .getAttributes().get(CygnusAttributes.TICK_SECTION_DURATION_MS));
    }

    @Test
    @DisplayName("Every section that ran is a child with its own duration, the ones that did not are omitted")
    void oneChildPerActiveSection() {
        this.sections.wrap(TickSectionNames.AMBIENT, () -> {
        });
        this.sections.wrap(TickSectionNames.CREEK, () -> {
        });
        runFor(TickSectionNames.PAGE_PROXIMITY, 4);
        runFor(TickSectionNames.STAMINA, 7);

        this.tracer.onTick(80.0D, 0.0D);

        List<SpanData> children = this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.SPAN_TICK_SECTION)).toList();
        assertEquals(List.of(TickSectionNames.PAGE_PROXIMITY, TickSectionNames.STAMINA),
                children.stream().map(span -> span.getAttributes().get(CygnusAttributes.TICK_SECTION_NAME)).toList(),
                "only the sections that ran, in registration order");
        assertEquals(List.of(4.0D, 7.0D), children.stream()
                .map(span -> span.getAttributes().get(CygnusAttributes.TICK_SECTION_DURATION_MS)).toList());
    }

    @Test
    @DisplayName("A service that ran twice in the tick is reported once, with both runs added")
    void repeatedRunsAreAdded() {
        runFor("gaze", 10);
        runFor("gaze", 15);

        this.tracer.onTick(80.0D, 0.0D);

        assertEquals(25.0D, this.telemetry.span(CygnusAttributes.SPAN_TICK_SECTION)
                .getAttributes().get(CygnusAttributes.TICK_SECTION_DURATION_MS));
    }

    @Test
    @DisplayName("A service under a tenth of a millisecond is left out")
    void tinyShareIsLeftOut() {
        this.sections.wrap("tunnelvision", () -> this.nanos.addAndGet(SlowTickTracer.SECTION_MIN_NANOS - 1)).run();

        this.tracer.onTick(80.0D, 0.0D);

        assertEquals(List.of(CygnusAttributes.SPAN_SLOW_TICK),
                this.telemetry.spans().stream().map(SpanData::getName).toList());
    }

    @Test
    @DisplayName("The time a service took in a fast tick does not show up in the next slow one")
    void fastTickResetsTheShares() {
        runFor("creek", 40);
        this.tracer.onTick(10.0D, 0.0D);

        this.tracer.onTick(80.0D, 0.0D);

        assertEquals(List.of(CygnusAttributes.SPAN_SLOW_TICK),
                this.telemetry.spans().stream().map(SpanData::getName).toList());
    }

    @Test
    @DisplayName("A service that throws still has its time counted")
    void throwingServiceIsStillCounted() {
        Runnable failing = this.sections.wrap("creek", () -> {
            this.nanos.addAndGet(20 * MILLI);
            throw new IllegalStateException("boom");
        });

        try {
            failing.run();
        } catch (IllegalStateException expected) {
            // the time is what is under test
        }
        this.tracer.onTick(80.0D, 0.0D);

        assertEquals(20.0D, this.telemetry.span(CygnusAttributes.SPAN_TICK_SECTION)
                .getAttributes().get(CygnusAttributes.TICK_SECTION_DURATION_MS));
    }

    @Test
    @DisplayName("Sections that do not measure hand the action back untouched")
    void noneReturnsTheActionItself() {
        Runnable action = () -> {
        };

        assertSame(action, TickSections.NONE.wrap("creek", action));
    }
}
