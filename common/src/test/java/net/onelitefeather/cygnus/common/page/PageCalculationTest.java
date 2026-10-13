package net.onelitefeather.cygnus.common.page;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageCalculationTest {

    private static final int MAX_SURVIVORS = 3;

    /**
     * A random source that always answers with the highest value of the asked range, or the
     * lowest, and checks that the range is the expected one.
     */
    private static RandomGenerator fixed(int expectedBound, boolean highest) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                assertEquals(expectedBound, bound, "the random part was drawn from the wrong range");
                return highest ? bound - 1 : 0;
            }
        };
    }

    @Test
    @DisplayName("An Eight Pages round has 8 pages plus 0 to 2")
    void eightPagesRange() {
        for (int survivors = 0; survivors <= MAX_SURVIVORS; survivors++) {
            assertEquals(8, PageCalculation.calculatePageAmount(survivors, MAX_SURVIVORS, fixed(3, false)));
            assertEquals(10, PageCalculation.calculatePageAmount(survivors, MAX_SURVIVORS, fixed(3, true)));
        }
    }

    @Test
    @DisplayName("A normal round builds on the 8 pages with 4 per survivor above the boundary, plus 0 to 4")
    void normalRange() {
        assertEquals(12, PageCalculation.calculatePageAmount(4, MAX_SURVIVORS, fixed(5, false)));
        assertEquals(16, PageCalculation.calculatePageAmount(4, MAX_SURVIVORS, fixed(5, true)));
        assertEquals(16, PageCalculation.calculatePageAmount(5, MAX_SURVIVORS, fixed(5, false)));
        assertEquals(28, PageCalculation.calculatePageAmount(8, MAX_SURVIVORS, fixed(5, false)));
        assertEquals(44, PageCalculation.calculatePageAmount(12, MAX_SURVIVORS, fixed(5, false)));
        assertEquals(48, PageCalculation.calculatePageAmount(12, MAX_SURVIVORS, fixed(5, true)));
    }

    @Test
    @DisplayName("Moving the boundary moves the start of the normal formula with it")
    void anchoredToTheBoundary() {
        assertEquals(10, PageCalculation.calculatePageAmount(2, 2, fixed(3, true)));
        assertEquals(12, PageCalculation.calculatePageAmount(3, 2, fixed(5, false)));
    }

    @Test
    @DisplayName("The page count varies across rounds and stays in range")
    void variesAcrossRounds() {
        Set<Integer> observed = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            observed.add(PageCalculation.calculatePageAmount(12, MAX_SURVIVORS));
        }

        assertTrue(observed.size() > 1, "observed " + observed);
        observed.forEach(value -> assertTrue(value >= 44 && value <= 48, "was " + value));
    }

    @Test
    void testActivePageCalculationWithoutScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        for (int i = 0; i < 3; i++) {
            env.createPlayer(instance);
        }

        assertEquals(GameConfig.MIN_ACTIVE_PAGE_COUNT, PageCalculation.calculateActivePageAmount());

        env.destroyInstance(instance, true);
    }

    @Test
    void testActivePageCalculationStaysFlatUntilNearTheTopOfTheRange(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        for (int i = 0; i < 11; i++) {
            env.createPlayer(instance);
        }

        assertEquals(GameConfig.MIN_ACTIVE_PAGE_COUNT, PageCalculation.calculateActivePageAmount(),
                "the active page count must still be at the minimum just below the top of the range");

        env.destroyInstance(instance, true);
    }

    @Test
    void testActivePageCalculationWithScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        for (int i = 0; i < 13; i++) {
            env.createPlayer(instance);
        }

        assertEquals(GameConfig.MIN_ACTIVE_PAGE_COUNT + 1, PageCalculation.calculateActivePageAmount(),
                "13 players (12 survivors) add exactly one page on top of the minimum");

        env.destroyInstance(instance, true);
    }
}
