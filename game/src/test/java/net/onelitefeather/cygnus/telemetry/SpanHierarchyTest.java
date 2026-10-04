package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import net.kyori.adventure.text.Component;
import net.onelitefeather.cygnus.common.config.TelemetryConfig;
import net.onelitefeather.cygnus.telemetry.ActionTracer.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers which span another span hangs below: actions, joins, kicks and slow ticks belong to the phase
 * they happened in, startup and shutdown stay their own traces and link to the round instead.
 * Clocks are fixed or hand-driven, nothing waits.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class SpanHierarchyTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TestTelemetry telemetry;
    private RoundTracer rounds;
    private ActionTracer actions;
    private JoinTracer joins;
    private KickTracer kicks;
    private SlowTickTracer slowTicks;
    private Object connection;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.actions = new ActionTracer(this.rounds, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), () -> "Cabin");
        this.joins = new JoinTracer(this.telemetry.tracing(), this.rounds, new AtomicLong()::get, Duration.ofMinutes(1));
        this.kicks = new KickTracer(this.telemetry.tracing(), this.rounds);
        this.slowTicks = new SlowTickTracer(this.telemetry.tracing(), this.rounds, new TelemetryConfig(50),
                Clock.fixed(Instant.parse("2026-01-01T00:00:10Z"), ZoneOffset.UTC), TickSections.NONE);
        this.connection = new Object();
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    private static Actor slender() {
        return new Actor(PLAYER, "slender", 0, 0, 0);
    }

    @Test
    @DisplayName("An action during the game phase is a child of the game phase span")
    void actionIsAChildOfTheGamePhase() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("GamePhase");

        this.actions.slenderRevived(slender());
        this.rounds.roundEnded();

        SpanData phase = this.telemetry.span("cygnus.phase.gamephase");
        SpanData action = this.telemetry.span(CygnusAttributes.ACTION_SLENDER_REVIVE);
        assertEquals(phase.getSpanId(), action.getParentSpanId());
        assertEquals(phase.getTraceId(), action.getTraceId());
    }

    @Test
    @DisplayName("An action after a phase ended attaches to the next phase, not the closed one")
    void actionAfterAPhaseAttachesToTheNextPhase() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("Waiting");
        this.rounds.phaseEnded();
        this.rounds.phaseStarted("GamePhase");

        this.actions.slenderRevived(slender());
        this.rounds.roundEnded();

        assertEquals(this.telemetry.span("cygnus.phase.gamephase").getSpanId(),
                this.telemetry.span(CygnusAttributes.ACTION_SLENDER_REVIVE).getParentSpanId());
    }

    @Test
    @DisplayName("Between two phases the round is the parent")
    void betweenPhasesTheRoundIsTheParent() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("Lobby");
        this.rounds.phaseEnded();

        this.kicks.complete(this.kicks.begin(PLAYER, Component.text("x")), CygnusAttributes.KICK_BY_ACK);
        this.rounds.roundEnded();

        assertEquals(this.telemetry.span(CygnusAttributes.SPAN_ROUND).getSpanId(),
                this.telemetry.span(CygnusAttributes.SPAN_PLAYER_KICK).getParentSpanId());
    }

    @Test
    @DisplayName("A join started in the lobby is a child of the lobby phase and may outlive it")
    void joinDuringTheLobbyIsAChildOfTheLobby() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("Lobby");
        this.joins.loginStarted(PLAYER, this.connection);
        this.rounds.phaseEnded();
        this.rounds.phaseStarted("Waiting");

        this.joins.spawned(PLAYER, this.connection);
        this.rounds.roundEnded();

        assertEquals(this.telemetry.span("cygnus.phase.lobby").getSpanId(),
                this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN).getParentSpanId());
    }

    @Test
    @DisplayName("A join without a round is a root")
    void joinWithoutARoundIsARoot() {
        this.joins.loginStarted(PLAYER, this.connection);
        this.joins.spawned(PLAYER, this.connection);

        assertFalse(this.telemetry.span(CygnusAttributes.SPAN_PLAYER_JOIN).getParentSpanContext().isValid());
    }

    @Test
    @DisplayName("A kick during the restart is a child of the restart phase")
    void kickDuringTheRestartIsAChildOfTheRestart() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("Restart");

        this.kicks.complete(this.kicks.begin(PLAYER, Component.text("x")), CygnusAttributes.KICK_BY_ACK);
        this.rounds.roundEnded();

        assertEquals(this.telemetry.span("cygnus.phase.restart").getSpanId(),
                this.telemetry.span(CygnusAttributes.SPAN_PLAYER_KICK).getParentSpanId());
    }

    @Test
    @DisplayName("A kick without a round is a root")
    void kickWithoutARoundIsARoot() {
        this.kicks.complete(this.kicks.begin(PLAYER, Component.text("x")), CygnusAttributes.KICK_BY_ACK);

        assertFalse(this.telemetry.span(CygnusAttributes.SPAN_PLAYER_KICK).getParentSpanContext().isValid());
    }

    @Test
    @DisplayName("A slow tick is a child of the current phase")
    void slowTickIsAChildOfTheCurrentPhase() {
        this.rounds.roundStarted();
        this.rounds.phaseStarted("GamePhase");

        this.slowTicks.onTick(80.0D, 0.0D);
        this.rounds.roundEnded();

        assertEquals(this.telemetry.span("cygnus.phase.gamephase").getSpanId(),
                this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK).getParentSpanId());
    }

    @Test
    @DisplayName("A slow tick without a round is a root")
    void slowTickWithoutARoundIsARoot() {
        this.slowTicks.onTick(80.0D, 0.0D);

        assertFalse(this.telemetry.span(CygnusAttributes.SPAN_SLOW_TICK).getParentSpanContext().isValid());
    }

    @Test
    @DisplayName("No context is offered when no round runs, and after the round ended")
    void noContextWithoutARound() {
        assertNull(this.rounds.currentContext());
        this.rounds.roundStarted();
        this.rounds.roundEnded();
        assertNull(this.rounds.currentContext());
    }

    @Test
    @DisplayName("The first round links to the startup span, a later one does not")
    void firstRoundLinksToTheStartup() {
        SpanContext startup = this.telemetry.tracing().root(CygnusAttributes.SPAN_STARTUP).span().getSpanContext();
        this.rounds.startupContext(startup);

        this.rounds.roundStarted();
        this.rounds.roundEnded();
        this.rounds.roundStarted();
        this.rounds.roundEnded();

        List<SpanData> roundSpans = this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.SPAN_ROUND)).toList();
        assertEquals(1, roundSpans.getFirst().getLinks().size());
        LinkData link = roundSpans.getFirst().getLinks().getFirst();
        assertEquals(startup.getSpanId(), link.getSpanContext().getSpanId());
        assertEquals(CygnusAttributes.LINK_STARTUP, link.getAttributes().get(CygnusAttributes.LINK_KIND));
        assertEquals(0, roundSpans.get(1).getLinks().size());
    }

    @Test
    @DisplayName("The shutdown span links to the round that was running")
    void shutdownLinksToTheRound() {
        ShutdownTracer shutdown = new ShutdownTracer(this.telemetry.tracing(), this.rounds);
        this.rounds.roundStarted();

        shutdown.requested();
        shutdown.serverStopped(null);

        SpanData round = this.telemetry.span(CygnusAttributes.SPAN_ROUND);
        SpanData span = this.telemetry.span(CygnusAttributes.SPAN_SHUTDOWN);
        assertEquals(1, span.getLinks().size());
        assertEquals(round.getSpanId(), span.getLinks().getFirst().getSpanContext().getSpanId());
        assertEquals(CygnusAttributes.LINK_ROUND,
                span.getLinks().getFirst().getAttributes().get(CygnusAttributes.LINK_KIND));
        assertFalse(span.getParentSpanContext().isValid(), "the shutdown stays its own trace");
    }

    @Test
    @DisplayName("The shutdown span links to the last round when it already ended")
    void shutdownLinksToTheLastRound() {
        ShutdownTracer shutdown = new ShutdownTracer(this.telemetry.tracing(), this.rounds);
        this.rounds.roundStarted();
        this.rounds.roundEnded();

        shutdown.requested();
        shutdown.serverStopped(null);

        assertEquals(this.telemetry.span(CygnusAttributes.SPAN_ROUND).getSpanId(),
                this.telemetry.span(CygnusAttributes.SPAN_SHUTDOWN).getLinks().getFirst().getSpanContext().getSpanId());
    }

    @Test
    @DisplayName("A shutdown before any round has no link")
    void shutdownWithoutARoundHasNoLink() {
        ShutdownTracer shutdown = new ShutdownTracer(this.telemetry.tracing(), this.rounds);

        shutdown.requested();
        shutdown.serverStopped(null);

        assertEquals(0, this.telemetry.span(CygnusAttributes.SPAN_SHUTDOWN).getLinks().size());
    }
}
