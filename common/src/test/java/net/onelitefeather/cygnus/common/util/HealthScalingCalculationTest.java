package net.onelitefeather.cygnus.common.util;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.random.RandomGenerator;

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
        assertEquals(0.0D, additionalHealth % 2.0D, 0.0001D, "additional health should always be a whole heart (multiple of 2 HP)");

        env.destroyInstance(instance, true);
    }

    @Test
    void testZeroHealthScaling() {
        assertEquals(0.0D, HealthScalingCalculation.getAdditionalHealth(12));
    }

    @Test
    void testRoundToWholeHeartExactValueIsUnaffectedByRandom() {
        RandomGenerator alwaysRoundDown = fixedRandom(0.999f);
        assertEquals(20.0f, HealthScalingCalculation.roundToWholeHeart(20.0f, alwaysRoundDown));
        assertEquals(0.0f, HealthScalingCalculation.roundToWholeHeart(0.0f, alwaysRoundDown));
    }

    @Test
    void testRoundToWholeHeartRoundsUp() {
        RandomGenerator alwaysRoundUp = fixedRandom(0.0f);
        assertEquals(16.0f, HealthScalingCalculation.roundToWholeHeart(15.0f, alwaysRoundUp));
        assertEquals(6.0f, HealthScalingCalculation.roundToWholeHeart(5.0f, alwaysRoundUp));
    }

    @Test
    void testRoundToWholeHeartRoundsDown() {
        RandomGenerator alwaysRoundDown = fixedRandom(0.999f);
        assertEquals(14.0f, HealthScalingCalculation.roundToWholeHeart(15.0f, alwaysRoundDown));
        assertEquals(4.0f, HealthScalingCalculation.roundToWholeHeart(5.0f, alwaysRoundDown));
    }

    private static RandomGenerator fixedRandom(float value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException("not used by roundToWholeHeart");
            }

            @Override
            public float nextFloat() {
                return value;
            }
        };
    }
}
