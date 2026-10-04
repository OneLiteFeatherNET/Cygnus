package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;

import java.util.random.RandomGenerator;

/**
 * Applies the usual catch effects first, then lets the creek choose a trick: swap two survivors
 * with a chance of {@code swapChance}, otherwise throw the caught one into the air.
 * <p>
 * The swap needs a second survivor. Without one the creek throws instead, so that a catch always
 * does something the survivor can see. A launch of height 0 does nothing; that is how the launch
 * is switched off.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CatchTricks implements CatchConsequence {

    private final CatchConsequence base;
    private final CatchTrick swap;
    private final CatchTrick launch;
    private final double swapChance;
    private final RandomGenerator random;

    /**
     * Sets up the choice.
     *
     * @param base       the punishment for every catch, applied first
     * @param swap       swaps two survivors
     * @param launch     throws the caught survivor
     * @param swapChance chance for the swap, from 0 (always launch) to 1 (always swap)
     * @param random     the random source
     */
    public CatchTricks(CatchConsequence base, CatchTrick swap, CatchTrick launch, double swapChance,
                       RandomGenerator random) {
        this.base = base;
        this.swap = swap;
        this.launch = launch;
        this.swapChance = swapChance;
        this.random = random;
    }

    @Override
    public void apply(Player survivor) {
        this.base.apply(survivor);
        if (this.random.nextDouble() < this.swapChance && this.swap.perform(survivor)) {
            return;
        }
        this.launch.perform(survivor);
    }

    @Override
    public void cleanUp() {
        this.base.cleanUp();
        this.swap.cleanUp();
        this.launch.cleanUp();
    }
}
