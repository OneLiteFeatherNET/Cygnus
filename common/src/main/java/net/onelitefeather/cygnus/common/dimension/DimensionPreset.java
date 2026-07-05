package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;

public enum DimensionPreset {

    /*DEEP_GREEN(
            new Color(20, 127, 64),
            new Color(20, 127, 64),
            Color.BLACK,
            0.0f,
            0f,
            120f,
            80f
    ),

    DENSE_FOG(
            new Color(15, 90, 50),
            new Color(15, 90, 50),
            new Color(5, 5, 5),
            0.01f,
            0f,
            60f,
            35f
    ),

    NIGHTMARE(
            new Color(5, 30, 20),
            new Color(5, 30, 20),
            Color.BLACK,
            0.0f,
            0f,
            25f,
            10f
    );*/

    DEEP_GREEN(
            "deep_green",
            new Color(20, 127, 64),
            new Color(20, 127, 64),
            Color.BLACK,
            0.01f,
            0f,
            128f,
            64f
    ),

    DENSE_FOG(
            "dense_fog",
            new Color(15, 96, 52),
            new Color(18, 105, 60),
            Color.BLACK,
            0.008f,
            0f,
            48f,
            32f
    ),

    COLD_BLUE(
            "cold_blue",
            new Color(35, 90, 180),
            new Color(70, 140, 255),
            new Color(8, 12, 32),
            0.015f,
            16f,
            160f,
            96f
    ),

    VERY_DARK(
            "very_dark",
            new Color(6, 24, 18),
            new Color(10, 32, 24),
            Color.BLACK,
            0.0025f,
            0f,
            32f,
            24f
    ),

    BRIGHT(
            "bright",
            new Color(55, 175, 170),
            new Color(100, 220, 220),
            new Color(10, 35, 45),
            0.05f,
            32f,
            256f,
            160f
    ),

    MYSTIC_PURPLE(
            "mystic_purple",
            new Color(95, 45, 170),
            new Color(150, 90, 255),
            new Color(25, 10, 45),
            0.02f,
            8f,
            96f,
            64f
    ),

    TOXIC_GREEN(
            "toxic_green",
            new Color(130, 155, 20),
            new Color(180, 210, 40),
            new Color(35, 40, 5),
            0.025f,
            0f,
            64f,
            48f
    ),

    LAVA(
            "lava",
            new Color(180, 90, 25),
            new Color(255, 170, 80),
            new Color(45, 20, 8),
            0.03f,
            0f,
            72f,
            40f
    ),

    CLEAR_SKY(
            "clear_sky",
            new Color(30, 70, 120),
            new Color(120, 180, 255),
            new Color(15, 25, 50),
            0.08f,
            64f,
            384f,
            256f
    ),

    DEEP_SEA(
            "deep_sea",
            new Color(5, 45, 80),
            new Color(20, 80, 140),
            new Color(0, 10, 25),
            0.006f,
            0f,
            32f,
            24f
    ),

    SWAMP(
            "swamp",
            new Color(45, 85, 40),
            new Color(65, 120, 55),
            new Color(10, 20, 10),
            0.012f,
            0f,
            64f,
            40f
    ),

    MOONLIGHT(
            "moonlight",
            new Color(60, 90, 170),
            new Color(170, 190, 255),
            new Color(5, 5, 25),
            0.03f,
            32f,
            256f,
            128f
    );

    private static final DimensionPreset[] VALUES = values();

    private final String key;
    private final RGBLike fogColor;
    private final RGBLike skyLightColor;
    private final RGBLike skyColor;

    private final float skyLightFactor;

    private final float fogStartDistance;
    private final float fogEndDistance;
    private final float skyFogEndDistance;

    DimensionPreset(
            String key,
            RGBLike fogColor,
            RGBLike skyLightColor,
            RGBLike skyColor,
            float skyLightFactor,
            float fogStartDistance,
            float fogEndDistance,
            float skyFogEndDistance
    ) {
        this.key = key;
        this.fogColor = fogColor;
        this.skyLightColor = skyLightColor;
        this.skyColor = skyColor;
        this.skyLightFactor = skyLightFactor;
        this.fogStartDistance = fogStartDistance;
        this.fogEndDistance = fogEndDistance;
        this.skyFogEndDistance = skyFogEndDistance;
    }

    public String getKey() {
        return key;
    }

    public RGBLike fogColor() {
        return fogColor;
    }

    public RGBLike skyLightColor() {
        return skyLightColor;
    }

    public RGBLike skyColor() {
        return skyColor;
    }

    public float skyLightFactor() {
        return skyLightFactor;
    }

    public float fogStartDistance() {
        return fogStartDistance;
    }

    public float fogEndDistance() {
        return fogEndDistance;
    }

    public float skyFogEndDistance() {
        return skyFogEndDistance;
    }

    /**
     * Returns all values from the enumeration from a cached array.
     *
     * @return the given values
     */
    public static DimensionPreset[] getValues() {
        return VALUES;
    }
}