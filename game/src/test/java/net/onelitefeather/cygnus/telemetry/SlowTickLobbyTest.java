package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
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

/**
 * Covers the slow tick in the lobby, where neither the creek nor the gaze nor the tunnel vision run:
 * the tick still has to say who was in it. Time is a hand-driven counter, nothing waits.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class SlowTickLobbyTest {

    private static final long MILLI = 1_000_000L;

    private TestTelemetry telemetry;
    private AtomicLong nanos;
    private TickSections sections;
    private RoundTracer rounds;
    private SlowTickTracer tracer;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.nanos = new AtomicLong();
        this.sections = TickSections.measuring(this.nanos::get);
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.tracer = new SlowTickTracer(this.telemetry.tracing(), this.rounds, new TelemetryConfig(50),
                Clock.fixed(Instant.parse("2026-01-01T00:00:10Z"), ZoneOffset.UTC), this.sections);
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    @Test
    @DisplayName("A slow lobby tick has the lobby's tasks as children and none of the game's")
    void lobbyTickHasTheLobbyTasksAsChildren() {
        // Every service registers once at startup, whether or not it runs in this phase.
        TickSectionNames.ALL.forEach(name -> this.sections.wrap(name, () -> {
        }));
        this.rounds.roundStarted();
        this.rounds.phaseStarted("Lobby");
        this.sections.wrap(TickSectionNames.LOBBY_WAITING, () -> this.nanos.addAndGet(2 * MILLI)).run();
        this.sections.wrap(TickSectionNames.LOBBY_TIME, () -> this.nanos.addAndGet(MILLI / 2)).run();

        this.tracer.onTick(59.0D, 0.0D);
        this.rounds.roundEnded();

        SpanData tick = this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK);
        List<SpanData> children = this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.SPAN_TICK_SECTION)).toList();
        assertEquals(List.of(TickSectionNames.LOBBY_WAITING, TickSectionNames.LOBBY_TIME),
                children.stream().map(span -> span.getAttributes().get(CygnusAttributes.TICK_SECTION_NAME)).toList());
        children.forEach(child -> assertEquals(tick.getSpanId(), child.getParentSpanId()));
        assertEquals(this.telemetry.span("cygnus.phase.lobby").getSpanId(), tick.getParentSpanId(),
                "the slow tick stays parented to the phase it happened in");
    }
}
