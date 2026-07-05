package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;

/**
 * Hand-crafted dimension atmospheres with predefined settings.
 * Use these for a consistent look. For seed-dependent variation, use
 * {@link SeedDimensionPreset}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.6
 */
public enum StaticDimensionPreset implements DimensionPreset {

    /**
     * A dense, leafy green — the classic overgrown-jungle feel.
     */
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

    /**
     * Thick, close-in fog — you can barely see past arm's reach.
     */
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

    /**
     * A crisp, icy blue with decent visibility — think frozen tundra.
     */
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

    /**
     * About as dim and enclosed as it gets — barely any sky light at all.
     */
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

    /**
     * Open, airy, and well-lit — long sightlines, minimal fog.
     */
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

    /**
     * A magical, otherworldly purple haze.
     */
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

    /**
     * A sickly, radioactive-looking yellow-green.
     */
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

    /**
     * Hot orange tones and a hazy, heat-shimmer kind of fog.
     */
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

    /**
     * The brightest, most open preset — a perfect clear day.
     */
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

    /**
     * Dark, murky blue-green — deep underwater with barely any light.
     */
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

    /**
     * Murky, muted greens — damp and overgrown.
     */
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

    /**
     * Cool, pale blue-white light — a calm, moonlit night.
     */
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

    // Cached once so getValues() doesn't clone the backing array on every
    // single call the way the built-in values() does.
    private static final StaticDimensionPreset[] VALUES = values();

    private final String key;
    private final RGBLike fogColor;
    private final RGBLike skyLightColor;
    private final RGBLike skyColor;

    private final float skyLightFactor;

    private final float fogStartDistance;
    private final float fogEndDistance;
    private final float skyFogEndDistance;

    /**
     * Creates a new instance of an enumeration entry with the given values.
     *
     * @param key               of the preset
     * @param fogColor          of the preset
     * @param skyLightColor     of the preset
     * @param skyColor          of the preset
     * @param skyLightFactor    of the preset
     * @param fogStartDistance  of the preset
     * @param fogEndDistance    of the preset
     * @param skyFogEndDistance of the preset
     */
    StaticDimensionPreset(
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RGBLike fogColor() {
        return fogColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RGBLike skyLightColor() {
        return skyLightColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RGBLike skyColor() {
        return skyColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float skyLightFactor() {
        return skyLightFactor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float fogStartDistance() {
        return fogStartDistance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float fogEndDistance() {
        return fogEndDistance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float skyFogEndDistance() {
        return skyFogEndDistance;
    }

    /**
     * All presets, in declaration order. Backed by a cached array instead of
     * calling the built-in {@code values()} every time, since that method
     * allocates a fresh array on every call — worth avoiding if this ever
     * gets called somewhere hot, like once per tick or per player.
     *
     * @return every declared preset
     */
    public static StaticDimensionPreset[] getValues() {
        return VALUES;
    }
}