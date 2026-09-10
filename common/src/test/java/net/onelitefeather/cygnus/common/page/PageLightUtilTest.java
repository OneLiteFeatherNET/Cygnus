package net.onelitefeather.cygnus.common.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageLightUtilTest {

    @Test
    void testRandomInitialBlockLightDefaultBounds() {
        for (int i = 0; i < 100; i++) {
            int light = PageLightUtil.randomInitialBlockLight();
            assertTrue(light >= PageLightUtil.DEFAULT_MIN_INITIAL_BLOCK_LIGHT,
                    "Light must be >= " + PageLightUtil.DEFAULT_MIN_INITIAL_BLOCK_LIGHT);
            assertTrue(light <= PageLightUtil.DEFAULT_MAX_INITIAL_BLOCK_LIGHT,
                    "Light must be <= " + PageLightUtil.DEFAULT_MAX_INITIAL_BLOCK_LIGHT);
        }
    }

    @Test
    void testRandomInitialBlockLightCustomBounds() {
        for (int i = 0; i < 100; i++) {
            int light = PageLightUtil.randomInitialBlockLight(5, 8);
            assertTrue(light >= 5 && light <= 8, "Light must be in [5, 8]");
        }

        // When min == max
        assertEquals(10, PageLightUtil.randomInitialBlockLight(10, 10));
        // When min > max, bounds are normalized
        int swapped = PageLightUtil.randomInitialBlockLight(10, 5);
        assertTrue(swapped >= 5 && swapped <= 10);
    }

    @ParameterizedTest
    @CsvSource({
            // initial, min, currentTick, ttl, expected
            "15, 1, 0, 60, 15",   // at 0% elapsed -> full initial light
            "15, 1, 30, 60, 8",   // at 50% elapsed -> midpoint: 1 + 0.5 * 14 = 8
            "15, 1, 60, 60, 1",   // at 100% elapsed -> min light
            "15, 1, 70, 60, 1",   // past ttl -> min light
            "12, 0, 0, 60, 12",   // at 0% with min 0 -> 12
            "12, 0, 30, 60, 6",   // at 50% with min 0 -> 6
            "12, 0, 60, 60, 0",   // at 100% with min 0 -> 0
            "15, 1, -5, 60, 15",  // negative current tick -> clamp to initial
            "15, 1, 10, 0, 1",    // 0 or negative ttl -> min light
            "15, 1, 10, -10, 1"   // negative ttl -> min light
    })
    void testCalculateBlockLight(int initial, int min, int currentTick, int ttl, int expected) {
        assertEquals(expected, PageLightUtil.calculateBlockLight(initial, min, currentTick, ttl));
    }

    @Test
    void testCalculateBlockLightWithDefaultMin() {
        assertEquals(15, PageLightUtil.calculateBlockLight(15, 0, 60));
        assertEquals(PageLightUtil.DEFAULT_MIN_BLOCK_LIGHT, PageLightUtil.calculateBlockLight(15, 60, 60));
    }
}
