package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;

/**
 * What happens when the creek catches a survivor.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface CatchConsequence {

    /**
     * Applies the consequence.
     *
     * @param survivor the survivor who was caught
     */
    void apply(Player survivor);

    /**
     * Removes any effects that are still active. Called at the end of a round.
     */
    default void cleanUp() {
    }
}
