package net.onelitefeather.cygnus.creek.state;

/**
 * How far the round has thinned out, and how much calmer the creek gets for it.
 * <p>
 * The fewer of the survivors who started the round are still alive, the slower the creek hunts and
 * the longer it waits before it jumps, takes a shortcut or comes back for someone. Without this the
 * last few survivors would take all the pressure the creek used to spread over the whole round.
 * </p>
 *
 * @param value {@code 0} while everyone who started is alive, {@code 1} when only one is left
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.18.0
 */
public record Calm(double value) {

    /** A round that has not thinned out: the creek behaves as it always did. */
    public static final Calm NONE = new Calm(0.0D);

    /** The share of its hunting speed the creek keeps when only one survivor is left. */
    static final double CALMEST_SPEED = 0.8D;

    /** How much longer the creek waits when only one survivor is left. */
    static final double CALMEST_PATIENCE = 1.5D;

    /**
     * Works out how far a round has thinned out.
     *
     * @param started how many survivors the round had at its fullest
     * @param alive   how many of them are still alive
     * @return the calm; a round that started with a single survivor has no course to follow
     */
    public static Calm of(int started, int alive) {
        if (started <= 1) return NONE;
        return new Calm(Math.clamp((double) (started - alive) / (started - 1), 0.0D, 1.0D));
    }

    /**
     * Slows a speed down by the calm.
     *
     * @param speed the speed in a full round
     * @return the speed in this round
     */
    public double speed(double speed) {
        return speed * (1.0D - (1.0D - CALMEST_SPEED) * this.value);
    }

    /**
     * Stretches a wait by the calm.
     *
     * @param millis the wait in a full round, in milliseconds
     * @return the wait in this round
     */
    public long stretch(long millis) {
        return Math.round(millis * this.patience());
    }

    /**
     * Stretches a distance by the calm.
     *
     * @param distance the distance in a full round
     * @return the distance in this round
     */
    public double stretch(double distance) {
        return distance * this.patience();
    }

    private double patience() {
        return 1.0D + (CALMEST_PATIENCE - 1.0D) * this.value;
    }
}
