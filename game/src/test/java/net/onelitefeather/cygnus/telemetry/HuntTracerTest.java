package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.trace.data.SpanData;
import net.onelitefeather.cygnus.creek.dread.HuntEnd;
import net.onelitefeather.cygnus.telemetry.ActionTracer.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the hunt span and the hunt metrics. Time is a clock the test moves by hand, the SDK is a
 * private in-memory one, nothing waits.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class HuntTracerTest {

    private static final UUID SURVIVOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private TestTelemetry telemetry;
    private InMemoryMetricReader reader;
    private SdkMeterProvider meters;
    private RoundTracer rounds;
    private HuntMetrics metrics;
    private HuntTracer hunts;
    private AtomicReference<Instant> now;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.reader = InMemoryMetricReader.create();
        this.meters = SdkMeterProvider.builder().registerMetricReader(this.reader).build();
        this.metrics = HuntMetrics.of(this.meters.get("test"));
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        this.hunts = new HuntTracer(this.rounds, clock, () -> "Cabin",
                id -> new Actor(id, "survivor", 1.04D, 40.0D, -3.0D), this.metrics);
    }

    @AfterEach
    void tearDown() {
        this.meters.close();
        this.telemetry.close();
    }

    private void pass(long seconds) {
        this.now.updateAndGet(instant -> instant.plusSeconds(seconds));
    }

    private List<SpanData> huntSpans() {
        return this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.ACTION_CREEK_HUNT)).toList();
    }

    private MetricData metric(String name) {
        return this.reader.collectAllMetrics().stream().filter(data -> data.getName().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError("no metric '" + name + "'"));
    }

    @ParameterizedTest(name = "{0} ends the hunt as {1}")
    @CsvSource({"CAUGHT,caught", "TIMEOUT,timeout", "GONE,gone", "SENT_AWAY,escaped", "REMOVED,round_end"})
    @DisplayName("Every way a hunt can end has its outcome")
    void outcomePerEndPath(HuntEnd how, String outcome) {
        this.rounds.roundStarted();
        this.hunts.started(SURVIVOR);
        pass(12);

        this.hunts.ended(SURVIVOR, how);

        SpanData span = huntSpans().getFirst();
        assertEquals(outcome, span.getAttributes().get(CygnusAttributes.HUNT_OUTCOME));
        assertEquals(12_000L, span.getAttributes().get(CygnusAttributes.HUNT_DURATION_MS));
    }

    @Test
    @DisplayName("A hunt span says who, where and on which map, and hangs below the phase")
    void spanAttributes() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("GamePhase");
        this.hunts.started(SURVIVOR);

        this.hunts.ended(SURVIVOR, HuntEnd.CAUGHT);
        this.rounds.roundEnded();

        SpanData span = huntSpans().getFirst();
        assertEquals(SURVIVOR.toString(), span.getAttributes().get(CygnusAttributes.PLAYER_UUID));
        assertEquals("survivor", span.getAttributes().get(CygnusAttributes.PLAYER_ROLE));
        assertEquals(1.0D, span.getAttributes().get(CygnusAttributes.POSITION_X));
        assertEquals(40.0D, span.getAttributes().get(CygnusAttributes.POSITION_Y));
        assertEquals("Cabin", span.getAttributes().get(CygnusAttributes.MAP));
        assertEquals(this.telemetry.span("cygnus.phase.gamephase").getSpanId(), span.getParentSpanId());
    }

    @Test
    @DisplayName("The hunt duration is recorded with the outcome and the map")
    void histogramHasOutcomeAndMap() {
        this.rounds.roundStarted();
        this.hunts.started(SURVIVOR);
        pass(8);
        this.hunts.ended(SURVIVOR, HuntEnd.TIMEOUT);

        MetricData data = metric(HuntMetrics.HUNT_DURATION);
        assertEquals("ms", data.getUnit());
        HistogramPointData point = data.getHistogramData().getPoints().iterator().next();
        assertEquals(1, point.getCount());
        assertEquals(8_000.0D, point.getSum(), 1e-9);
        assertEquals(Attributes.of(CygnusAttributes.HUNT_OUTCOME, "timeout", CygnusAttributes.MAP, "Cabin"),
                point.getAttributes());
        assertEquals(1L, metric(HuntMetrics.HUNTS).getLongSumData().getPoints().iterator().next().getValue());
    }

    @Test
    @DisplayName("No metric carries a player")
    void metricsHaveNoPlayer() {
        this.rounds.roundStarted();
        this.hunts.started(SURVIVOR);
        this.hunts.ended(SURVIVOR, HuntEnd.CAUGHT);

        for (String name : List.of(HuntMetrics.HUNT_DURATION, HuntMetrics.HUNTS)) {
            metric(name).getData().getPoints().forEach(point -> {
                assertEquals(2, point.getAttributes().size(), "outcome and map only");
                assertNull(point.getAttributes().get(CygnusAttributes.PLAYER_UUID));
            });
        }
    }

    @Test
    @DisplayName("The ending of a round closes the hunts still open as round_end, below a phase that still runs")
    void roundEndClosesOpenHunts() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("GamePhase");
        this.hunts.started(SURVIVOR);
        this.hunts.started(UUID.randomUUID());
        pass(5);

        this.rounds.roundEnded();

        List<SpanData> spans = huntSpans();
        assertEquals(2, spans.size());
        SpanData phase = this.telemetry.span("cygnus.phase.gamephase");
        spans.forEach(span -> {
            assertEquals("round_end", span.getAttributes().get(CygnusAttributes.HUNT_OUTCOME));
            assertEquals(5_000L, span.getAttributes().get(CygnusAttributes.HUNT_DURATION_MS));
            assertTrue(span.getEndEpochNanos() <= phase.getEndEpochNanos(), "ends before its parent");
        });
        assertEquals(2L, metric(HuntMetrics.HUNTS).getLongSumData().getPoints().iterator().next().getValue());
    }

    @Test
    @DisplayName("Two creeks hunting one survivor are two spans, closed one by one")
    void twoHuntsOfOneSurvivor() {
        this.rounds.roundStarted();
        this.hunts.started(SURVIVOR);
        pass(2);
        this.hunts.started(SURVIVOR);
        pass(3);

        this.hunts.ended(SURVIVOR, HuntEnd.CAUGHT);
        assertEquals(1, huntSpans().size());
        this.hunts.ended(SURVIVOR, HuntEnd.TIMEOUT);

        assertEquals(2, huntSpans().size());
    }

    @Test
    @DisplayName("An end without a start is ignored, and a hunt is counted even when no round runs")
    void endWithoutStartAndHuntWithoutRound() {
        this.hunts.ended(SURVIVOR, HuntEnd.CAUGHT);
        assertTrue(this.reader.collectAllMetrics().isEmpty());

        this.hunts.started(SURVIVOR);
        pass(1);
        this.hunts.ended(SURVIVOR, HuntEnd.GONE);

        assertTrue(huntSpans().isEmpty(), "no round, no trace to belong to");
        assertEquals(1L, metric(HuntMetrics.HUNTS).getLongSumData().getPoints().iterator().next().getValue());
    }

    @Test
    @DisplayName("The attributes of an outcome on a map are built once and reused")
    void attributesAreReused() {
        assertSame(this.metrics.attributes("caught", "Cabin"), this.metrics.attributes("caught", "Cabin"));
    }

    @Test
    @DisplayName("Metrics that record nothing leave the span working")
    void noneMetrics() {
        HuntTracer plain = new HuntTracer(this.rounds, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), () -> null,
                id -> null, HuntMetrics.NONE);
        this.rounds.roundStarted();

        plain.started(SURVIVOR);
        plain.ended(SURVIVOR, HuntEnd.CAUGHT);

        assertEquals(1, huntSpans().size());
        assertNull(huntSpans().getFirst().getAttributes().get(CygnusAttributes.MAP));
    }
}
