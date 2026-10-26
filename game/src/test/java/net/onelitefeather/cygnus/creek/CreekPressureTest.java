package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.state.Calm;
import net.onelitefeather.cygnus.creek.state.CreekMood;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekPressureTest {

    private final MoodClock clock = new MoodClock(900_000L, new Random(5));
    private final CreekPressure pressure = new CreekPressure(CreekConfig.DEFAULT, this.clock);

    @Test
    @DisplayName("While nobody dies and the mood stays, every step gets the very same values")
    void sameTuningWhileNothingChanges() {
        this.pressure.start(0L);
        this.pressure.step(4, 100L);
        var first = this.pressure.tuning();

        this.pressure.step(4, 200L);

        assertSame(first, this.pressure.tuning());
    }

    @Test
    @DisplayName("When a survivor dies, the calm grows and the values follow")
    void newTuningWhenSomeoneDies() {
        this.pressure.start(0L);
        this.pressure.step(4, 100L);
        var full = this.pressure.tuning();

        this.pressure.step(3, 200L);

        assertNotSame(full, this.pressure.tuning());
        assertEquals(Calm.of(4, 3), this.pressure.tuning().calm());
    }

    @Test
    @DisplayName("Survivors who are still joining do not count as a thinned out round")
    void joiningKeepsTheRoundFull() {
        this.pressure.start(0L);
        this.pressure.step(2, 100L);
        this.pressure.step(4, 200L);

        assertEquals(Calm.NONE, this.pressure.tuning().calm());
    }

    @Test
    @DisplayName("The step of the switch says so, and from then on the values carry the new mood")
    void switchChangesTheMood() {
        this.pressure.start(0L);

        assertFalse(this.pressure.step(4, this.clock.switchAt() - 1L));
        assertEquals(CreekMood.NORMAL, this.pressure.tuning().mood());
        assertTrue(this.pressure.step(4, this.clock.switchAt()));
        assertEquals(this.clock.get(), this.pressure.tuning().mood());
        assertFalse(this.pressure.tuning().mood() == CreekMood.NORMAL);
    }

    @Test
    @DisplayName("Stopping forgets the round: normal mood and no calm")
    void stopForgetsTheRound() {
        this.pressure.start(0L);
        this.pressure.step(4, 100L);
        this.pressure.step(1, this.clock.switchAt());

        this.pressure.stop();

        assertEquals(CreekMood.NORMAL, this.pressure.tuning().mood());
        assertEquals(Calm.NONE, this.pressure.tuning().calm());
    }

    @Test
    @DisplayName("A found page reaches the mood clock")
    void pagesReachTheClock() {
        this.pressure.start(0L);
        long before = this.clock.switchAt();

        this.pressure.pageFound(1000L);

        assertTrue(this.clock.switchAt() < before);
    }
}
