package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import net.theevilreaper.xerus.api.phase.Phase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the round span and its phase children.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class RoundTracerTest {

    private TestTelemetry telemetry;
    private RoundTracer rounds;
    private TracedPhaseSeries<Phase> series;
    private StubPhase lobby;
    private StubPhase game;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.series = new TracedPhaseSeries<>("game", this.rounds);
        this.lobby = new StubPhase("Lobby");
        this.game = new StubPhase("GamePhase");
        this.series.add(this.lobby);
        this.series.add(this.game);
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    @Test
    @DisplayName("A finished round is one root span with its id")
    void roundIsARootSpanWithItsId() {
        this.series.start();
        this.lobby.finish();
        this.game.finish();

        SpanData round = this.telemetry.span(CygnusAttributes.SPAN_ROUND);
        assertEquals("round-1", round.getAttributes().get(CygnusAttributes.ROUND_ID));
        assertFalse(round.getParentSpanContext().isValid(), "the round has no parent");
        assertTrue(round.hasEnded());
    }

    @Test
    @DisplayName("Every phase is a child of the round span, named after the phase")
    void phasesAreChildrenOfTheRound() {
        this.series.start();
        this.lobby.finish();
        this.game.finish();

        SpanData round = this.telemetry.span(CygnusAttributes.SPAN_ROUND);
        SpanData lobbySpan = this.telemetry.span("cygnus.phase.lobby");
        SpanData gameSpan = this.telemetry.span("cygnus.phase.gamephase");

        assertEquals(round.getSpanId(), lobbySpan.getParentSpanId());
        assertEquals(round.getSpanId(), gameSpan.getParentSpanId());
        assertEquals(round.getTraceId(), gameSpan.getTraceId(), "all phases belong to the round's trace");
        assertEquals("Lobby", lobbySpan.getAttributes().get(CygnusAttributes.PHASE_NAME));
    }

    @Test
    @DisplayName("A phase ends when it finishes, before the next one starts")
    void phaseEndsBeforeTheNextStarts() {
        this.series.start();
        this.lobby.finish();

        assertEquals(List.of("cygnus.phase.lobby"), this.telemetry.spans().stream().map(SpanData::getName).toList(),
                "only the lobby ended, the round and the game phase are still open");
        SpanData lobbySpan = this.telemetry.span("cygnus.phase.lobby");
        assertTrue(lobbySpan.getEndEpochNanos() > 0);
    }

    @Test
    @DisplayName("The round stays open while the last phase runs")
    void roundStaysOpenUntilTheLastPhaseFinished() {
        this.series.start();
        this.lobby.finish();

        assertEquals(0, this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.SPAN_ROUND)).count());
    }

    @Test
    @DisplayName("Aborting a round, for a shutdown mid-round, ends the open phase and the round")
    void abortEndsOpenSpans() {
        this.series.start();

        this.rounds.abort("shutdown");

        SpanData round = this.telemetry.span(CygnusAttributes.SPAN_ROUND);
        assertEquals("shutdown", round.getAttributes().get(CygnusAttributes.GAME_END_REASON));
        assertTrue(this.telemetry.spans().stream().anyMatch(span -> span.getName().equals("cygnus.phase.lobby")),
                "the open phase span is ended as well");
    }

    @Test
    @DisplayName("Aborting keeps the end reason the game finish already set")
    void abortKeepsAnExistingEndReason() {
        this.series.start();
        this.rounds.gameFinished("TIME_OVER");

        this.rounds.abort("shutdown");

        assertEquals("TIME_OVER", this.telemetry.span(CygnusAttributes.SPAN_ROUND)
                .getAttributes().get(CygnusAttributes.GAME_END_REASON));
    }

    @Test
    @DisplayName("Aborting twice, or without a round, does nothing")
    void abortIsIdempotent() {
        this.rounds.abort("shutdown");
        this.series.start();
        this.rounds.abort("shutdown");
        this.rounds.abort("shutdown");

        assertEquals(1, this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.SPAN_ROUND)).count());
    }

    @Test
    @DisplayName("A page found adds an event with the counts and the finder")
    void pageFoundAddsAnEvent() {
        UUID finder = UUID.randomUUID();
        this.series.start();

        this.rounds.pageFound(finder, 3, 8);
        this.rounds.abort("test");

        EventData event = TestTelemetry.event(this.telemetry.span(CygnusAttributes.SPAN_ROUND),
                CygnusAttributes.EVENT_PAGE_FOUND);
        assertEquals(3L, event.getAttributes().get(CygnusAttributes.PAGES_FOUND));
        assertEquals(8L, event.getAttributes().get(CygnusAttributes.PAGES_MAX));
        assertEquals(finder.toString(), event.getAttributes().get(CygnusAttributes.PLAYER_UUID));
    }

    @Test
    @DisplayName("A death adds an event with the player and the role they had")
    void deathAddsAnEvent() {
        UUID victim = UUID.randomUUID();
        this.series.start();

        this.rounds.playerDied(victim, "survivor");
        this.rounds.abort("test");

        EventData event = TestTelemetry.event(this.telemetry.span(CygnusAttributes.SPAN_ROUND),
                CygnusAttributes.EVENT_PLAYER_DEATH);
        assertEquals(victim.toString(), event.getAttributes().get(CygnusAttributes.PLAYER_UUID));
        assertEquals("survivor", event.getAttributes().get(CygnusAttributes.PLAYER_ROLE));
    }

    @Test
    @DisplayName("A slender revive adds an event with the revived player")
    void slenderReviveAddsAnEvent() {
        UUID slender = UUID.randomUUID();
        this.series.start();

        this.rounds.slenderRevived(slender);
        this.rounds.abort("test");

        EventData event = TestTelemetry.event(this.telemetry.span(CygnusAttributes.SPAN_ROUND),
                CygnusAttributes.EVENT_SLENDER_REVIVE);
        assertEquals(slender.toString(), event.getAttributes().get(CygnusAttributes.PLAYER_UUID));
    }

    @Test
    @DisplayName("The game start adds an event")
    void gameStartAddsAnEvent() {
        this.series.start();

        this.rounds.gameStarted();
        this.rounds.abort("test");

        TestTelemetry.event(this.telemetry.span(CygnusAttributes.SPAN_ROUND), CygnusAttributes.EVENT_GAME_START);
    }

    @Test
    @DisplayName("The game finish adds an event and sets the end reason on the round")
    void gameFinishSetsTheEndReason() {
        this.series.start();

        this.rounds.gameFinished("ALL_PAGES_FOUND");
        this.lobby.finish();
        this.game.finish();

        SpanData round = this.telemetry.span(CygnusAttributes.SPAN_ROUND);
        assertEquals("ALL_PAGES_FOUND", round.getAttributes().get(CygnusAttributes.GAME_END_REASON));
        assertEquals("ALL_PAGES_FOUND", TestTelemetry.event(round, CygnusAttributes.EVENT_GAME_FINISH)
                .getAttributes().get(CygnusAttributes.GAME_END_REASON));
    }

    @Test
    @DisplayName("Events with no round running are dropped without failing")
    void eventsWithoutARoundAreDropped() {
        this.rounds.pageFound(UUID.randomUUID(), 1, 8);
        this.rounds.playerDied(UUID.randomUUID(), "survivor");
        this.rounds.gameStarted();

        assertTrue(this.telemetry.spans().isEmpty());
    }

    @Test
    @DisplayName("A phase that throws while starting still ends its span, marked as failed")
    void failingPhaseStartEndsItsSpanAsError() {
        StubPhase broken = new StubPhase("Broken") {
            @Override
            protected void onStart() {
                throw new IllegalStateException("boom");
            }
        };
        TracedPhaseSeries<Phase> brokenSeries = new TracedPhaseSeries<>("broken", this.rounds);
        brokenSeries.add(broken);

        try {
            brokenSeries.start();
        } catch (IllegalStateException expected) {
            // the exception belongs to the phase, the span is what is under test
        }

        SpanData phase = this.telemetry.span("cygnus.phase.broken");
        assertEquals(StatusCode.ERROR, phase.getStatus().getStatusCode());
        assertTrue(phase.getEvents().stream().anyMatch(event -> event.getName().equals("exception")),
                "the exception is recorded on the span");
        assertNull(phase.getAttributes().get(CygnusAttributes.GAME_END_REASON));
    }

    /**
     * A phase that does nothing and finishes when the test says so.
     */
    private static class StubPhase extends Phase {

        StubPhase(String name) {
            super(name);
        }

        @Override
        protected void onStart() {
        }
    }

    private static io.opentelemetry.api.trace.SpanContext remote(int trace, int span) {
        return io.opentelemetry.api.trace.SpanContext.createFromRemoteParent(
                String.format("%032x", trace), String.format("%016x", span),
                io.opentelemetry.api.trace.TraceFlags.getSampled(), io.opentelemetry.api.trace.TraceState.getDefault());
    }

    @Test
    @DisplayName("Two incoming spans of the same trace are one link")
    void linksAreKeyedOnTheTraceId() {
        this.series.start();

        this.rounds.linkPrevious(remote(1, 1));
        this.rounds.linkPrevious(remote(1, 2));
        this.rounds.abort("test");

        assertEquals(1, this.telemetry.span(CygnusAttributes.SPAN_ROUND).getLinks().size());
    }

    @Test
    @DisplayName("Links are capped, and the ones beyond the cap are counted")
    void linksAreCappedAndTheDroppedAreCounted() {
        this.series.start();

        for (int i = 1; i <= RoundTracer.MAX_LINKS + 4; i++) {
            this.rounds.linkPrevious(remote(i, 1));
        }
        this.rounds.abort("test");

        SpanData round = this.telemetry.span(CygnusAttributes.SPAN_ROUND);
        assertEquals(RoundTracer.MAX_LINKS, round.getLinks().size());
        assertEquals(4L, round.getAttributes().get(CygnusAttributes.LINKS_DROPPED));
    }

    @Test
    @DisplayName("A round that dropped no link has no dropped count")
    void noDroppedCountWithoutDrops() {
        this.series.start();
        this.rounds.linkPrevious(remote(1, 1));
        this.rounds.abort("test");

        assertNull(this.telemetry.span(CygnusAttributes.SPAN_ROUND).getAttributes().get(CygnusAttributes.LINKS_DROPPED));
    }
}
