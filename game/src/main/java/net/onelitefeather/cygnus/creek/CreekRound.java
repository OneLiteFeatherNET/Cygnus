package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchConsequence;
import net.onelitefeather.cygnus.creek.consequence.CreekMarks;
import net.onelitefeather.cygnus.creek.consequence.PatrolHelper;
import net.onelitefeather.cygnus.creek.consequence.StalkSounds;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.state.CreekTuning;
import net.onelitefeather.cygnus.creek.state.HuntCooldowns;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.SpotFinder;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * What every creek of a round shares: the patrolling one and each variant.
 *
 * @param sight       decides whether a survivor sees a creek
 * @param spots       finds places where nobody can see a creek
 * @param consequence what happens on a catch
 * @param marks       sometimes gives a survivor away to the slender outside of a catch
 * @param witness     hears about catches and sightings
 * @param patrol      what the patrolling creek does to the survivors around it
 * @param stalk       lets a stalked survivor hear the creek
 * @param config      the settings
 * @param settings    the distances the map sets for the creek
 * @param random      the random source
 * @param hunts       when each survivor's last hunt ended
 * @param tuning      the values every creek acts on right now
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
record CreekRound(
        CreekSight sight,
        SpotFinder spots,
        CatchConsequence consequence,
        CreekMarks marks,
        CreekWitness witness,
        PatrolHelper patrol,
        StalkSounds stalk,
        CreekConfig config,
        CreekSettings settings,
        RandomGenerator random,
        HuntCooldowns hunts,
        Supplier<CreekTuning> tuning
) {
}
