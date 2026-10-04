package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the span of a player joining.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class JoinTracerTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TestTelemetry telemetry;
    private JoinTracer joins;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.joins = new JoinTracer(this.telemetry.tracing());
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    @Test
    @DisplayName("A join stays open until the first spawn, then ends with the player's UUID")
    void joinEndsAtTheFirstSpawn() {
        this.joins.loginStarted(PLAYER);
        this.joins.configurationStarted(PLAYER);
        assertTrue(this.telemetry.spans().isEmpty(), "nothing ended before the spawn");

        this.joins.spawned(PLAYER);

        SpanData join = this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN);
        assertEquals(PLAYER.toString(), join.getAttributes().get(CygnusAttributes.PLAYER_UUID));
        assertEquals(JoinTracer.OUTCOME_SPAWNED, join.getAttributes().get(CygnusAttributes.JOIN_OUTCOME));
    }

    @Test
    @DisplayName("The configuration and the ResourcePack status are events on the join span")
    void stationsAreEvents() {
        this.joins.loginStarted(PLAYER);
        this.joins.configurationStarted(PLAYER);
        this.joins.resourcePackStatus(PLAYER, ResourcePackStatus.SUCCESSFULLY_LOADED);
        this.joins.spawned(PLAYER);

        SpanData join = this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN);
        TestTelemetry.event(join, CygnusAttributes.EVENT_JOIN_CONFIGURATION);
        assertEquals("SUCCESSFULLY_LOADED", TestTelemetry.event(join, CygnusAttributes.EVENT_JOIN_RESOURCEPACK)
                .getAttributes().get(CygnusAttributes.RESOURCEPACK_STATUS));
    }

    @Test
    @DisplayName("A player who leaves before spawning ends the join as abandoned")
    void leavingBeforeTheSpawnAbandonsTheJoin() {
        this.joins.loginStarted(PLAYER);

        this.joins.disconnected(PLAYER);

        assertEquals(JoinTracer.OUTCOME_ABANDONED, this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN)
                .getAttributes().get(CygnusAttributes.JOIN_OUTCOME));
    }

    @Test
    @DisplayName("A disconnect after a finished join adds no span")
    void disconnectAfterTheJoinAddsNothing() {
        this.joins.loginStarted(PLAYER);
        this.joins.spawned(PLAYER);

        this.joins.disconnected(PLAYER);

        assertEquals(1, this.telemetry.spans().size());
    }

    @Test
    @DisplayName("A configuration without a seen login still gets a join span")
    void configurationOpensTheSpanWhenTheLoginWasMissed() {
        this.joins.configurationStarted(PLAYER);
        this.joins.spawned(PLAYER);

        TestTelemetry.event(this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN),
                CygnusAttributes.EVENT_JOIN_CONFIGURATION);
    }

    @Test
    @DisplayName("A second login of the same player ends the stale join first")
    void secondLoginEndsTheStaleJoin() {
        this.joins.loginStarted(PLAYER);
        this.joins.loginStarted(PLAYER);

        assertEquals(1, this.telemetry.spans().size(), "the stale span ended");
        this.joins.spawned(PLAYER);
        assertEquals(2, this.telemetry.spans().size(), "and the new one ends at its spawn");
    }

    @Test
    @DisplayName("Joins of two players are separate traces")
    void playersDoNotShareASpan() {
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        this.joins.loginStarted(PLAYER);
        this.joins.loginStarted(other);

        this.joins.spawned(other);

        assertEquals(1, this.telemetry.spans().size());
        assertEquals(other.toString(), this.telemetry.spans().getFirst()
                .getAttributes().get(CygnusAttributes.PLAYER_UUID));
    }
}
