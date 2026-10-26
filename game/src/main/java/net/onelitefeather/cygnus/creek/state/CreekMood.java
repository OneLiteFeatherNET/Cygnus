package net.onelitefeather.cygnus.creek.state;

import org.jetbrains.annotations.Contract;

/**
 * How the creek behaves after its mood switched, see {@code MoodClock}.
 * <p>
 * Every round starts {@link #NORMAL}. The other moods change how often and how hard the creek
 * uses its moves, see {@link Traits}. Each mood only names what sets it apart from the normal
 * creek. The factors multiply the settings before the round's calm applies.
 * </p>
 * <p>
 * The moves only one mood has are asked for by the mood itself: the shadow standing behind a
 * teammate, the trickster's doppelgangers and its teleports.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.21.0
 */
public enum CreekMood {

    /** The creek everyone knows. */
    NORMAL(Traits.NORMAL),

    /** Hunts sooner, faster and again sooner, and keeps creeping while it is watched. */
    HUNTER(Traits.builder()
            .huntThreshold(0.7D)
            .minStalk(0.5D)
            .huntSpeed(1.2D)
            .huntCooldown(0.6D)
            .watchedHuntSpeed(0.25D)
            .build()),

    /** Stalks longer and more often, comes back closer, marks more and hunts less. */
    SHADOW(Traits.builder()
            .huntThreshold(1.3D)
            .stalkDuration(1.5D)
            .variantBreather(0.7D)
            .spotMarkChance(0.5D)
            .build()),

    /** Teleports whoever it gets hold of and vanishes more. */
    TRICKSTER(Traits.builder()
            .deadEndVanishChance(0.8D)
            .build());

    private final Traits traits;

    CreekMood(Traits traits) {
        this.traits = traits;
    }

    /**
     * Tells how this mood changes the creek's values.
     *
     * @return the values
     */
    public Traits traits() {
        return this.traits;
    }

    /**
     * The values a mood changes.
     *
     * @param huntThreshold       the factor on the dread a stalked survivor needs before a hunt
     * @param minStalk            the factor on how long a stalk has to run before a hunt
     * @param huntSpeed           the factor on the hunt speed
     * @param huntCooldown        the factor on a survivor's breather after a hunt
     * @param watchedHuntSpeed    the share of the hunt speed the creek keeps while the hunted
     *                            survivor looks at it, 0 when it freezes
     * @param stalkDuration       the factor on how long a stalk lasts
     * @param variantBreather     the factor on a survivor's breather after a variant
     * @param spotMarkChance      the chance that a stalked survivor who looked at the creek is
     *                            marked for the slender
     * @param deadEndVanishChance the chance that the creek vanishes at a dead end instead of
     *                            resting there
     */
    public record Traits(double huntThreshold, double minStalk, double huntSpeed, double huntCooldown,
                         double watchedHuntSpeed, double stalkDuration, double variantBreather,
                         double spotMarkChance, double deadEndVanishChance) {

        /** The values of the creek everyone knows. */
        public static final Traits NORMAL = builder().build();

        /**
         * Starts from the values of the creek everyone knows.
         *
         * @return the builder
         */
        @Contract(pure = true, value = "-> new")
        static Builder builder() {
            return new Builder();
        }

        /**
         * Builds the values of a mood. Everything it is not told keeps the value of the normal
         * creek.
         */
        static final class Builder {

            private double huntThreshold = 1.0D;
            private double minStalk = 1.0D;
            private double huntSpeed = 1.0D;
            private double huntCooldown = 1.0D;
            private double watchedHuntSpeed = 0.0D;
            private double stalkDuration = 1.0D;
            private double variantBreather = 1.0D;
            private double spotMarkChance = 0.25D;
            private double deadEndVanishChance = 0.5D;

            private Builder() {
            }

            Builder huntThreshold(double factor) {
                this.huntThreshold = factor;
                return this;
            }

            Builder minStalk(double factor) {
                this.minStalk = factor;
                return this;
            }

            Builder huntSpeed(double factor) {
                this.huntSpeed = factor;
                return this;
            }

            Builder huntCooldown(double factor) {
                this.huntCooldown = factor;
                return this;
            }

            Builder watchedHuntSpeed(double share) {
                this.watchedHuntSpeed = share;
                return this;
            }

            Builder stalkDuration(double factor) {
                this.stalkDuration = factor;
                return this;
            }

            Builder variantBreather(double factor) {
                this.variantBreather = factor;
                return this;
            }

            Builder spotMarkChance(double chance) {
                this.spotMarkChance = chance;
                return this;
            }

            Builder deadEndVanishChance(double chance) {
                this.deadEndVanishChance = chance;
                return this;
            }

            Traits build() {
                return new Traits(this.huntThreshold, this.minStalk, this.huntSpeed, this.huntCooldown,
                        this.watchedHuntSpeed, this.stalkDuration, this.variantBreather, this.spotMarkChance,
                        this.deadEndVanishChance);
            }
        }
    }
}
