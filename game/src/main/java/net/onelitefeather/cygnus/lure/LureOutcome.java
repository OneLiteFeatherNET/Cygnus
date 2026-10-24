package net.onelitefeather.cygnus.lure;

/**
 * What follows once the figure has struck. Half the time nothing does, so a survivor never knows
 * whether it gets serious this time.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public enum LureOutcome {

    /** The scare was all. */
    NOTHING,

    /** The survivor is slowed for a few seconds. */
    SLOWNESS,

    /** The slender sees the survivor on the locator bar for a few seconds. */
    BETRAYAL,

    /** The survivor is blinded for a few seconds. */
    BLINDNESS;

    /** The share of scares followed by slowness. */
    static final double SLOWNESS_WEIGHT = 0.20D;

    /** The share of scares followed by a reveal on the slender's locator bar. */
    static final double BETRAYAL_WEIGHT = 0.15D;

    /** The share of scares followed by blindness. Whatever the three leave is nothing. */
    static final double BLINDNESS_WEIGHT = 0.15D;

    /**
     * Picks the follow-up. On the way from 0 to 1 come slowness, betrayal and blindness, each as wide
     * as its weight, and nothing takes the rest.
     *
     * @param roll a random number from 0 to 1
     * @return the follow-up
     */
    public static LureOutcome roll(double roll) {
        if (roll < SLOWNESS_WEIGHT) return SLOWNESS;
        double betrayal = SLOWNESS_WEIGHT + BETRAYAL_WEIGHT;
        if (roll < betrayal) return BETRAYAL;
        if (roll < betrayal + BLINDNESS_WEIGHT) return BLINDNESS;
        return NOTHING;
    }
}
