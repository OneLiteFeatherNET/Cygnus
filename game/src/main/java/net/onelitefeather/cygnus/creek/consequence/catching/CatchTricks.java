package net.onelitefeather.cygnus.creek.consequence.catching;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.state.CreekMood;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Applies the usual catch effects first, then lets the creek choose a trick: teleport the caught
 * survivor with a chance of {@code swapChance}, otherwise throw them into the air.
 * <p>
 * A teleport either swaps two survivors or sends the caught one to the slender spawn, each with
 * a chance of {@link #SPAWN_SHARE}. If the chosen teleport has no way to work, for example
 * because there is no second survivor, the creek tries the other one. Without either it throws
 * instead, so that a catch always does something the survivor can see. A launch of height 0 does
 * nothing. That is how the launch is switched off.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.15.0
 */
public final class CatchTricks implements CatchConsequence {

    /**
     * The share of teleports that go to the slender spawn instead of swapping two survivors.
     */
    static final double SPAWN_SHARE = 0.5D;

    private final CatchConsequence base;
    private final CatchTrick swap;
    private final CatchTrick spawnTeleport;
    private final CatchTrick launch;
    private final double swapChance;
    private final RandomGenerator random;
    private final Supplier<CreekMood> mood;

    /**
     * Sets up the choice.
     *
     * @param base          the punishment for every catch, applied first
     * @param swap          swaps two survivors
     * @param spawnTeleport sends the caught survivor to the slender spawn
     * @param launch        throws the caught survivor
     * @param swapChance    chance for a teleport, from 0 (always launch) to 1 (always teleport)
     * @param random        the random source
     * @param mood          the creek's mood right now. The trickster makes every catch teleport
     */
    public CatchTricks(
            CatchConsequence base,
            CatchTrick swap,
            CatchTrick spawnTeleport,
            CatchTrick launch,
            double swapChance,
            RandomGenerator random,
            Supplier<CreekMood> mood
    ) {
        this.base = base;
        this.swap = swap;
        this.spawnTeleport = spawnTeleport;
        this.launch = launch;
        this.swapChance = swapChance;
        this.random = random;
        this.mood = mood;
    }

    @Override
    public void apply(Player survivor) {
        this.base.apply(survivor);
        if (this.random.nextDouble() < this.teleportChance() && this.teleport(survivor)) {
            return;
        }
        this.launch.perform(survivor);
    }

    /**
     * Plays one of the two teleports, falling back to the other one. The trickster also uses this
     * at the end of a patrol stare.
     *
     * @param survivor the survivor who was caught
     * @return {@code true} if one of the teleports worked
     */
    public boolean teleport(Player survivor) {
        boolean toSpawn = this.random.nextDouble() < SPAWN_SHARE;
        CatchTrick first = toSpawn ? this.spawnTeleport : this.swap;
        CatchTrick second = toSpawn ? this.swap : this.spawnTeleport;
        return first.perform(survivor) || second.perform(survivor);
    }

    /**
     * The chance for a teleport instead of a throw: {@code swapChance}, or always for the
     * trickster.
     */
    private double teleportChance() {
        return this.mood.get() == CreekMood.TRICKSTER ? 1.0D : this.swapChance;
    }

    @Override
    public void cleanUp() {
        this.base.cleanUp();
        this.swap.cleanUp();
        this.spawnTeleport.cleanUp();
        this.launch.cleanUp();
    }
}
