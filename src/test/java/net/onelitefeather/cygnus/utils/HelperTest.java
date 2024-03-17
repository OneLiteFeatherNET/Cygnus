package net.onelitefeather.cygnus.utils;

import net.minestom.server.utils.MathUtils;
import net.onelitefeather.cygnus.utils.Helper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class HelperTest {

    @Test
    void testGetRandomPitchValue() {
        float pitchValue = Helper.getRandomPitchValue();
        assertNotEquals(0.0f, pitchValue);
        assertTrue(MathUtils.isBetween(pitchValue, .1f, 1.0f));
    }

    @Test
    void testOffsetTimeCreation() {
        int baseTime = 120;
        var timesCases = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            timesCases.add(Helper.calculateOffsetTime(baseTime));
        }

        assertFalse(timesCases.isEmpty());

        for (int i = 0; i < timesCases.size(); i++) {
            assertNotEquals(baseTime, timesCases.get(i));
        }
    }

    @Test
    void testRandomInt() {
        int maximumValue = 21;
        assertNotEquals(0, Helper.getRandomInt(maximumValue));
        assertNotEquals(0, Helper.getRandomInt(maximumValue));
        assertNotEquals(0, Helper.getRandomInt(maximumValue));
        assertNotEquals(0, Helper.getRandomInt(maximumValue));
        assertNotEquals(0, Helper.getRandomInt(maximumValue));
        assertNotEquals(0, Helper.getRandomInt(maximumValue));
    }
}
