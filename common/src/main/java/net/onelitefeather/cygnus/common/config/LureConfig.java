package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.between;

/**
 * The dead teammate who lures a lone survivor.
 * <p>
 * Once a survivor has died, a survivor who is on their own may see them standing in the distance,
 * just outside their view. Look at the figure for a moment and it is right in front of their face.
 * After the scare a follow-up is rolled, and the creek leaves the survivor alone for a while. Every
 * survivor gets one lure per round at most. The keys carry the {@code lure.} prefix, for example
 * {@code lure.rollChance=0.08}.
 * </p>
 * <p>
 * Only what a playtest is likely to tune is here. Where the figure stands, how long it waits and how
 * the gaze is measured hang together and are fixed in the lure itself.
 * </p>
 *
 * @param enabled        whether the lure takes part in the round
 * @param aloneRadius    how far away every other living survivor has to be, in blocks
 * @param rollChance     the chance of a lure on every roll of a lone survivor, between 0 and 1
 * @param fearGain       the jump in fear when the figure strikes, between 0 and 1
 * @param effectSeconds  how long a follow-up lasts, in seconds
 * @param respiteSeconds how long the creek leaves the survivor alone after the scare, in seconds
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public record LureConfig(
        boolean enabled,
        int aloneRadius,
        double rollChance,
        double fearGain,
        int effectSeconds,
        int respiteSeconds
) {

    /**
     * The default settings: a lone survivor is rolled for every 5 seconds at 8 %, so a lure comes
     * after about a minute alone.
     */
    public static final LureConfig DEFAULT = new LureConfig(true, 30, 0.08D, 0.30D, 4, 60);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public LureConfig {
        atLeast("aloneRadius", aloneRadius, 1);
        between("rollChance", rollChance, 0.0D, 1.0D);
        between("fearGain", fearGain, 0.0D, 1.0D);
        atLeast("effectSeconds", effectSeconds, 1);
        atLeast("respiteSeconds", respiteSeconds, 0);
    }

    /**
     * Reads the settings. The section carries the {@code lure.} prefix. Invalid values fall back to
     * the DEFAULT.
     *
     * @param section the {@code lure.} part of the config
     * @return the settings
     */
    public static LureConfig read(ConfigSection section) {
        return section.orDefault("lure", () -> new LureConfig(
                section.getBoolean("enabled", DEFAULT.enabled()),
                section.getInt("aloneRadius", DEFAULT.aloneRadius()),
                section.getDouble("rollChance", DEFAULT.rollChance()),
                section.getDouble("fearGain", DEFAULT.fearGain()),
                section.getInt("effectSeconds", DEFAULT.effectSeconds()),
                section.getInt("respiteSeconds", DEFAULT.respiteSeconds())
        ), DEFAULT);
    }
}
