package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the span of a player joining. Connections are plain marker objects and time is a
 * hand-driven counter, so nothing here waits.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.15.0
 */
class JoinTracerTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private TestTelemetry telemetry;
    private AtomicLong nanos;
    private JoinTracer joins;
    private Object connection;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.nanos = new AtomicLong();
        this.joins = new JoinTracer(this.telemetry.tracing(), this.nanos::get, TIMEOUT);
        this.connection = new Object();
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    @Test
    @DisplayName("A join stays open until the first spawn, then ends with the player's UUID")
    void joinEndsAtTheFirstSpawn() {
        this.joins.loginStarted(PLAYER, this.connection);
        this.joins.configurationStarted(PLAYER, this.connection);
        assertTrue(this.telemetry.spans().isEmpty(), "nothing ended before the spawn");

        this.joins.spawned(PLAYER, this.connection);

        SpanData join = this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN);
        assertEquals(PLAYER.toString(), join.getAttributes().get(CygnusAttributes.PLAYER_UUID));
        assertEquals(JoinTracer.OUTCOME_SPAWNED, join.getAttributes().get(CygnusAttributes.JOIN_OUTCOME));
    }

    @Test
    @DisplayName("The configuration and the ResourcePack status are events on the join span")
    void stationsAreEvents() {
        this.joins.loginStarted(PLAYER, this.connection);
        this.joins.configurationStarted(PLAYER, this.connection);
        this.joins.resourcePackStatus(PLAYER, this.connection, ResourcePackStatus.SUCCESSFULLY_LOADED);
        this.joins.spawned(PLAYER, this.connection);

        SpanData join = this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN);
        TestTelemetry.event(join, CygnusAttributes.EVENT_JOIN_CONFIGURATION);
        assertEquals("SUCCESSFULLY_LOADED", TestTelemetry.event(join, CygnusAttributes.EVENT_JOIN_RESOURCEPACK)
                .getAttributes().get(CygnusAttributes.RESOURCEPACK_STATUS));
    }

    @Test
    @DisplayName("A player who leaves before spawning ends the join as abandoned")
    void leavingBeforeTheSpawnAbandonsTheJoin() {
        this.joins.loginStarted(PLAYER, this.connection);

        this.joins.disconnected(PLAYER, this.connection);

        assertEquals(JoinTracer.OUTCOME_ABANDONED, this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN)
                .getAttributes().get(CygnusAttributes.JOIN_OUTCOME));
    }

    @Test
    @DisplayName("A disconnect after a finished join adds no span")
    void disconnectAfterTheJoinAddsNothing() {
        this.joins.loginStarted(PLAYER, this.connection);
        this.joins.spawned(PLAYER, this.connection);

        this.joins.disconnected(PLAYER, this.connection);

        assertEquals(1, this.telemetry.spans().size());
    }

    @Test
    @DisplayName("A configuration that arrives after the disconnect opens no span")
    void lateConfigurationDoesNotReopenTheJoin() {
        this.joins.loginStarted(PLAYER, this.connection);
        this.joins.disconnected(PLAYER, this.connection);

        this.joins.configurationStarted(PLAYER, this.connection);
        this.nanos.addAndGet(TIMEOUT.toNanos() * 2);
        this.joins.sweep();

        assertEquals(1, this.telemetry.spans().size(), "only the abandoned join, nothing re-opened");
    }

    @Test
    @DisplayName("A configuration without a seen login opens no span")
    void configurationWithoutLoginIsIgnored() {
        this.joins.configurationStarted(PLAYER, this.connection);
        this.joins.spawned(PLAYER, this.connection);

        assertTrue(this.telemetry.spans().isEmpty());
    }

    @Test
    @DisplayName("A second login of the same player ends the stale join first")
    void secondLoginEndsTheStaleJoin() {
        this.joins.loginStarted(PLAYER, this.connection);
        Object second = new Object();
        this.joins.loginStarted(PLAYER, second);

        assertEquals(1, this.telemetry.spans().size(), "the stale span ended");
        this.joins.spawned(PLAYER, second);
        assertEquals(2, this.telemetry.spans().size(), "and the new one ends at its spawn");
    }

    @Test
    @DisplayName("The late disconnect of an old connection does not end the join of the new one")
    void oldDisconnectLeavesTheNewJoinAlone() {
        Object oldConnection = this.connection;
        Object newConnection = new Object();
        this.joins.loginStarted(PLAYER, oldConnection);
        this.joins.loginStarted(PLAYER, newConnection);
        int endedByReplacement = this.telemetry.spans().size();

        this.joins.disconnected(PLAYER, oldConnection);
        assertEquals(endedByReplacement, this.telemetry.spans().size(), "the new join is still open");

        this.joins.spawned(PLAYER, newConnection);
        assertEquals(JoinTracer.OUTCOME_SPAWNED, this.telemetry.spans().getLast()
                .getAttributes().get(CygnusAttributes.JOIN_OUTCOME));
    }

    @Test
    @DisplayName("Joins of two players are separate traces")
    void playersDoNotShareASpan() {
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Object otherConnection = new Object();
        this.joins.loginStarted(PLAYER, this.connection);
        this.joins.loginStarted(other, otherConnection);

        this.joins.spawned(other, otherConnection);

        assertEquals(1, this.telemetry.spans().size());
        assertEquals(other.toString(), this.telemetry.spans().getFirst()
                .getAttributes().get(CygnusAttributes.PLAYER_UUID));
    }

    @Test
    @DisplayName("A login whose connection died before a player existed is ended as timeout by the sweep")
    void sweepEndsJoinsThatNeverReachedASpawn() {
        this.joins.loginStarted(PLAYER, this.connection);

        this.nanos.addAndGet(TIMEOUT.toNanos() - 1);
        this.joins.sweep();
        assertTrue(this.telemetry.spans().isEmpty(), "not yet timed out");

        this.nanos.addAndGet(1);
        this.joins.sweep();
        assertEquals(JoinTracer.OUTCOME_TIMEOUT, this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN)
                .getAttributes().get(CygnusAttributes.JOIN_OUTCOME), "a timeout, not an abandoned join: the player may just be slow");
    }

    @Test
    @DisplayName("The default timeout leaves room for a slow ResourcePack download")
    void defaultTimeoutIsGenerous() {
        assertTrue(JoinTracer.DEFAULT_TIMEOUT.compareTo(Duration.ofMinutes(10)) >= 0);
    }

    @Test
    @DisplayName("The sweep ends a join once, and leaves a join that spawned alone")
    void sweepIsIdempotentAndSkipsFinishedJoins() {
        this.joins.loginStarted(PLAYER, this.connection);
        this.joins.spawned(PLAYER, this.connection);
        this.nanos.addAndGet(TIMEOUT.toNanos() * 2);

        this.joins.sweep();
        this.joins.sweep();

        assertEquals(1, this.telemetry.spans().size());
        assertEquals(JoinTracer.OUTCOME_SPAWNED, this.telemetry.spans().getFirst()
                .getAttributes().get(CygnusAttributes.JOIN_OUTCOME));
    }
}
