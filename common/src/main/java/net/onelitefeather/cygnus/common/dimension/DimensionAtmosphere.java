package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.util.RGBLike;

/**
 * The set of values that make up the visual atmosphere of a dimension: how far you can see, what
 * color the haze in front of you has, and how much light the sky contributes.
 * <p>
 * This carries no identity of its own. A {@link DimensionPreset} is a named atmosphere that can be
 * referenced from a config, while {@link MapAtmosphere} is the same set of values read from a
 * single map's {@code map.json} - it borrows its registry key from the map it belongs to.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public interface DimensionAtmosphere {

    /**
     * The color of the fog itself.
     *
     * @return the fog color
     */
    RGBLike fogColor();

    /**
     * The color of the light that appears to come down from the sky.
     *
     * @return the sky light color
     */
    RGBLike skyLightColor();

    /**
     * The sky's own background color.
     *
     * @return the sky color
     */
    RGBLike skyColor();

    /**
     * How strongly sky-light affects the dimension. Higher values feel brighter and more open;
     * values near zero feel dim and enclosed even without much fog.
     *
     * @return the sky light factor
     */
    float skyLightFactor();

    /**
     * The distance at which fog starts to become noticeable.
     *
     * @return the fog start distance
     */
    float fogStartDistance();

    /**
     * The distance beyond which the fog is fully opaque.
     *
     * @return the fog end distance
     */
    float fogEndDistance();

    /**
     * Like {@link #fogEndDistance()}, but for the sky-colored fog seen at the horizon rather than
     * the regular ground fog.
     *
     * @return the sky fog end distance
     */
    float skyFogEndDistance();
}
