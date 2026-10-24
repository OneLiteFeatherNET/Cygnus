package net.onelitefeather.cygnus.lure;

import net.onelitefeather.cygnus.common.config.LureConfig;

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

    /**
     * Picks the follow-up. On the way from 0 to 1 come slowness, betrayal and blindness, each as wide
     * as its weight, and nothing takes the rest.
     *
     * @param config the weights
     * @param roll   a random number from 0 to 1
     * @return the follow-up
     */
    public static LureOutcome roll(LureConfig config, double roll) {
        double slowness = config.slownessWeight();
        if (roll < slowness) return SLOWNESS;
        double betrayal = slowness + config.betrayalWeight();
        if (roll < betrayal) return BETRAYAL;
        if (roll < betrayal + config.blindnessWeight()) return BLINDNESS;
        return NOTHING;
    }
}
