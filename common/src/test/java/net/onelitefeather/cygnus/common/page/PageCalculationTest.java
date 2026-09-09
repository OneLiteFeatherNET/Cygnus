package net.onelitefeather.cygnus.common.page;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageCalculationTest {

    /**
     * The page count carries an unpredictable +0..+2 on top of the base amount (see
     * {@link #testPageCalculationVariesAcrossRounds}), so exact-value assertions elsewhere in this
     * class check a range instead of a single number.
     */
    private static final int MAX_JITTER = 2;

    @Test
    void testPageCalculationWithoutScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 3; i++) {
            env.createPlayer(instance);
        }

        int pageCount = PageCalculation.calculatePageAmount();
        assertTrue(pageCount >= GameConfig.MIN_PAGE_COUNT && pageCount <= GameConfig.MIN_PAGE_COUNT + MAX_JITTER,
                "the page count must be the minimum plus at most the jitter, was " + pageCount);

        env.destroyInstance(instance, true);
    }

    @Test
    void testPageCalculationWithScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 10; i++) {
            env.createPlayer(instance);
        }

        int pageCount = PageCalculation.calculatePageAmount();
        assertTrue(pageCount >= 18 && pageCount <= 18 + MAX_JITTER,
                "the page count must be the scaled amount plus at most the jitter, was " + pageCount);

        env.destroyInstance(instance, true);
    }

    @Test
    void testPageCalculationVariesAcrossRounds(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 10; i++) {
            env.createPlayer(instance);
        }

        Set<Integer> observed = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            observed.add(PageCalculation.calculatePageAmount());
        }

        assertTrue(observed.size() > 1,
                "the page count must vary across rounds instead of always landing on the same number, observed " + observed);
        for (int value : observed) {
            assertTrue(value >= 18 && value <= 18 + MAX_JITTER,
                    "each roll must stay within the base amount plus at most the jitter, was " + value);
        }

        env.destroyInstance(instance, true);
    }

    @Test
    void testActivePageCalculationWithoutScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 3; i++) {
            env.createPlayer(instance);
        }

        int activePageCount = PageCalculation.calculateActivePageAmount();
        assertEquals(GameConfig.MIN_ACTIVE_PAGE_COUNT, activePageCount);

        env.destroyInstance(instance, true);
    }

    @Test
    void testActivePageCalculationStaysFlatUntilNearTheTopOfTheRange(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 11; i++) {
            env.createPlayer(instance);
        }

        int activePageCount = PageCalculation.calculateActivePageAmount();
        assertEquals(GameConfig.MIN_ACTIVE_PAGE_COUNT, activePageCount,
                "the active page count must still be at the minimum just below the top of the range");

        env.destroyInstance(instance, true);
    }

    @Test
    void testActivePageCalculationWithScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 13; i++) {
            env.createPlayer(instance);
        }

        int activePageCount = PageCalculation.calculateActivePageAmount();
        assertEquals(9, activePageCount,
                "the active page count at the top of the range must stay below 10");

        env.destroyInstance(instance, true);
    }
}
