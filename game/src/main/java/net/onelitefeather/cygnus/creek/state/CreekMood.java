package net.onelitefeather.cygnus.creek.state;

/**
 * How the creek behaves after its mood switched, see {@code MoodClock}.
 * <p>
 * Every round starts {@link #NORMAL}. The other moods change how often and how hard the creek
 * uses its moves. The factors multiply the settings before the round's calm applies.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public enum CreekMood {

    /** The creek everyone knows. */
    NORMAL(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.25D, false, false, 0.5D),

    /** Hunts sooner, faster and again sooner, and keeps creeping while it is watched. */
    HUNTER(0.7D, 0.5D, 1.2D, 0.6D, 0.25D, 1.0D, 1.0D, 0.25D, false, false, 0.5D),

    /** Stalks longer and more often, comes back closer, marks more and hunts less. */
    SHADOW(1.3D, 1.0D, 1.0D, 1.0D, 0.0D, 1.5D, 0.7D, 0.5D, false, false, 0.5D),

    /** Teleports whoever it gets hold of and vanishes more. */
    TRICKSTER(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.25D, true, true, 0.8D);

    private final double huntThreshold;
    private final double minStalk;
    private final double huntSpeed;
    private final double huntCooldown;
    private final double watchedHuntSpeed;
    private final double stalkDuration;
    private final double variantBreather;
    private final double spotMarkChance;
    private final boolean alwaysTeleport;
    private final boolean stareTeleports;
    private final double deadEndVanishChance;

    CreekMood(double huntThreshold, double minStalk, double huntSpeed, double huntCooldown, double watchedHuntSpeed,
              double stalkDuration, double variantBreather, double spotMarkChance, boolean alwaysTeleport,
              boolean stareTeleports, double deadEndVanishChance) {
        this.huntThreshold = huntThreshold;
        this.minStalk = minStalk;
        this.huntSpeed = huntSpeed;
        this.huntCooldown = huntCooldown;
        this.watchedHuntSpeed = watchedHuntSpeed;
        this.stalkDuration = stalkDuration;
        this.variantBreather = variantBreather;
        this.spotMarkChance = spotMarkChance;
        this.alwaysTeleport = alwaysTeleport;
        this.stareTeleports = stareTeleports;
        this.deadEndVanishChance = deadEndVanishChance;
    }

    /**
     * The factor on the dread a stalked survivor needs before a hunt.
     *
     * @return the factor
     */
    public double huntThreshold() {
        return this.huntThreshold;
    }

    /**
     * The factor on how long a stalk has to run before a hunt.
     *
     * @return the factor
     */
    public double minStalk() {
        return this.minStalk;
    }

    /**
     * The factor on the hunt speed.
     *
     * @return the factor
     */
    public double huntSpeed() {
        return this.huntSpeed;
    }

    /**
     * The factor on a survivor's breather after a hunt.
     *
     * @return the factor
     */
    public double huntCooldown() {
        return this.huntCooldown;
    }

    /**
     * The share of the hunt speed the creek keeps while the hunted survivor looks at it.
     *
     * @return the share, 0 when it freezes
     */
    public double watchedHuntSpeed() {
        return this.watchedHuntSpeed;
    }

    /**
     * The factor on how long a stalk lasts.
     *
     * @return the factor
     */
    public double stalkDuration() {
        return this.stalkDuration;
    }

    /**
     * The factor on a survivor's breather after a variant.
     *
     * @return the factor
     */
    public double variantBreather() {
        return this.variantBreather;
    }

    /**
     * The chance that a stalked survivor who looked at the creek is marked for the slender.
     *
     * @return the chance
     */
    public double spotMarkChance() {
        return this.spotMarkChance;
    }

    /**
     * Tells whether a catch always teleports instead of throwing.
     *
     * @return {@code true} if it always teleports
     */
    public boolean alwaysTeleport() {
        return this.alwaysTeleport;
    }

    /**
     * Tells whether the end of a patrol stare teleports the survivor instead of freezing or
     * flinging them.
     *
     * @return {@code true} if the stare teleports
     */
    public boolean stareTeleports() {
        return this.stareTeleports;
    }

    /**
     * The chance that the creek vanishes at a dead end instead of resting there.
     *
     * @return the chance
     */
    public double deadEndVanishChance() {
        return this.deadEndVanishChance;
    }
}
