package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;

/**
 * What happens to a survivor the creek catches.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface CatchConsequence {

    /**
     * Lets the catch hit the survivor.
     *
     * @param survivor the survivor who was caught
     */
    void apply(Player survivor);

    /**
     * Takes back anything that is still running. Called when the round ends.
     */
    default void cleanUp() {
    }
}
