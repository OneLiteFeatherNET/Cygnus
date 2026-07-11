package net.onelitefeather.cygnus.common.dimension;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StaticDimensionPresetTest {

    @Test
    void everyKeyIsUniqueAndNotBlank() {
        Set<String> keys = new HashSet<>();

        for (StaticDimensionPreset preset : StaticDimensionPreset.getValues()) {
            assertFalse(preset.getKey().isBlank(), preset.name() + " has a blank key");
            assertTrue(keys.add(preset.getKey()), "duplicate key: " + preset.getKey());
        }
    }

    @Test
    void everyPresetHasNonNullColors() {
        for (StaticDimensionPreset preset : StaticDimensionPreset.getValues()) {
            assertNotNull(preset.fogColor(), preset.name() + " has no fog color");
            assertNotNull(preset.skyLightColor(), preset.name() + " has no sky light color");
            assertNotNull(preset.skyColor(), preset.name() + " has no sky color");
        }
    }

    @Test
    void fogStartDistanceNeverExceedsFogEndDistance() {
        for (StaticDimensionPreset preset : StaticDimensionPreset.getValues()) {
            assertTrue(preset.fogStartDistance() <= preset.fogEndDistance(),
                    preset.name() + " has a fog start distance beyond its end distance");
        }
    }

    @Test
    void skyLightFactorIsNeverNegative() {
        for (StaticDimensionPreset preset : StaticDimensionPreset.getValues()) {
            assertTrue(preset.skyLightFactor() >= 0f, preset.name() + " has a negative sky light factor");
        }
    }
}