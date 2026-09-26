package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;

/**
 * Gives a survivor's position away to the slender.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface SlenderReveal {

    /**
     * Shows the slender where a survivor is.
     *
     * @param survivor the survivor to give away
     * @param slender  the slender
     */
    void reveal(Player survivor, Player slender);

    /**
     * Ends every reveal that is still running.
     */
    default void cleanUp() {
    }
}
