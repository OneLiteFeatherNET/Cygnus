package net.onelitefeather.cygnus.lure;

import net.minestom.server.entity.Player;

/**
 * What a lure does to the survivor once the figure has struck.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public interface LureConsequence {

    /**
     * Lets the scare hit the survivor right when the figure strikes.
     *
     * @param survivor the survivor who was lured
     * @param now      the current time in milliseconds
     */
    void struck(Player survivor, long now);

    /**
     * Follows the scare up once the phantom has dissolved.
     *
     * @param survivor the survivor who was lured
     */
    void followUp(Player survivor);

    /**
     * Takes back anything that is still running. Called when the round ends.
     */
    default void cleanUp() {
    }
}
