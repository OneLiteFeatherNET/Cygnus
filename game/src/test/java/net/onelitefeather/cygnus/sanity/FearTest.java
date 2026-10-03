package net.onelitefeather.cygnus.sanity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FearTest {

    private static final double DECAY = 0.005D;
    private static final double EPSILON = 1.0E-9;

    @Test
    @DisplayName("A new survivor is not scared")
    void startsCalm() {
        assertEquals(0.0D, new Fear(DECAY, 0L).read(0L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear never goes above 1")
    void gainsClampAtOne() {
        Fear fear = new Fear(DECAY, 0L);
        fear.add(0.8D, 0L, 0.0D);
        fear.add(0.8D, 0L, 0.0D);

        assertEquals(1.0D, fear.read(0L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear wears off over time")
    void decaysOverTime() {
        Fear fear = new Fear(DECAY, 0L);
        fear.add(0.3D, 0L, 0.0D);

        assertEquals(0.15D, fear.read(30_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear does not wear off below 0")
    void decayStopsAtZero() {
        Fear fear = new Fear(DECAY, 0L);
        fear.add(0.1D, 0L, 0.0D);

        assertEquals(0.0D, fear.read(1_000_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear does not wear off below the floor")
    void decayStopsAtTheFloor() {
        Fear fear = new Fear(DECAY, 0L);
        fear.add(0.3D, 0L, 0.0D);

        assertEquals(0.1D, fear.read(120_000L, 0.1D), EPSILON);
    }

    @Test
    @DisplayName("A higher floor lifts the value, and it wears off from there")
    void aHigherFloorLiftsTheStoredValue() {
        Fear fear = new Fear(DECAY, 0L);

        assertEquals(0.4D, fear.read(0L, 0.4D), EPSILON);
        assertEquals(0.35D, fear.read(10_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("A gain lands on top of the decayed value")
    void gainAfterDecay() {
        Fear fear = new Fear(DECAY, 0L);
        fear.add(0.3D, 0L, 0.0D);
        fear.add(0.1D, 20_000L, 0.0D);

        assertEquals(0.3D, fear.read(20_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("A clock going backwards does not raise fear")
    void aClockGoingBackwardsDoesNotRaiseFear() {
        Fear fear = new Fear(DECAY, 0L);
        fear.add(0.3D, 1_000L, 0.0D);

        assertEquals(0.3D, fear.read(500L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Sightings only count once per cooldown")
    void sightingCooldown() {
        Fear fear = new Fear(DECAY, 0L);

        assertTrue(fear.trySighting(0L, 20_000L));
        assertFalse(fear.trySighting(10_000L, 20_000L));
        assertTrue(fear.trySighting(20_000L, 20_000L));
    }
}
