package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.util.RGBLike;


/**
 * Describes which preset values are required for a custom Dimension in Minestom.
 * It contains only data about the fog and sky colors, and nothing else.
 * If you also want to adjust your biome, that is an additional step that is not covered here.
 *
 * @author thEvilReaper
 * @version 1.0.0
 * @since 2.6.6
 */
public interface DimensionPreset {

    /**
     * A short, unique, lowercase identifier for this preset, used wherever
     * the preset needs to be referenced by name instead of by object
     * reference, e.g. in configs or when registering the dimension.
     */
    String getKey();

    /**
     * The color of the fog itself.
     */
    RGBLike fogColor();

    /**
     * The color of the light that appears to come down from the sky.
     */
    RGBLike skyLightColor();

    /**
     * The sky's own background color.
     */
    RGBLike skyColor();

    /**
     * How strongly sky-light affects the dimension. Higher values feel
     * brighter and more open; values near zero feel dim and enclosed even
     * without much fog.
     */
    float skyLightFactor();

    /**
     * The distance at which fog starts to become noticeable.
     */
    float fogStartDistance();

    /**
     * The distance beyond which the fog is fully opaque.
     */
    float fogEndDistance();

    /**
     * Like {@link #fogEndDistance()}, but for the sky-colored fog seen at
     * the horizon rather than the regular ground fog.
     */
    float skyFogEndDistance();
}
