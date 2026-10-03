package net.onelitefeather.cygnus.common.config;

/**
 * Settings for the survivors' fear.
 * <p>
 * Fear jumps on what a survivor lives through and slowly wears off, but never below a floor that
 * grows with the pages found. The creek reads it to pick whom to stalk and when to hunt.
 * </p>
 *
 * @param pageFloorWeight         the floor once every page is found, between 0 and 1
 * @param pageFoundGain           the jump for the survivor who found a page
 * @param sightingGain            the jump when the creek comes into a survivor's view
 * @param sightingCooldownSeconds the shortest gap between two counted sightings of one survivor
 * @param caughtGain              the jump when the creek catches a survivor
 * @param deathGain               the jump for survivors near someone who died
 * @param deathRadius             how close a survivor has to be to a death to be scared by it, in blocks
 * @param decayPerSecond          how much fear wears off per second
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public record SanityConfig(
        double pageFloorWeight,
        double pageFoundGain,
        double sightingGain,
        int sightingCooldownSeconds,
        double caughtGain,
        double deathGain,
        int deathRadius,
        double decayPerSecond
) {

    /**
     * The default settings, tuned against the creek's default thresholds (stalk 0.25, hunt 0.6).
     */
    public static final SanityConfig DEFAULT = new SanityConfig(
            0.5D, 0.10D, 0.10D, 20, 0.30D, 0.25D, 32, 0.005D
    );

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public SanityConfig {
        between("pageFloorWeight", pageFloorWeight);
        between("pageFoundGain", pageFoundGain);
        between("sightingGain", sightingGain);
        atLeast("sightingCooldownSeconds", sightingCooldownSeconds, 1);
        between("caughtGain", caughtGain);
        between("deathGain", deathGain);
        atLeast("deathRadius", deathRadius, 1);
        atLeast("decayPerSecond", decayPerSecond, 0.0D);
    }

    private static void between(String name, double value) {
        if (value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " (" + value + ") must be between 0.0 and 1.0");
        }
    }

    private static void atLeast(String name, double value, double minimum) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " (" + value + ") must be at least " + minimum);
        }
    }
}
