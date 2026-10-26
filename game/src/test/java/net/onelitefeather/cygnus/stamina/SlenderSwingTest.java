package net.onelitefeather.cygnus.stamina;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlenderSwingTest {

    private static final double DELTA = 1.0E-9D;

    @Test
    @DisplayName("A calm survivor is missed in one of twenty swings and takes 0.6 to 1.25 hearts")
    void calmSurvivorBounds() {
        assertEquals(0.05D, SlenderSwing.missChance(0.0D), DELTA);
        assertEquals(0.60D, SlenderSwing.minHearts(0.0D), DELTA);
        assertEquals(1.25D, SlenderSwing.maxHearts(0.0D), DELTA);
    }

    @Test
    @DisplayName("A terrified survivor is missed in three of twenty swings and takes 0.5 to 1 heart")
    void terrifiedSurvivorBounds() {
        assertEquals(0.15D, SlenderSwing.missChance(1.0D), DELTA);
        assertEquals(0.50D, SlenderSwing.minHearts(1.0D), DELTA);
        assertEquals(1.00D, SlenderSwing.maxHearts(1.0D), DELTA);
    }

    @Test
    @DisplayName("Fear in between blends both ends")
    void halfwayBlends() {
        assertEquals(0.10D, SlenderSwing.missChance(0.5D), DELTA);
        assertEquals(0.55D, SlenderSwing.minHearts(0.5D), DELTA);
        assertEquals(1.125D, SlenderSwing.maxHearts(0.5D), DELTA);
    }

    @Test
    @DisplayName("Fear outside 0 to 1 never pushes the swing past its ends")
    void fearIsClamped() {
        assertEquals(SlenderSwing.missChance(1.0D), SlenderSwing.missChance(3.0D), DELTA);
        assertEquals(SlenderSwing.maxHearts(0.0D), SlenderSwing.maxHearts(-1.0D), DELTA);
    }

    @Test
    @DisplayName("A roll below the miss chance misses")
    void rollBelowMissChanceMisses() {
        assertEquals(0.0D, new SlenderSwing(() -> new FixedRandom(0.04D)).roll(0.0D));
    }

    @Test
    @DisplayName("A roll at the miss chance hits with the lowest damage")
    void rollAtMissChanceHits() {
        assertEquals(0.60D, new SlenderSwing(() -> new FixedRandom(0.05D, 0.0D)).roll(0.0D), DELTA);
    }

    @Test
    @DisplayName("A hit spreads over the damage range")
    void hitSpreadsOverTheRange() {
        assertEquals(1.25D, new SlenderSwing(() -> new FixedRandom(0.99D, 1.0D)).roll(0.0D), DELTA);
        assertEquals(0.75D, new SlenderSwing(() -> new FixedRandom(0.99D, 0.5D)).roll(1.0D), DELTA);
    }

    @Test
    @DisplayName("The same roll hits a calm survivor but misses a terrified one")
    void moreFearMissesMore() {
        assertTrue(new SlenderSwing(() -> new FixedRandom(0.1D, 0.0D)).roll(0.0D) > 0.0D);
        assertEquals(0.0D, new SlenderSwing(() -> new FixedRandom(0.1D)).roll(1.0D));
    }
}
