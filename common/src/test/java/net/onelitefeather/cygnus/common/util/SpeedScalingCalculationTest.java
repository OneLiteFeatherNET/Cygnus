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
class SpeedScalingCalculationTest {

    @ParameterizedTest(name = "Test additional speed for {0} players")
    @ValueSource(ints = {4, 3, 2, 1})
    void testAdditionalSpeedCount(int count, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        for (int i = 0; i < count; i++) {
            env.createPlayer(instance);
        }

        double additionalSpeed = SpeedScalingCalculation.getAdditionalSpeed(count);
        assertNotEquals(0.0D, additionalSpeed);
        assertTrue(additionalSpeed <= 0.01D);

        env.destroyInstance(instance, true);
    }

    @Test
    void testZeroSpeedScaling() {
        assertEquals(0.0D, SpeedScalingCalculation.getAdditionalSpeed(12));
    }
}
