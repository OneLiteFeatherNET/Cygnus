package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.below;
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
 *
 * @param enabled         whether the lure takes part in the round
 * @param aloneRadius     how far away every other living survivor has to be, in blocks
 * @param rollSeconds     how often a lone survivor is rolled for, in seconds
 * @param rollChance      the chance of a lure on every roll, between 0 and 1
 * @param minDistance     how close the figure stands at the least, in blocks
 * @param maxDistance     how far away the figure stands at the most, in blocks
 * @param minAngle        how far off the view the figure stands at the least, in degrees. Above
 *                        {@link CreekConfig#HIDE_ANGLE} the survivor never sees it pop in
 * @param maxAngle        how far off the view the figure stands at the most, in degrees
 * @param maxHeightOffset how much higher or lower than the survivor the figure may stand, in blocks
 * @param waitSeconds     how long the figure waits to be noticed before it is gone, in seconds
 * @param gazeMillis      how long the survivor has to look at the figure before it strikes, in milliseconds
 * @param gazeAngle       how far off the view centre the figure still counts as looked at, in degrees
 * @param fearGain        the jump in fear when the figure strikes, between 0 and 1
 * @param slownessWeight  the share of scares followed by slowness
 * @param betrayalWeight  the share of scares followed by a reveal on the slender's locator bar
 * @param blindnessWeight the share of scares followed by blindness. Whatever the three leave is nothing
 * @param effectSeconds   how long a follow-up lasts, in seconds
 * @param respiteSeconds  how long the creek leaves the survivor alone after the scare, in seconds
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public record LureConfig(
        boolean enabled,
        int aloneRadius,
        int rollSeconds,
        double rollChance,
        int minDistance,
        int maxDistance,
        int minAngle,
        int maxAngle,
        int maxHeightOffset,
        int waitSeconds,
        int gazeMillis,
        int gazeAngle,
        double fearGain,
        double slownessWeight,
        double betrayalWeight,
        double blindnessWeight,
        int effectSeconds,
        int respiteSeconds
) {

    /** Past this the figure stands so far away that nobody makes out who it is. */
    public static final int MAX_DISTANCE = 64;

    /** Past this the figure stands on roofs and down in gorges. */
    public static final int MAX_HEIGHT_OFFSET = 8;

    /**
     * The default settings: a lone survivor is rolled for every 5 seconds at 8 %, so a lure comes
     * after about a minute alone. Half the scares are followed by nothing.
     */
    public static final LureConfig DEFAULT = new LureConfig(true, 30, 5, 0.08D, 15, 25, 80, 130, 3, 20, 700, 35,
            0.30D, 0.20D, 0.15D, 0.15D, 4, 60);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range or two values contradict each other
     */
    public LureConfig {
        atLeast("aloneRadius", aloneRadius, 1);
        atLeast("rollSeconds", rollSeconds, 1);
        between("rollChance", rollChance, 0.0D, 1.0D);
        atLeast("minDistance", minDistance, 1);
        between("maxDistance", maxDistance, 1, MAX_DISTANCE);
        below("minDistance", minDistance, "maxDistance", maxDistance);
        between("minAngle", minAngle, 0, 180);
        between("maxAngle", maxAngle, 0, 180);
        below("minAngle", minAngle, "maxAngle", maxAngle);
        between("maxHeightOffset", maxHeightOffset, 0, MAX_HEIGHT_OFFSET);
        atLeast("waitSeconds", waitSeconds, 1);
        atLeast("gazeMillis", gazeMillis, 0);
        between("gazeAngle", gazeAngle, 1, 90);
        between("fearGain", fearGain, 0.0D, 1.0D);
        between("slownessWeight", slownessWeight, 0.0D, 1.0D);
        between("betrayalWeight", betrayalWeight, 0.0D, 1.0D);
        between("blindnessWeight", blindnessWeight, 0.0D, 1.0D);
        double weights = slownessWeight + betrayalWeight + blindnessWeight;
        // Rounded for the message only, so 0.5 + 0.4 + 0.2 reads as 1.1.
        if (weights > 1.0D + 1.0E-9) {
            throw new IllegalArgumentException("slownessWeight + betrayalWeight + blindnessWeight ("
                    + Math.round(weights * 1000.0D) / 1000.0D + ") must be at most 1");
        }
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
                section.getInt("rollSeconds", DEFAULT.rollSeconds()),
                section.getDouble("rollChance", DEFAULT.rollChance()),
                section.getInt("minDistance", DEFAULT.minDistance()),
                section.getInt("maxDistance", DEFAULT.maxDistance()),
                section.getInt("minAngle", DEFAULT.minAngle()),
                section.getInt("maxAngle", DEFAULT.maxAngle()),
                section.getInt("maxHeightOffset", DEFAULT.maxHeightOffset()),
                section.getInt("waitSeconds", DEFAULT.waitSeconds()),
                section.getInt("gazeMillis", DEFAULT.gazeMillis()),
                section.getInt("gazeAngle", DEFAULT.gazeAngle()),
                section.getDouble("fearGain", DEFAULT.fearGain()),
                section.getDouble("slownessWeight", DEFAULT.slownessWeight()),
                section.getDouble("betrayalWeight", DEFAULT.betrayalWeight()),
                section.getDouble("blindnessWeight", DEFAULT.blindnessWeight()),
                section.getInt("effectSeconds", DEFAULT.effectSeconds()),
                section.getInt("respiteSeconds", DEFAULT.respiteSeconds())
        ), DEFAULT);
    }

    /**
     * Returns the share of scares followed by nothing: whatever the three follow-ups leave.
     *
     * @return the share, between 0 and 1
     */
    public double nothingWeight() {
        return Math.max(0.0D, 1.0D - this.slownessWeight - this.betrayalWeight - this.blindnessWeight);
    }
}
