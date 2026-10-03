package net.onelitefeather.cygnus.common.config;

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
        if (radius < 1) {
            throw new IllegalArgumentException("radius (" + radius + ") must be at least 1");
        }
        if (speedBonus <= 0.0D || speedBonus > 1.0D) {
            throw new IllegalArgumentException("speedBonus (" + speedBonus + ") must be above 0.0 and at most 1.0");
        }
        if (durationSeconds < 1) {
            throw new IllegalArgumentException("durationSeconds (" + durationSeconds + ") must be at least 1");
        }
        if (cooldownSeconds < 0) {
            throw new IllegalArgumentException("cooldownSeconds (" + cooldownSeconds + ") must not be negative");
        }
    }
}
