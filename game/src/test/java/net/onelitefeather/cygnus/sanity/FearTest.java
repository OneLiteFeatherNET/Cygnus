package net.onelitefeather.cygnus.sanity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FearTest {

    private static final double DECAY = 0.005D;
    private static final double EPSILON = 1.0E-9;

    /** A fear without scars or cap, so a test can look at the decay alone. */
    private static Fear calm() {
        return new Fear(DECAY, 0.0D, 1.0D, 0L);
    }

    @Test
    @DisplayName("A new survivor is not scared")
    void startsCalm() {
        assertEquals(0.0D, calm().read(0L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear never goes above 1")
    void gainsClampAtOne() {
        Fear fear = calm();
        fear.add(0.8D, 0L, 0.0D);
        fear.add(0.8D, 0L, 0.0D);

        assertEquals(1.0D, fear.read(0L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear wears off over time")
    void decaysOverTime() {
        Fear fear = calm();
        fear.add(0.3D, 0L, 0.0D);

        assertEquals(0.15D, fear.read(30_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear does not wear off below 0")
    void decayStopsAtZero() {
        Fear fear = calm();
        fear.add(0.1D, 0L, 0.0D);

        assertEquals(0.0D, fear.read(1_000_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Fear does not wear off below the floor")
    void decayStopsAtTheFloor() {
        Fear fear = calm();
        fear.add(0.3D, 0L, 0.0D);

        assertEquals(0.1D, fear.read(120_000L, 0.1D), EPSILON);
    }

    @Test
    @DisplayName("A higher floor lifts the value, and it wears off from there")
    void aHigherFloorLiftsTheStoredValue() {
        Fear fear = calm();

        assertEquals(0.4D, fear.read(0L, 0.4D), EPSILON);
        assertEquals(0.35D, fear.read(10_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("A gain lands on top of the decayed value")
    void gainAfterDecay() {
        Fear fear = calm();
        fear.add(0.3D, 0L, 0.0D);
        fear.add(0.1D, 20_000L, 0.0D);

        assertEquals(0.3D, fear.read(20_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("A clock going backwards does not raise fear")
    void aClockGoingBackwardsDoesNotRaiseFear() {
        Fear fear = calm();
        fear.add(0.3D, 1_000L, 0.0D);

        assertEquals(0.3D, fear.read(500L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Sightings only count once per cooldown")
    void sightingCooldown() {
        Fear fear = calm();

        assertTrue(fear.trySighting(0L, 20_000L));
        assertFalse(fear.trySighting(10_000L, 20_000L));
        assertTrue(fear.trySighting(20_000L, 20_000L));
    }

    @Test
    @DisplayName("A share of every jump never wears off")
    void jumpsLeaveAScar() {
        Fear fear = new Fear(DECAY, 0.25D, 1.0D, 0L);
        fear.add(0.4D, 0L, 0.0D);

        assertEquals(0.1D, fear.read(1_000_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("The scar sits on top of the floor")
    void scarAddsToTheFloor() {
        Fear fear = new Fear(DECAY, 0.25D, 1.0D, 0L);
        fear.add(0.4D, 0L, 0.0D);

        assertEquals(0.3D, fear.read(1_000_000L, 0.2D), EPSILON);
    }

    @Test
    @DisplayName("Floor and scar together stay below the cap, a jump does not")
    void floorAndScarAreCapped() {
        Fear fear = new Fear(DECAY, 1.0D, 0.55D, 0L);
        fear.add(0.4D, 0L, 0.0D);

        assertEquals(0.55D, fear.read(1_000_000L, 0.5D), EPSILON);
        fear.add(0.3D, 1_000_000L, 0.5D);
        assertEquals(0.85D, fear.read(1_000_000L, 0.5D), EPSILON);
    }

    @Test
    @DisplayName("Being stalked raises fear by the time since the last report")
    void stalkingGrowsFear() {
        Fear fear = new Fear(0.0D, 0.0D, 1.0D, 0L);
        fear.stalked(0.02D, 0L, 0.0D);
        fear.stalked(0.02D, 500L, 0.0D);
        fear.stalked(0.02D, 1_000L, 0.0D);

        assertEquals(0.02D, fear.read(1_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("The first report of a stalk adds nothing yet")
    void firstStalkReportAddsNothing() {
        Fear fear = calm();
        fear.stalked(0.02D, 5_000L, 0.0D);

        assertEquals(0.0D, fear.read(5_000L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("A gap between two stalks is not counted as stalking")
    void stalkGapIsNotCounted() {
        Fear fear = calm();
        fear.stalked(0.02D, 0L, 0.0D);
        fear.stalked(0.02D, Fear.STALK_GAP_MILLIS + 1L, 0.0D);

        assertEquals(0.0D, fear.read(Fear.STALK_GAP_MILLIS + 1L, 0.0D), EPSILON);
    }

    @Test
    @DisplayName("Stalking leaves no scar")
    void stalkingLeavesNoScar() {
        Fear fear = new Fear(DECAY, 0.25D, 1.0D, 0L);
        fear.stalked(0.5D, 0L, 0.0D);
        fear.stalked(0.5D, 1_000L, 0.0D);

        assertEquals(0.0D, fear.read(1_000_000L, 0.0D), EPSILON);
    }
}
