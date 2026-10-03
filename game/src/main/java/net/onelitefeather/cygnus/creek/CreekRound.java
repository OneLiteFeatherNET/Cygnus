package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
import net.onelitefeather.cygnus.creek.consequence.PatrolHelper;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.SpotFinder;

import java.util.random.RandomGenerator;

/**
 * What every creek of a round shares: the patrolling one and each variant.
 *
 * @param sight       decides whether a survivor sees a creek
 * @param spots       finds places where nobody can see a creek
 * @param consequence what happens on a catch
 * @param witness     hears about catches and sightings
 * @param patrol      what the patrolling creek does to the survivors around it
 * @param config      the settings
 * @param random      the random source
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
record CreekRound(
        CreekSight sight,
        SpotFinder spots,
        CatchConsequence consequence,
        CreekWitness witness,
        PatrolHelper patrol,
        CreekConfig config,
        RandomGenerator random
) {
}
