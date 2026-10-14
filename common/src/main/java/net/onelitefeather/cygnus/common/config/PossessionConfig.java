package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.notNegative;

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
 * @param startDelaySeconds how long the slender waits after the round starts before he may possess
 *                          the creek the first time
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public record PossessionConfig(
        int maxSeconds,
        int cooldownSeconds,
        double sightFactor,
        int startDelaySeconds
) {

    /**
     * The default settings: a short look, a long wait, half again the normal sight range, and a
     * locked first minute and a half.
     */
    public static final PossessionConfig DEFAULT = new PossessionConfig(8, 60, 1.5D, 90);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public PossessionConfig {
        atLeast("maxSeconds", maxSeconds, 1);
        notNegative("cooldownSeconds", cooldownSeconds);
        atLeast("sightFactor", sightFactor, 1.0D);
        notNegative("startDelaySeconds", startDelaySeconds);
    }

    /**
     * Reads the possession settings. The section carries the {@code possession.} prefix. Values
     * out of range fall back to the defaults with a warning.
     *
     * @param section the {@code possession.} part of the config
     * @return the possession settings
     */
    public static PossessionConfig read(ConfigSection section) {
        return section.orDefault("possession", () -> new PossessionConfig(
                section.getInt("maxSeconds", DEFAULT.maxSeconds()),
                section.getInt("cooldownSeconds", DEFAULT.cooldownSeconds()),
                section.getDouble("sightFactor", DEFAULT.sightFactor()),
                section.getInt("startDelaySeconds", DEFAULT.startDelaySeconds())
        ), DEFAULT);
    }
}
