package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;

/**
 * Reveals a survivor's position to the slender.
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
     * @param survivor the survivor to reveal
     * @param slender  the slender
     */
    void reveal(Player survivor, Player slender);

    /**
     * Ends all running reveals.
     */
    default void cleanUp() {
    }
}
