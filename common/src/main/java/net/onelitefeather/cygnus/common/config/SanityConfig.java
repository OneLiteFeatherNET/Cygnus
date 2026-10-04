package net.onelitefeather.cygnus.common.config;

/**
 * Settings for the survivors' fear.
 * <p>
 * Fear jumps on what a survivor lives through and slowly wears off, but never below a floor that
 * grows with the pages found, the time played, and every scare the survivor has taken. Being
 * stalked raises it steadily. The creek reads it to pick whom to stalk and when to hunt.
 * </p>
 *
 * @param pageFloorWeight         the floor once every page is found, between 0 and 1
 * @param pageFoundGain           the jump for the survivor who found a page
 * @param sightingGain            the jump when the creek comes into a survivor's view
 * @param sightingCooldownSeconds the shortest gap between two counted sightings of one survivor
 * @param caughtGain              the jump when the creek catches a survivor
 * @param selectedGain            the jump when the patrolling creek picks a survivor out
 * @param deathGain               the jump for every survivor when another one dies
 * @param decayPerSecond          how much fear wears off per second
 * @param stalkGainPerSecond      how much fear grows per second while a creek stalks the survivor
 * @param residualShare           the share of every jump that never wears off again between 0 and 1
 * @param timeFloorWeight         the floor added once the round's time is up, between 0 and 1
 * @param floorCap                the highest the floor may grow, between 0 and 1, kept below the
 *                                creek's hunt threshold, only a fresh scare should start a hunt
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
        double selectedGain,
        double deathGain,
        double decayPerSecond,
        double stalkGainPerSecond,
        double residualShare,
        double timeFloorWeight,
        double floorCap
) {

    /**
     * The default settings, tuned against the creek's default thresholds (stalk 0.25, hunt 0.6).
     * A stalk of 45 to 90 seconds raises the fear by about 0.01 per second after decay, so a long
     * one turns into a hunt on its own, and sooner the later it is in the round.
     */
    public static final SanityConfig DEFAULT = new SanityConfig(
            0.5D, 0.10D, 0.10D, 20, 0.30D, 0.15D, 0.25D, 0.005D,
            0.015D, 0.25D, 0.15D, 0.55D
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
        between("selectedGain", selectedGain);
        between("deathGain", deathGain);
        atLeast("decayPerSecond", decayPerSecond, 0.0D);
        between("stalkGainPerSecond", stalkGainPerSecond);
        between("residualShare", residualShare);
        between("timeFloorWeight", timeFloorWeight);
        between("floorCap", floorCap);
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
