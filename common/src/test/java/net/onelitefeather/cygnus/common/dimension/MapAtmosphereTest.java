package net.onelitefeather.cygnus.common.dimension;

import net.minestom.server.color.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapAtmosphereTest {

    private static final Color FOG = new Color(15, 96, 52);
    private static final Color SKY_LIGHT = new Color(18, 105, 60);
    private static final Color BLACK = Color.fromRGBLike(Color.BLACK);

    private static MapAtmosphere with(float skyLightFactor, float fogStart, float fogEnd, float skyFogEnd) {
        return new MapAtmosphere(FOG, SKY_LIGHT, BLACK, skyLightFactor, fogStart, fogEnd, skyFogEnd);
    }

    @Test
    void clampsNegativeFogStartToZero() {
        assertEquals(0f, with(0.008f, -20f, 48f, 32f).fogStartDistance());
    }

    @Test
    void liftsFogEndAboveFogStart() {
        MapAtmosphere atmosphere = with(0.008f, 64f, 32f, 32f);

        assertTrue(atmosphere.fogEndDistance() > atmosphere.fogStartDistance());
    }

    @Test
    void clampsSkyLightFactorIntoUnitRange() {
        assertEquals(1f, with(5f, 0f, 48f, 32f).skyLightFactor());
        assertEquals(0f, with(-1f, 0f, 48f, 32f).skyLightFactor());
    }

    @Test
    void liftsANonPositiveSkyFogEndAboveZero() {
        assertTrue(with(0.008f, 0f, 48f, 0f).skyFogEndDistance() > 0f);
    }

    @Test
    void keepsValidValuesUntouched() {
        MapAtmosphere atmosphere = with(0.008f, 0f, 48f, 32f);

        assertEquals(0f, atmosphere.fogStartDistance());
        assertEquals(48f, atmosphere.fogEndDistance());
        assertEquals(32f, atmosphere.skyFogEndDistance());
        assertEquals(0.008f, atmosphere.skyLightFactor());
    }

    @Test
    void copiesEveryValueFromAPreset() {
        MapAtmosphere atmosphere = MapAtmosphere.from(StaticDimensionPreset.DENSE_FOG);

        assertEquals(StaticDimensionPreset.DENSE_FOG.fogStartDistance(), atmosphere.fogStartDistance());
        assertEquals(StaticDimensionPreset.DENSE_FOG.fogEndDistance(), atmosphere.fogEndDistance());
        assertEquals(StaticDimensionPreset.DENSE_FOG.skyFogEndDistance(), atmosphere.skyFogEndDistance());
        assertEquals(StaticDimensionPreset.DENSE_FOG.skyLightFactor(), atmosphere.skyLightFactor());
        assertEquals(StaticDimensionPreset.DENSE_FOG.fogColor(), atmosphere.fogColor());
        assertEquals(StaticDimensionPreset.DENSE_FOG.skyColor(), atmosphere.skyColor());
        assertEquals(StaticDimensionPreset.DENSE_FOG.skyLightColor(), atmosphere.skyLightColor());
    }

    @Test
    void twoAtmospheresWithTheSameValuesAreEqual() {
        assertEquals(with(0.008f, 0f, 48f, 32f), with(0.008f, 0f, 48f, 32f));
    }
}
