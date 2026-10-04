package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the shutdown span.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class ShutdownTracerTest {

    private TestTelemetry telemetry;
    private RoundTracer rounds;
    private ShutdownTracer shutdown;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.shutdown = new ShutdownTracer(this.telemetry.tracing(), this.rounds);
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    @Test
    @DisplayName("The shutdown span is open after the request and ended once the server stopped")
    void spanCoversTheStop() {
        this.shutdown.requested();
        assertTrue(this.telemetry.spans().isEmpty(), "still running while the server stops");

        this.shutdown.serverStopped(null);

        SpanData span = this.telemetry.span(CygnusAttributes.SPAN_SHUTDOWN);
        assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode());
    }

    @Test
    @DisplayName("A server that failed to stop marks the span as failed with the exception")
    void failedStopIsAnError() {
        this.shutdown.requested();

        this.shutdown.serverStopped(new IllegalStateException("refuses to stop"));

        SpanData span = this.telemetry.span(CygnusAttributes.SPAN_SHUTDOWN);
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertTrue(span.getEvents().stream().anyMatch(event -> event.getName().equals("exception")));
    }

    @Test
    @DisplayName("A round running at shutdown is ended with the reason shutdown")
    void runningRoundEndsWithTheShutdown() {
        this.rounds.roundStarted();
        this.shutdown.requested();

        this.shutdown.serverStopped(null);

        assertEquals(ShutdownTracer.END_REASON_SHUTDOWN, this.telemetry.span(CygnusAttributes.SPAN_ROUND)
                .getAttributes().get(CygnusAttributes.GAME_END_REASON));
    }

    @Test
    @DisplayName("A stop that was never requested through the observer creates no span")
    void stopWithoutRequestCreatesNoSpan() {
        this.shutdown.serverStopped(null);

        assertTrue(this.telemetry.spans().isEmpty());
    }
}
