package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.creek.state.CreekMood;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoodClockTest {

    private static final long GAME_TIME = 900_000L;

    private static MoodClock started(RandomGenerator random) {
        MoodClock clock = new MoodClock(GAME_TIME, random);
        clock.start(0L);
        return clock;
    }

    /** Answers every bounded int with the given value, and every long with 0. */
    private static RandomGenerator picking(int value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

    @Test
    @DisplayName("The switch comes between a quarter and half of the game time")
    void switchesInsideTheWindow() {
        for (long seed = 0; seed < 50; seed++) {
            long at = started(new Random(seed)).switchAt();
            assertTrue(at >= 225_000L && at <= 450_000L, "switch at " + at);
        }
    }

    @Test
    @DisplayName("The creek is normal until the switch, then in one of the moods, and it switches only once")
    void normalUntilTheSwitch() {
        MoodClock clock = started(new Random(3));

        assertFalse(clock.tick(clock.switchAt() - 1L));
        assertEquals(CreekMood.NORMAL, clock.get());
        assertTrue(clock.tick(clock.switchAt()));
        assertNotEquals(CreekMood.NORMAL, clock.get());
        assertFalse(clock.tick(clock.switchAt() + 1L));
    }

    @Test
    @DisplayName("A found page pulls the switch forward by a tenth of the time still left")
    void pagesPullTheSwitchForward() {
        MoodClock clock = started(new Random(3));
        long before = clock.switchAt();

        clock.pageFound(100_000L);

        assertEquals(100_000L + Math.round((before - 100_000L) * 0.9D), clock.switchAt());
    }

    @Test
    @DisplayName("However many pages are found, the switch never moves into the past")
    void pagesNeverPullIntoThePast() {
        MoodClock clock = started(new Random(3));

        for (int page = 0; page < 50; page++) {
            clock.pageFound(200_000L);
        }

        assertTrue(clock.switchAt() >= 200_000L);
    }

    @Test
    @DisplayName("Pages found after the switch change nothing")
    void pagesAfterSwitchDoNothing() {
        MoodClock clock = started(new Random(3));
        clock.tick(clock.switchAt());
        CreekMood mood = clock.get();
        long at = clock.switchAt();

        clock.pageFound(at + 1000L);

        assertEquals(mood, clock.get());
        assertEquals(at, clock.switchAt());
    }

    @Test
    @DisplayName("After a reset the creek is normal and does not switch until the next start")
    void resetForgetsTheRound() {
        MoodClock clock = started(new Random(3));
        clock.tick(clock.switchAt());

        clock.reset();

        assertEquals(CreekMood.NORMAL, clock.get());
        assertFalse(clock.tick(Long.MAX_VALUE));
        clock.start(0L);
        assertTrue(clock.tick(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Each of the three moods can come up")
    void picksEachMood() {
        List<CreekMood> moods = new ArrayList<>();
        for (int roll = 0; roll < 3; roll++) {
            MoodClock clock = started(picking(roll));
            clock.tick(clock.switchAt());
            moods.add(clock.get());
        }

        assertEquals(List.of(CreekMood.HUNTER, CreekMood.SHADOW, CreekMood.TRICKSTER), moods);
    }
}
