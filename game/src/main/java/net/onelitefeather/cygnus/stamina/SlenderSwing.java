package net.onelitefeather.cygnus.stamina;

import net.onelitefeather.cygnus.vitality.Vitality;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Rolls what one swing of the slender does to one survivor.
 * <p>
 * A swing first decides whether it misses, then how hard it hits. Both lean on the survivor's fear
 * {@code f}, clamped to {@code 0} to {@code 1}: the miss chance is {@code 0.10 + 0.20 f}, a hit deals
 * between {@code 0.6 - 0.1 f} and {@code 1.25 - 0.25 f} hearts, evenly spread. A survivor who is
 * already terrified stands badly anyway, so the swing gives them a little more air, while a calm
 * survivor without a health bonus still dies within one appearance.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class SlenderSwing {

    /** Points of {@link Vitality} per heart: two health points make a heart. */
    public static final double POINTS_PER_HEART = 2 * Vitality.POINTS_PER_HEALTH;

    private static final double MISS_CALM = 0.10D;
    private static final double MISS_FEAR = 0.20D;
    private static final double MIN_CALM = 0.6D;
    private static final double MIN_FEAR = 0.1D;
    private static final double MAX_CALM = 1.25D;
    private static final double MAX_FEAR = 0.25D;

    private final Supplier<RandomGenerator> random;

    /**
     * Creates a swing that rolls with the given random source.
     *
     * @param random supplies the random source of the calling thread
     */
    public SlenderSwing(Supplier<RandomGenerator> random) {
        this.random = random;
    }

    /**
     * Creates the swing the game uses. A {@link ThreadLocalRandom} must never be shared between
     * threads, so it is fetched anew on every roll.
     *
     * @return the swing
     */
    public static SlenderSwing random() {
        return new SlenderSwing(ThreadLocalRandom::current);
    }

    /**
     * Rolls one swing against one survivor.
     *
     * @param fear the survivor's fear
     * @return the hearts the swing deals, {@code 0} for a miss
     */
    public double roll(double fear) {
        RandomGenerator generator = this.random.get();
        if (generator.nextDouble() < missChance(fear)) return 0.0D;
        double min = minHearts(fear);
        return min + generator.nextDouble() * (maxHearts(fear) - min);
    }

    static double missChance(double fear) {
        return MISS_CALM + MISS_FEAR * clamp(fear);
    }

    static double minHearts(double fear) {
        return MIN_CALM - MIN_FEAR * clamp(fear);
    }

    static double maxHearts(double fear) {
        return MAX_CALM - MAX_FEAR * clamp(fear);
    }

    private static double clamp(double fear) {
        return Math.clamp(fear, 0.0D, 1.0D);
    }
}
