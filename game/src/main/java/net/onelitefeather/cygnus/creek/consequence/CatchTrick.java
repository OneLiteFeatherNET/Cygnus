package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;

/**
 * One of the punishments the creek chooses between on a catch, on top of the usual effects.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface CatchTrick {

    /**
     * Plays the trick on a caught survivor.
     *
     * @param survivor the survivor who was caught
     * @return {@code true} if the trick could be played, {@code false} if it had no way to work
     * and the creek should choose another
     */
    boolean perform(Player survivor);

    /**
     * Takes back anything that is still running. Called when the round ends.
     */
    default void cleanUp() {
    }
}
