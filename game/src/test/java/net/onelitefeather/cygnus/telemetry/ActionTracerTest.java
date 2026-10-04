package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.telemetry.ActionTracer.Actor;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the action spans: that each one is a child of the round, and that it can be found by type
 * (the span name), player (UUID and role) and place (position and map).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class ActionTracerTest {

    private static final UUID SURVIVOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SLENDER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID PAGE = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    private TestTelemetry telemetry;
    private RoundTracer rounds;
    private MutableClock clock;
    private @Nullable String map;
    private ActionTracer actions;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        this.clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        this.map = "Cabin";
        this.actions = new ActionTracer(this.rounds, this.clock, () -> this.map);
        this.rounds.roundStarted();
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    private static Actor survivor(double x, double y, double z) {
        return new Actor(SURVIVOR, "survivor", x, y, z);
    }

    private static Actor slender(double x, double y, double z) {
        return new Actor(SLENDER, "slender", x, y, z);
    }

    @Test
    @DisplayName("An action is a child of the round span")
    void actionIsAChildOfTheRound() {
        this.actions.slenderRevived(slender(0, 0, 0));
        this.rounds.abort("test");

        SpanData round = this.telemetry.span(CygnusAttributes.SPAN_ROUND);
        SpanData action = this.telemetry.span(CygnusAttributes.ACTION_SLENDER_REVIVE);
        assertEquals(round.getSpanId(), action.getParentSpanId());
        assertEquals(round.getTraceId(), action.getTraceId());
    }

    @Test
    @DisplayName("With no round running an action creates no span")
    void noRoundNoSpan() {
        this.rounds.abort("test");
        int before = this.telemetry.spans().size();

        this.actions.slenderRevived(slender(0, 0, 0));

        assertEquals(before, this.telemetry.spans().size());
    }

    @Test
    @DisplayName("An action carries the player, the role, the rounded position and the map")
    void actionCarriesWhoAndWhere() {
        this.actions.spectatorJoined(new Actor(SURVIVOR, "spectator", 10.04, 64.0, -3.26));

        SpanData action = this.telemetry.span(CygnusAttributes.ACTION_SPECTATOR_JOIN);
        assertEquals(SURVIVOR.toString(), action.getAttributes().get(CygnusAttributes.PLAYER_UUID));
        assertEquals("spectator", action.getAttributes().get(CygnusAttributes.PLAYER_ROLE));
        assertEquals(10.0D, action.getAttributes().get(CygnusAttributes.POSITION_X));
        assertEquals(64.0D, action.getAttributes().get(CygnusAttributes.POSITION_Y));
        assertEquals(-3.3D, action.getAttributes().get(CygnusAttributes.POSITION_Z));
        assertEquals("Cabin", action.getAttributes().get(CygnusAttributes.MAP));
    }

    @Test
    @DisplayName("Without a loaded map the action has no map attribute")
    void noMapNoAttribute() {
        this.map = null;

        this.actions.spectatorJoined(survivor(0, 0, 0));

        assertNull(this.telemetry.span(CygnusAttributes.ACTION_SPECTATOR_JOIN)
                .getAttributes().get(CygnusAttributes.MAP));
    }

    @Test
    @DisplayName("A page spawn names the page, its spot and whether it was moved")
    void pageSpawn() {
        this.actions.pageSpawned(PAGE, new Pos(1.26, 65, 2), true);

        SpanData span = this.telemetry.span(CygnusAttributes.ACTION_PAGE_SPAWN);
        assertEquals(PAGE.toString(), span.getAttributes().get(CygnusAttributes.PAGE_ID));
        assertEquals(1.3D, span.getAttributes().get(CygnusAttributes.POSITION_X));
        assertEquals(Boolean.TRUE, span.getAttributes().get(CygnusAttributes.PAGE_RELOCATED));
    }

    @Test
    @DisplayName("A found page records who, where, the index, the maximum, the time it was out and the distance")
    void pageFound() {
        this.actions.pageSpawned(PAGE, new Pos(0, 64, 0), false);
        this.clock.advance(Duration.ofMillis(4500));

        this.actions.pageFound(survivor(3, 64, 4), PAGE, 2, 8);

        SpanData span = this.telemetry.span(CygnusAttributes.ACTION_PAGE_FOUND);
        assertEquals(2L, span.getAttributes().get(CygnusAttributes.PAGE_INDEX));
        assertEquals(8L, span.getAttributes().get(CygnusAttributes.PAGES_MAX));
        assertEquals(4500L, span.getAttributes().get(CygnusAttributes.PAGE_OUT_MS));
        assertEquals(5.0D, span.getAttributes().get(CygnusAttributes.DISTANCE));
        assertEquals(0.0D, span.getAttributes().get(CygnusAttributes.PAGE_SPOT_X));
        assertEquals(SURVIVOR.toString(), span.getAttributes().get(CygnusAttributes.PLAYER_UUID));
        assertEquals(3.0D, span.getAttributes().get(CygnusAttributes.POSITION_X));
    }

    @Test
    @DisplayName("A found page whose spawn was not seen still produces a span, without the time it was out")
    void pageFoundUnknownPage() {
        this.actions.pageFound(survivor(0, 0, 0), UUID.randomUUID(), 1, 8);

        SpanData span = this.telemetry.span(CygnusAttributes.ACTION_PAGE_FOUND);
        assertNull(span.getAttributes().get(CygnusAttributes.PAGE_OUT_MS));
        assertNull(span.getAttributes().get(CygnusAttributes.DISTANCE));
    }

    @Test
    @DisplayName("A found page without an id still produces a span")
    void pageFoundWithoutId() {
        this.actions.pageFound(survivor(0, 0, 0), null, 1, 8);

        assertEquals(1L, this.telemetry.span(CygnusAttributes.ACTION_PAGE_FOUND)
                .getAttributes().get(CygnusAttributes.PAGE_INDEX));
    }

    @Test
    @DisplayName("A page that expired records how long it was out")
    void pageExpired() {
        this.actions.pageSpawned(PAGE, new Pos(5, 64, 5), false);
        this.clock.advance(Duration.ofSeconds(60));

        this.actions.pageExpired(PAGE);

        SpanData span = this.telemetry.span(CygnusAttributes.ACTION_PAGE_EXPIRED);
        assertEquals(60_000L, span.getAttributes().get(CygnusAttributes.PAGE_OUT_MS));
        assertEquals(5.0D, span.getAttributes().get(CygnusAttributes.POSITION_X));
    }

    @Test
    @DisplayName("A survivor's death names the slender as killer and the distance between them")
    void survivorDeath() {
        this.actions.playerDied(survivor(0, 64, 0), slender(6, 64, 8));

        SpanData span = this.telemetry.span(CygnusAttributes.ACTION_PLAYER_DEATH);
        assertEquals(SLENDER.toString(), span.getAttributes().get(CygnusAttributes.KILLER_UUID));
        assertEquals(10.0D, span.getAttributes().get(CygnusAttributes.DISTANCE));
        assertEquals("survivor", span.getAttributes().get(CygnusAttributes.PLAYER_ROLE));
    }

    @Test
    @DisplayName("A death with no slender in the round names no killer")
    void deathWithoutSlender() {
        this.actions.playerDied(survivor(0, 64, 0), null);

        SpanData span = this.telemetry.span(CygnusAttributes.ACTION_PLAYER_DEATH);
        assertNull(span.getAttributes().get(CygnusAttributes.KILLER_UUID));
        assertNull(span.getAttributes().get(CygnusAttributes.DISTANCE));
    }

    @Test
    @DisplayName("The slender dying is no kill")
    void slenderDeathHasNoKiller() {
        this.actions.playerDied(slender(0, 64, 0), slender(0, 64, 0));

        assertNull(this.telemetry.span(CygnusAttributes.ACTION_PLAYER_DEATH)
                .getAttributes().get(CygnusAttributes.KILLER_UUID));
    }

    @Test
    @DisplayName("A stamina state change of the slender names the state")
    void staminaState() {
        this.actions.staminaState(slender(0, 0, 0), "DRAINING");

        assertEquals("DRAINING", this.telemetry.span(CygnusAttributes.ACTION_SLENDER_STAMINA)
                .getAttributes().get(CygnusAttributes.STAMINA_STATE));
    }

    @Test
    @DisplayName("Accepting and declining the disclaimer are separate action types")
    void disclaimerAnswers() {
        this.actions.disclaimer(survivor(0, 0, 0), true);
        this.actions.disclaimer(new Actor(SLENDER, "slender", 0, 0, 0), false);

        this.telemetry.span(CygnusAttributes.ACTION_DISCLAIMER_ACKNOWLEDGE);
        this.telemetry.span(CygnusAttributes.ACTION_DISCLAIMER_DECLINE);
    }

    @Test
    @DisplayName("Every action span ends")
    void actionsEnd() {
        this.actions.creek(CygnusAttributes.ACTION_CREEK_CAUGHT, survivor(0, 0, 0));

        assertTrue(this.telemetry.span(CygnusAttributes.ACTION_CREEK_CAUGHT).hasEnded());
    }

    /** A clock that only moves when the test says so. */
    static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

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
            return this.now;
        }
    }

    @Test
    @DisplayName("A player's disclaimer answer is traced once per round, however often the packet arrives")
    void disclaimerIsTracedOncePerPlayerAndRound() {
        for (int i = 0; i < 100; i++) {
            this.actions.disclaimer(survivor(0, 0, 0), true);
        }
        this.actions.disclaimer(survivor(0, 0, 0), false);

        assertEquals(1, this.telemetry.spans().stream()
                .filter(span -> span.getName().startsWith("cygnus.action.disclaimer")).count());
    }

    @Test
    @DisplayName("Another player's answer is traced separately")
    void disclaimerOfAnotherPlayerIsTraced() {
        this.actions.disclaimer(survivor(0, 0, 0), true);
        this.actions.disclaimer(new Actor(SLENDER, "slender", 0, 0, 0), true);

        assertEquals(2, this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.ACTION_DISCLAIMER_ACKNOWLEDGE)).count());
    }

    @Test
    @DisplayName("The next round traces a player's answer again")
    void disclaimerIsTracedAgainInTheNextRound() {
        this.actions.disclaimer(survivor(0, 0, 0), true);

        this.rounds.roundStarted();
        this.actions.disclaimer(survivor(0, 0, 0), true);

        assertEquals(2, this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.ACTION_DISCLAIMER_ACKNOWLEDGE)).count());
    }

    @Test
    @DisplayName("The pages of a finished round are forgotten, so they cannot date a page of the next one")
    void pagesAreForgottenWhenARoundEnds() {
        this.actions.pageSpawned(PAGE, new Pos(0, 64, 0), false);
        this.rounds.abort("test");
        this.rounds.roundStarted();
        this.clock.advance(Duration.ofSeconds(30));

        this.actions.pageFound(survivor(0, 64, 0), PAGE, 1, 8);

        assertNull(this.telemetry.span(CygnusAttributes.ACTION_PAGE_FOUND)
                .getAttributes().get(CygnusAttributes.PAGE_OUT_MS));
    }

    @Test
    @DisplayName("A listener body that throws does not escape")
    void safelySwallowsFailures() {
        ActionTracer.safely(() -> {
            throw new IllegalStateException("player is mid-disconnect");
        });
    }
}
