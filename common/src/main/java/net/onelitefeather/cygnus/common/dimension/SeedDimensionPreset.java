package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.util.RGBLike;
import net.onelitefeather.cygnus.common.util.ColorUtil;

import java.util.Random;

/**
 * A {@link DimensionPreset} that is generated deterministically from a seed.
 * <p>
 * The same seed always produces the same atmosphere, making these presets
 * suitable for procedural worlds while keeping their appearance consistent
 * across sessions.
 *
 * @version 1.0.0
 * @since 2.6.6
 * @author theEvilReaper
 */
public record SeedDimensionPreset(
        String key,
        RGBLike fogColor,
        RGBLike skyLightColor,
        RGBLike skyColor,
        float skyLightFactor,
        float fogStartDistance,
        float fogEndDistance,
        float skyFogEndDistance
) implements DimensionPreset {

    // Parameter bounds derived from the predefined DimensionPreset values.
    // Keeping generated presets within these ranges prevents atmospheres
    // from becoming excessively bright, dark, or foggy.
    private static final float MIN_SKY_LIGHT_FACTOR = 0.002f;
    private static final float MAX_SKY_LIGHT_FACTOR = 0.06f;

    private static final float MIN_FOG_START = 0f;
    private static final float MAX_FOG_START = 64f;

    private static final float MIN_FOG_END = 24f;
    private static final float MAX_FOG_END = 384f;

    private static final float MIN_SKY_FOG_END = 16f;
    private static final float MAX_SKY_FOG_END = 256f;

    /**
     * Creates a deterministic dimension preset from the given seed.
     * <p>
     * The seed determines both the base hue and a density value. The density
     * controls visibility, lighting, and color intensity, while small hue
     * offsets keep the fog, skylight, and sky colors visually related.
     *
     * @param seed any stable seed, such as a world seed or hashed dimension name
     * @return the generated dimension preset
     */
    public static SeedDimensionPreset fromSeed(long seed) {
        Random random = new Random(seed);

        float hue = random.nextFloat() * 360f;
        float density = random.nextFloat(); // 0 = open/bright, 1 = dense/dark

        float skyLightFactor = lerp(MAX_SKY_LIGHT_FACTOR, MIN_SKY_LIGHT_FACTOR, density);
        float fogStartDistance = lerp(MAX_FOG_START, MIN_FOG_START, density);
        float fogEndDistance = lerp(MAX_FOG_END, MIN_FOG_END, density);
        float skyFogEndDistance = lerp(MAX_SKY_FOG_END, MIN_SKY_FOG_END, density);

        float fogHue = hue;
        float skyLightHue = wrapHue(hue + 8f);
        float skyHue = wrapHue(hue - 6f);

        RGBLike fogColor = ColorUtil.hsl(
                fogHue,
                lerp(0.55f, 0.25f, density),
                lerp(0.42f, 0.14f, density)
        );

        RGBLike skyLightColor = ColorUtil.hsl(
                skyLightHue,
                lerp(0.75f, 0.45f, density),
                lerp(0.72f, 0.32f, density)
        );

        RGBLike skyColor = ColorUtil.hsl(
                skyHue,
                lerp(0.55f, 0.35f, density),
                lerp(0.16f, 0.03f, density)
        );

        String key = "seed_" + Long.toHexString(seed);

        return new SeedDimensionPreset(
                key,
                fogColor,
                skyLightColor,
                skyColor,
                skyLightFactor,
                fogStartDistance,
                fogEndDistance,
                skyFogEndDistance
        );
    }

    /**
     * Performs linear interpolation between {@code a} and {@code b}.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Wraps a hue into the {@code [0, 360)} range.
     */
    private static float wrapHue(float hue) {
        float h = hue % 360f;
        return h < 0f ? h + 360f : h;
    }

    /**
     * Adapts the record component accessor to the {@link DimensionPreset}
     * naming convention.
     */
    @Override
    public String getKey() {
        return key;
    }
}