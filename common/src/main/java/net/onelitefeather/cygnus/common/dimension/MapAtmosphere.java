package net.onelitefeather.cygnus.common.dimension;

import net.minestom.server.color.Color;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The atmosphere of a single map, as read from its {@code map.json}.
 * <p>
 * Unlike a {@link DimensionPreset} this carries no key of its own: the registry key is derived from
 * the map it belongs to, so the same values can be edited per map without inventing a preset name
 * for every one of them.
 * </p>
 * <p>
 * The components are declared as {@link Color} rather than {@link RGBLike} on purpose. Gson cannot
 * deserialize an interface without being told which implementation to pick, and a map file that
 * fails to parse would take the whole service down with it.
 * </p>
 * <p>
 * Values outside their sensible range are clamped rather than rejected. A map file is edited by
 * hand often enough that a typo is a question of when, not if, and a service that refuses to start
 * over a fog distance is worse than one that starts with a corrected value and says so.
 * </p>
 *
 * @param fogColor          the color of the fog itself
 * @param skyLightColor     the color of the light coming down from the sky
 * @param skyColor          the sky's own background color
 * @param skyLightFactor    how strongly sky-light affects the map, in {@code [0, 1]}
 * @param fogStartDistance  the distance at which fog becomes noticeable, at least {@code 0}
 * @param fogEndDistance    the distance at which fog is fully opaque, above {@code fogStartDistance}
 * @param skyFogEndDistance the same for the sky-colored fog at the horizon, above {@code 0}
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public record MapAtmosphere(
        Color fogColor,
        Color skyLightColor,
        Color skyColor,
        float skyLightFactor,
        float fogStartDistance,
        float fogEndDistance,
        float skyFogEndDistance
) implements DimensionAtmosphere {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapAtmosphere.class);

    /**
     * Smallest gap the fog is allowed to fade over. Anything narrower reads as a wall rather than
     * as fog, and a gap of zero makes the client's fog calculation degenerate.
     */
    private static final float MIN_FOG_SPAN = 1f;

    /**
     * Creates a new atmosphere, correcting any value that lies outside its documented range.
     */
    public MapAtmosphere {
        fogColor = orBlack(fogColor, "fogColor");
        skyLightColor = orBlack(skyLightColor, "skyLightColor");
        skyColor = orBlack(skyColor, "skyColor");

        if (skyLightFactor < 0f || skyLightFactor > 1f) {
            LOGGER.warn("skyLightFactor {} is outside [0, 1], clamping it", skyLightFactor);
            skyLightFactor = Math.clamp(skyLightFactor, 0f, 1f);
        }

        if (fogStartDistance < 0f) {
            LOGGER.warn("fogStartDistance {} is negative, using 0 instead", fogStartDistance);
            fogStartDistance = 0f;
        }

        if (fogEndDistance <= fogStartDistance) {
            float corrected = fogStartDistance + MIN_FOG_SPAN;
            LOGGER.warn(
                    "fogEndDistance {} is not beyond fogStartDistance {}, using {} instead",
                    fogEndDistance, fogStartDistance, corrected
            );
            fogEndDistance = corrected;
        }

        if (skyFogEndDistance <= 0f) {
            LOGGER.warn("skyFogEndDistance {} is not positive, using {} instead", skyFogEndDistance, MIN_FOG_SPAN);
            skyFogEndDistance = MIN_FOG_SPAN;
        }
    }

    /**
     * Copies the values of an existing atmosphere, for instance one of the
     * {@link StaticDimensionPreset}s a map builder picked as a starting point.
     *
     * @param atmosphere the atmosphere to copy
     * @return a map atmosphere holding the same values
     */
    @Contract(value = "_ -> new", pure = true)
    public static MapAtmosphere from(DimensionAtmosphere atmosphere) {
        return new MapAtmosphere(
                Color.fromRGBLike(atmosphere.fogColor()),
                Color.fromRGBLike(atmosphere.skyLightColor()),
                Color.fromRGBLike(atmosphere.skyColor()),
                atmosphere.skyLightFactor(),
                atmosphere.fogStartDistance(),
                atmosphere.fogEndDistance(),
                atmosphere.skyFogEndDistance()
        );
    }

    /**
     * Substitutes black for a color a map file left out, so a partially written {@code atmosphere}
     * block still yields a usable dimension.
     *
     * @param color the color as read from the file
     * @param name  the field name, for the warning
     * @return the given color, or black if it was absent
     */
    private static Color orBlack(@Nullable Color color, String name) {
        if (color != null) return color;
        LOGGER.warn("{} is missing from the atmosphere, using black instead", name);
        return Color.fromRGBLike(Color.BLACK);
    }
}
