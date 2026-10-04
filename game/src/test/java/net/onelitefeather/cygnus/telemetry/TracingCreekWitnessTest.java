package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.telemetry.ActionTracer.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the creek witness decorator.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class TracingCreekWitnessTest {

    private static final UUID SURVIVOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private TestTelemetry telemetry;
    private List<String> heard;
    private TracingCreekWitness witness;
    private RoundTracer rounds;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        this.rounds = new RoundTracer(this.telemetry.tracing(), () -> "round-1");
        rounds.roundStarted();
        ActionTracer actions = new ActionTracer(rounds, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), () -> "Cabin");
        this.heard = new ArrayList<>();
        CreekWitness recorder = new CreekWitness() {
            @Override
            public void sighted(UUID survivor) {
                heard.add("sighted");
            }

            @Override
            public void caught(UUID survivor) {
                heard.add("caught");
            }

            @Override
            public void stalked(UUID survivor) {
                heard.add("stalked");
            }

            @Override
            public void selected(UUID survivor) {
                heard.add("selected");
            }
        };
        this.witness = new TracingCreekWitness(recorder, actions,
                id -> id.equals(SURVIVOR) ? new Actor(SURVIVOR, "survivor", 1, 64, 2) : null);
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    private long spansNamed(String name) {
        return this.telemetry.spans().stream().map(SpanData::getName).filter(name::equals).count();
    }

    @Test
    @DisplayName("Every event is passed on to the real witness")
    void delegateHearsEverything() {
        this.witness.sighted(SURVIVOR);
        this.witness.selected(SURVIVOR);
        this.witness.stalked(SURVIVOR);
        this.witness.caught(SURVIVOR);

        assertEquals(List.of("sighted", "selected", "stalked", "caught"), this.heard);
    }

    @Test
    @DisplayName("A sighting, a selection and a catch are one action each")
    void eventsBecomeActions() {
        this.witness.sighted(SURVIVOR);
        this.witness.selected(SURVIVOR);
        this.witness.caught(SURVIVOR);

        assertEquals(1, spansNamed(CygnusAttributes.ACTION_CREEK_SIGHTED));
        assertEquals(1, spansNamed(CygnusAttributes.ACTION_CREEK_SELECTED));
        assertEquals(1, spansNamed(CygnusAttributes.ACTION_CREEK_CAUGHT));
    }

    @Test
    @DisplayName("A stalk is one action however many steps it takes")
    void stalkIsTracedOnce() {
        for (int i = 0; i < 50; i++) {
            this.witness.stalked(SURVIVOR);
        }

        assertEquals(1, spansNamed(CygnusAttributes.ACTION_CREEK_STALK));
        assertEquals(50, this.heard.size(), "the real witness still hears every step");
    }

    @Test
    @DisplayName("After a catch the next stalk is traced again")
    void catchEndsTheStalk() {
        this.witness.stalked(SURVIVOR);
        this.witness.caught(SURVIVOR);
        this.witness.stalked(SURVIVOR);

        assertEquals(2, spansNamed(CygnusAttributes.ACTION_CREEK_STALK));
    }

    @Test
    @DisplayName("A survivor who is gone is not traced, and the real witness still hears about it")
    void unknownSurvivorIsSkipped() {
        this.witness.caught(UUID.randomUUID());

        assertEquals(0, spansNamed(CygnusAttributes.ACTION_CREEK_CAUGHT));
        assertEquals(List.of("caught"), this.heard);
    }

    @Test
    @DisplayName("A survivor stalked again in the next round gets a first-step span again")
    void stalkIsTracedAgainInTheNextRound() {
        this.witness.stalked(SURVIVOR);

        this.rounds.roundStarted();
        this.witness.stalked(SURVIVOR);

        assertEquals(2, spansNamed(CygnusAttributes.ACTION_CREEK_STALK));
    }
}
