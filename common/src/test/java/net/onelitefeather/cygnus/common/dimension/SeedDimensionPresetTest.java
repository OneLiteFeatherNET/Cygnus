package net.onelitefeather.cygnus.common.dimension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeedDimensionPresetTest {

    private static final float MIN_SKY_LIGHT_FACTOR = 0.002f;
    private static final float MAX_SKY_LIGHT_FACTOR = 0.06f;
    private static final float MIN_FOG_START = 0f;
    private static final float MAX_FOG_START = 64f;
    private static final float MIN_FOG_END = 24f;
    private static final float MAX_FOG_END = 384f;
    private static final float MIN_SKY_FOG_END = 16f;
    private static final float MAX_SKY_FOG_END = 256f;

    private static final int SAMPLE_SEED_COUNT = 500;

    @Test
    void sameSeedProducesAnEqualPreset() {
        assertEquals(SeedDimensionPreset.fromSeed(42L), SeedDimensionPreset.fromSeed(42L));
    }

    @Test
    void differentSeedsProduceDifferentKeys() {
        assertNotEquals(
                SeedDimensionPreset.fromSeed(1L).getKey(),
                SeedDimensionPreset.fromSeed(2L).getKey()
        );
    }

    @Test
    void keyFollowsTheDocumentedFormat() {
        long seed = 123456789L;
        String expectedKey = "seed_" + Long.toHexString(seed);

        assertEquals(expectedKey, SeedDimensionPreset.fromSeed(seed).getKey());
    }

    @Test
    void generatedValuesStayWithinDocumentedBounds() {
        for (long seed = 0; seed < SAMPLE_SEED_COUNT; seed++) {
            SeedDimensionPreset preset = SeedDimensionPreset.fromSeed(seed);

            assertTrue(preset.skyLightFactor() >= MIN_SKY_LIGHT_FACTOR && preset.skyLightFactor() <= MAX_SKY_LIGHT_FACTOR,
                    "skyLightFactor out of bounds for seed " + seed);
            assertTrue(preset.fogStartDistance() >= MIN_FOG_START && preset.fogStartDistance() <= MAX_FOG_START,
                    "fogStartDistance out of bounds for seed " + seed);
            assertTrue(preset.fogEndDistance() >= MIN_FOG_END && preset.fogEndDistance() <= MAX_FOG_END,
                    "fogEndDistance out of bounds for seed " + seed);
            assertTrue(preset.skyFogEndDistance() >= MIN_SKY_FOG_END && preset.skyFogEndDistance() <= MAX_SKY_FOG_END,
                    "skyFogEndDistance out of bounds for seed " + seed);

            assertNotNull(preset.fogColor(), "fogColor was null for seed " + seed);
            assertNotNull(preset.skyLightColor(), "skyLightColor was null for seed " + seed);
            assertNotNull(preset.skyColor(), "skyColor was null for seed " + seed);
        }
    }

    @Test
    void fogStartDistanceNeverExceedsFogEndDistance() {
        for (long seed = 0; seed < SAMPLE_SEED_COUNT; seed++) {
            SeedDimensionPreset preset = SeedDimensionPreset.fromSeed(seed);

            assertTrue(preset.fogStartDistance() < preset.fogEndDistance(),
                    "fog start distance reached or passed fog end distance for seed " + seed);
        }
    }
}