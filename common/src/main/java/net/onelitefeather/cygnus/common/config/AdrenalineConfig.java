package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.notNegative;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.share;

/**
 * Settings for the adrenaline rush of a survivor who sees the slender close by.
 * <p>
 * Once the visible slender comes within the radius, the survivor moves faster for a moment, so
 * even one whose sprint bar is empty can get away. The same survivor gets the next rush only once
 * the cooldown after the last one ran out.
 * </p>
 *
 * @param radius          how close the visible slender has to be, in blocks
 * @param speedBonus      how much faster the survivor moves, as a share on top of their speed, above 0 and at most 1
 * @param durationSeconds how long a rush lasts
 * @param cooldownSeconds how long a survivor waits after a rush before the next one
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public record AdrenalineConfig(
        int radius,
        double speedBonus,
        int durationSeconds,
        int cooldownSeconds
) {

    /**
     * The default settings, from the playtest in which hunted survivors had no chance to escape.
     */
    public static final AdrenalineConfig DEFAULT = new AdrenalineConfig(8, 0.2D, 4, 20);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public AdrenalineConfig {
        atLeast("radius", radius, 1);
        share("speedBonus", speedBonus);
        atLeast("durationSeconds", durationSeconds, 1);
        notNegative("cooldownSeconds", cooldownSeconds);
    }

    /**
     * Reads the adrenaline settings. The section carries the {@code adrenaline.} prefix.
     *
     * @param section the {@code adrenaline.} part of the config
     * @return the adrenaline settings
     */
    public static AdrenalineConfig read(ConfigSection section) {
        return section.orDefault("adrenaline", () -> new AdrenalineConfig(
                section.getInt("radius", DEFAULT.radius()),
                section.getDouble("speedBonus", DEFAULT.speedBonus()),
                section.getInt("durationSeconds", DEFAULT.durationSeconds()),
                section.getInt("cooldownSeconds", DEFAULT.cooldownSeconds())
        ), DEFAULT);
    }
}
