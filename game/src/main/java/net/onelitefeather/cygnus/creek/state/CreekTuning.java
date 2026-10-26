package net.onelitefeather.cygnus.creek.state;

import net.onelitefeather.cygnus.common.config.CreekConfig;

import java.util.random.RandomGenerator;

/**
 * The values the creek acts on right now: the settings, as the creek's mood and the round's calm
 * change them. The mood's factors apply first, the calm on top.
 * <p>
 * The states ask here instead of reading the settings themselves, so whatever changes the creek
 * during a round changes it in one place.
 * </p>
 *
 * @param config the settings
 * @param calm   how far the round has thinned out
 * @param mood   how the creek behaves since its mood switched
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public record CreekTuning(CreekConfig config, Calm calm, CreekMood mood) {

    /**
     * How fast the creek hunts.
     *
     * @return the speed
     */
    public double huntSpeed() {
        return this.calm.speed(this.config.hunt().speed() * this.mood.traits().huntSpeed());
    }

    /**
     * How fast the creek hunts while the hunted survivor looks at it.
     *
     * @return the speed, 0 when it freezes
     */
    public double watchedHuntSpeed() {
        return this.huntSpeed() * this.mood.traits().watchedHuntSpeed();
    }

    /**
     * How scared a stalked survivor has to be before the stalk turns into a hunt.
     *
     * @return the dread, from 0 to 1
     */
    public double huntThreshold() {
        return this.config.hunt().threshold() * this.mood.traits().huntThreshold();
    }

    /**
     * How long a stalk has to run before it may turn into a hunt.
     *
     * @return the time in milliseconds
     */
    public long huntMinStalkMillis() {
        return Math.round(this.config.hunt().minStalkSeconds() * 1000L * this.mood.traits().minStalk());
    }

    /**
     * Stretches a survivor's breather after a hunt.
     *
     * @param millis the breather in a full round, in milliseconds
     * @return the breather in this round
     */
    public long huntCooldown(long millis) {
        return this.calm.stretch(Math.round(millis * this.mood.traits().huntCooldown()));
    }

    /**
     * How long a stalked survivor has to look at the creek before it moves away.
     *
     * @return the time in milliseconds
     */
    public long revealMillis() {
        return this.calm.stretch((long) this.config.stalk().revealMillis());
    }

    /**
     * How long the creek may make no progress before it takes a shortcut.
     *
     * @return the time in milliseconds
     */
    public long stuckMillis() {
        return this.calm.stretch((long) this.config.stuckMillis());
    }

    /**
     * Stretches a distance the creek keeps to a survivor.
     *
     * @param distance the distance in a full round
     * @return the distance in this round
     */
    public double stretch(double distance) {
        return this.calm.stretch(distance);
    }

    /**
     * How long a new stalk lasts.
     *
     * @param random picks the length between {@code stalkMinSeconds} and {@code stalkMaxSeconds}
     * @return the length in milliseconds
     */
    public long stalkMillis(RandomGenerator random) {
        long seconds = random.nextLong(this.config.stalk().minSeconds(), this.config.stalk().maxSeconds() + 1L);
        return Math.round(seconds * 1000L * this.mood.traits().stalkDuration());
    }

    /**
     * How long a survivor's breather after a variant lasts: {@code vanishMaxSeconds} for a calm
     * survivor, down to {@code vanishMinSeconds} for one who is scared to death. A thinned out round
     * stretches it, so the last survivors are not haunted one after the other.
     *
     * @param dread the survivor's dread
     * @return the breather in milliseconds
     */
    public long variantBreatherMillis(double dread) {
        CreekConfig.Vanish vanish = this.config.vanish();
        double span = vanish.maxSeconds() - vanish.minSeconds();
        double seconds = vanish.maxSeconds() - span * Math.clamp(dread, 0.0D, 1.0D);
        return this.calm.stretch(Math.round(seconds * 1000.0D * this.mood.traits().variantBreather()));
    }

    /**
     * The chance that the creek vanishes at a dead end instead of resting there.
     *
     * @return the chance
     */
    public double deadEndVanishChance() {
        return this.mood.traits().deadEndVanishChance();
    }
}
