package net.onelitefeather.cygnus.common.util;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class HealthScalingCalculationTest {

    @ParameterizedTest(name = "Test additional health for {0} players")
    @ValueSource(ints = {4, 3, 2, 1})
    void testAdditionalHealthCount(int count, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        for (int i = 0; i < count; i++) {
            env.createPlayer(instance);
        }

        double additionalHealth = HealthScalingCalculation.getAdditionalHealth(count);
        assertNotEquals(0.0D, additionalHealth);
        assertTrue(additionalHealth <= 20.0D);

        env.destroyInstance(instance, true);
    }

    @Test
    void testZeroHealthScaling() {
        assertEquals(0.0D, HealthScalingCalculation.getAdditionalHealth(12));
    }
}
