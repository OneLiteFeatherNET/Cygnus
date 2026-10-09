package net.onelitefeather.cygnus.common.config;

/**
 * Settings for the slender's look through the eyes of the creek.
 * <p>
 * While he possesses the creek, his own body stands revealed and frozen, survivors around the
 * creek glow for him, and the creek is noticed from farther away. It only exists together with the
 * creek, so {@link CreekConfig#enabled()} switches it off as well.
 * </p>
 *
 * @param maxSeconds      the longest the slender may look through the creek's eyes
 * @param cooldownSeconds how long the slender waits before he may possess the creek again
 * @param sightFactor     how much farther survivors notice the creek while it is possessed. 1 keeps
 *                        the normal range
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public record PossessionConfig(
        int maxSeconds,
        int cooldownSeconds,
        double sightFactor
) {

    /**
     * The default settings: a short look, a long wait, and half again the normal sight range.
     */
    public static final PossessionConfig DEFAULT = new PossessionConfig(8, 60, 1.5D);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public PossessionConfig {
        if (maxSeconds < 1) {
            throw new IllegalArgumentException("maxSeconds (" + maxSeconds + ") must be at least 1");
        }
        if (cooldownSeconds < 0) {
            throw new IllegalArgumentException("cooldownSeconds (" + cooldownSeconds + ") must not be negative");
        }
        if (sightFactor < 1.0D) {
            throw new IllegalArgumentException("sightFactor (" + sightFactor + ") must be at least 1.0");
        }
    }

    /**
     * Reads the possession settings. The section carries the {@code possession.} prefix. Values
     * out of range fall back to the defaults with a warning.
     *
     * @param section the {@code possession.} part of the config
     * @return the possession settings
     */
    public static PossessionConfig read(ConfigSection section) {
        PossessionConfig defaults = DEFAULT;
        return section.orDefault("possession", () -> new PossessionConfig(
                section.getInt("maxSeconds", defaults.maxSeconds()),
                section.getInt("cooldownSeconds", defaults.cooldownSeconds()),
                section.getDouble("sightFactor", defaults.sightFactor())
        ), defaults);
    }
}
