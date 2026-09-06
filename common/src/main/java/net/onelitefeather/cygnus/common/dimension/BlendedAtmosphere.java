package net.onelitefeather.cygnus.common.dimension;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;

/**
 * One atmosphere read part of the way towards another.
 *
 * <p>This exists for the lobby. A player who walks straight from a vanilla sky into a map that
 * closes in at forty blocks meets the whole atmosphere at once, at the same moment the round
 * starts. Giving the lobby a weakened version of the map's own atmosphere turns that into a
 * build-up: the same colours and the same haze, only far enough away to still see across the
 * lobby, so the start of the round reads as the world tightening rather than as a cut.</p>
 *
 * <p>Every value is read off the straight line between the two ends, colours channel by channel.
 * A share of {@code 0} is the open end untouched, {@code 1} is the other atmosphere exactly, and
 * anything between is proportionally near each.</p>
 *
 * <p>The result is a {@link MapAtmosphere}, which means its own corrections apply on the way out:
 * a blend that would leave the fog no span to fade over is repaired there rather than here.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DimensionAtmosphere lobby = BlendedAtmosphere.between(StaticDimensionPreset.BRIGHT, mapAtmosphere, 0.3f);
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class BlendedAtmosphere {

    private BlendedAtmosphere() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads an atmosphere off the line between two others.
     *
     * @param open   the atmosphere a share of {@code 0} yields
     * @param target the atmosphere a share of {@code 1} yields
     * @param share  how far to travel from {@code open} towards {@code target}, clamped to
     *               {@code [0, 1]}
     * @return the blended atmosphere
     */
    public static DimensionAtmosphere between(DimensionAtmosphere open, DimensionAtmosphere target, float share) {
        float amount = Math.clamp(share, 0.0f, 1.0f);
        return new MapAtmosphere(
                mix(open.fogColor(), target.fogColor(), amount),
                mix(open.skyLightColor(), target.skyLightColor(), amount),
                mix(open.skyColor(), target.skyColor(), amount),
                mix(open.ambientLightColor(), target.ambientLightColor(), amount),
                lerp(open.skyLightFactor(), target.skyLightFactor(), amount),
                lerp(open.fogStartDistance(), target.fogStartDistance(), amount),
                lerp(open.fogEndDistance(), target.fogEndDistance(), amount),
                lerp(open.skyFogEndDistance(), target.skyFogEndDistance(), amount)
        );
    }

    /**
     * Mixes two colours channel by channel.
     *
     * @param open   the colour at {@code amount} 0
     * @param target the colour at {@code amount} 1
     * @param amount where between the two to read
     * @return the mixed colour
     */
    private static Color mix(RGBLike open, RGBLike target, float amount) {
        return new Color(
                Math.round(lerp(open.red(), target.red(), amount)),
                Math.round(lerp(open.green(), target.green(), amount)),
                Math.round(lerp(open.blue(), target.blue(), amount))
        );
    }

    /**
     * Reads a value off the line between two ends.
     *
     * @param open   the value at {@code amount} 0
     * @param target the value at {@code amount} 1
     * @param amount where between the two to read
     * @return the value at that point
     */
    private static float lerp(float open, float target, float amount) {
        return open + (target - open) * amount;
    }
}
