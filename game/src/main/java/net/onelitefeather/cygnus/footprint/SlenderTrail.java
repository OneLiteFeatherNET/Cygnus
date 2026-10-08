package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.config.FootprintConfig;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.visibility.VisibilityRules;

import java.util.random.RandomGenerator;

/**
 * Lets the hidden slender leave a print now and then, which only survivors see.
 * <p>
 * Every few blocks a roll decides. The print appears a little later, so the newest one never
 * marks where he stands right now. Whether he was hidden counts at the step: turning visible
 * during the delay does not take the print back, he was hidden when he walked there.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class SlenderTrail {

    private static final double TICK_MILLIS = 50.0D;

    private final FootprintConfig config;
    private final RandomGenerator random;
    private final FootprintSpawner spawner;
    private final StepMeter meter;

    /**
     * Creates the trail.
     *
     * @param config  the step length, the chance, the delay and the lifetime
     * @param random  the source for the roll and the delay
     * @param spawner places the prints
     */
    SlenderTrail(FootprintConfig config, RandomGenerator random, FootprintSpawner spawner) {
        this.config = config;
        this.random = random;
        this.spawner = spawner;
        this.meter = new StepMeter(config.slenderStepBlocks(), config.teleportBlocks());
    }

    /**
     * Takes a move of the slender into account.
     *
     * @param slender the slender
     * @param from    where the move started
     * @param to      where the move ended
     */
    void moved(Player slender, Pos from, Pos to) {
        if (!this.meter.advance(from, to)) return;
        if (!VisibilityRules.isHidden(slender)) return;
        if (this.random.nextDouble() >= this.config.slenderChance()) return;
        Instance instance = slender.getInstance();
        if (instance == null) return;
        this.spawner.spawnLater(this.delayTicks(), instance, to, FootprintKind.SLENDER,
                this.config.slenderLifetimeSeconds(), TeamHelper::isSurvivorTeam);
    }

    /**
     * Drops the distance walked so far, for a new round or a new slender.
     */
    void reset() {
        this.meter.reset();
    }

    private int delayTicks() {
        int min = this.config.slenderDelayMinMillis();
        int max = this.config.slenderDelayMaxMillis();
        int millis = min == max ? min : this.random.nextInt(min, max + 1);
        return Math.max(1, (int) Math.ceil(millis / TICK_MILLIS));
    }
}
