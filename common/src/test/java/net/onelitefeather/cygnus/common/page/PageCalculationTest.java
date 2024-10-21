package net.onelitefeather.cygnus.common.page;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageCalculationTest {

    @Test
    void testPageCalculationWithoutScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 3; i++) {
            env.createPlayer(instance);
        }

        int pageCount = PageCalculation.calculatePageAmount();
        assertEquals(GameConfig.MIN_PAGE_COUNT, pageCount);

        env.destroyInstance(instance, true);
    }

    @Test
    void testPageCalculationWithScaling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i < 10; i++) {
            env.createPlayer(instance);
        }

        int pageCount = PageCalculation.calculatePageAmount();
        assertEquals(18, pageCount);

        env.destroyInstance(instance, true);
    }
}
