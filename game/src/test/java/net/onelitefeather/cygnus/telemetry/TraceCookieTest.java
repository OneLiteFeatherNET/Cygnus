package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the trace cookie: what is written, and what is made of what a client hands back. The cookie
 * comes from the client, so most of these are about refusing it.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class TraceCookieTest {

    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String SPAN_ID = "b7ad6b7169203331";
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private TestTelemetry telemetry;
    private RoundTracer rounds;
    private JoinTracer joins;
    private TraceCookie cookie;
    private Object connection;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.joins = new JoinTracer(this.telemetry.tracing());
        this.cookie = new TraceCookie(this.rounds, this.joins);
        this.connection = new Object();
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] valid() {
        return bytes("00-" + TRACE_ID + "-" + SPAN_ID + "-01");
    }

    @Test
    @DisplayName("The round's context is written as a W3C traceparent")
    void roundContextIsWrittenAsTraceparent() {
        this.rounds.roundStarted();
        SpanContext round = this.rounds.roundContext();
        assertNotNull(round);

        byte[] written = this.cookie.roundCookie();

        assertNotNull(written);
        assertEquals("00-" + round.getTraceId() + "-" + round.getSpanId() + "-0" + (round.isSampled() ? "1" : "0"),
                new String(written, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("With no round running there is nothing to write")
    void noRoundNoCookie() {
        assertNull(this.cookie.roundCookie());
    }

    @Test
    @DisplayName("A written cookie reads back as the same remote context")
    void cookieRoundTrips() {
        this.rounds.roundStarted();
        SpanContext round = this.rounds.roundContext();

        SpanContext read = TraceCookie.decode(this.cookie.roundCookie());

        assertNotNull(read);
        assertEquals(round.getTraceId(), read.getTraceId());
        assertEquals(round.getSpanId(), read.getSpanId());
        assertTrue(read.isRemote(), "what a cookie hands back always comes from elsewhere");
    }

    @Test
    @DisplayName("A cookie that is absent, empty or too large is refused")
    void absentEmptyAndOversizedAreRefused() {
        assertNull(TraceCookie.decode(null));
        assertNull(TraceCookie.decode(new byte[0]));
        assertNull(TraceCookie.decode(new byte[TraceCookie.MAX_BYTES + 1]));
        assertNull(TraceCookie.decode(bytes("00-" + TRACE_ID + "-" + SPAN_ID + "-01" + "x".repeat(TraceCookie.MAX_BYTES))));
    }

    @Test
    @DisplayName("Garbage, a wrong version and all-zero ids are refused")
    void malformedIsRefused() {
        assertNull(TraceCookie.decode(bytes("not a traceparent")));
        assertNull(TraceCookie.decode(bytes("ff-" + TRACE_ID + "-" + SPAN_ID + "-01")));
        assertNull(TraceCookie.decode(bytes("00-" + "0".repeat(32) + "-" + SPAN_ID + "-01")));
        assertNull(TraceCookie.decode(bytes("00-" + TRACE_ID + "-" + "0".repeat(16) + "-01")));
        assertNull(TraceCookie.decode(new byte[]{(byte) 0xff, (byte) 0xfe, 0, 1}));
    }

    @Test
    @DisplayName("A valid cookie links the join span to the trace it came from")
    void validCookieLinksTheJoin() {
        this.joins.loginStarted(PLAYER, this.connection);

        this.cookie.joined(PLAYER, this.connection, CompletableFuture.completedFuture(valid()));
        this.joins.spawned(PLAYER, this.connection);

        SpanData join = this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN);
        assertEquals(1, join.getLinks().size());
        LinkData link = join.getLinks().getFirst();
        assertEquals(TRACE_ID, link.getSpanContext().getTraceId());
        assertEquals(SPAN_ID, link.getSpanContext().getSpanId());
        assertEquals(CygnusAttributes.LINK_PREVIOUS_ROUND, link.getAttributes().get(CygnusAttributes.LINK_KIND));
    }

    @Test
    @DisplayName("The running round is linked to the incoming trace once, however many players bring it")
    void roundIsLinkedOncePerTrace() {
        this.rounds.roundStarted();
        this.joins.loginStarted(PLAYER, this.connection);
        Object otherConnection = new Object();
        this.joins.loginStarted(OTHER, otherConnection);

        this.cookie.joined(PLAYER, this.connection, CompletableFuture.completedFuture(valid()));
        this.cookie.joined(OTHER, otherConnection, CompletableFuture.completedFuture(valid()));
        this.rounds.abort("test");

        assertEquals(1, this.telemetry.span(CygnusAttributes.SPAN_ROUND).getLinks().size());
    }

    @Test
    @DisplayName("A cookie holding the running round's own trace is not linked to itself")
    void ownTraceIsNotLinked() {
        this.rounds.roundStarted();
        SpanContext own = this.rounds.roundContext();
        this.joins.loginStarted(PLAYER, this.connection);

        this.cookie.joined(PLAYER, this.connection, CompletableFuture.completedFuture(this.cookie.roundCookie()));
        this.rounds.abort("test");

        assertEquals(0, this.telemetry.span(CygnusAttributes.SPAN_ROUND).getLinks().size(),
                "linking a round to its own trace " + own.getTraceId() + " says nothing");
    }

    @Test
    @DisplayName("A missing, invalid or failed cookie adds no link and throws nothing")
    void unusableCookiesAddNothing() {
        this.joins.loginStarted(PLAYER, this.connection);

        this.cookie.joined(PLAYER, this.connection, CompletableFuture.completedFuture(null));
        this.cookie.joined(PLAYER, this.connection, CompletableFuture.completedFuture(bytes("garbage")));
        this.cookie.joined(PLAYER, this.connection, CompletableFuture.failedFuture(new IllegalStateException("no")));
        this.joins.spawned(PLAYER, this.connection);

        assertEquals(0, this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN).getLinks().size());
    }

    @Test
    @DisplayName("A cookie that arrives after the join ended adds no link")
    void lateCookieIsIgnored() {
        this.joins.loginStarted(PLAYER, this.connection);
        CompletableFuture<byte[]> pending = new CompletableFuture<>();
        this.cookie.joined(PLAYER, this.connection, pending);
        this.joins.spawned(PLAYER, this.connection);

        pending.complete(valid());

        assertEquals(0, this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN).getLinks().size());
    }
}
