package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;

import java.util.UUID;

/**
 * What a state can do to the survivors, beyond moving the creek's body.
 * <p>
 * The states only know survivors by id. Whoever runs the creek looks the player up and applies the
 * consequence, so the states stay free of server objects.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public interface CreekActions {

    /**
     * The creek caught a survivor.
     *
     * @param survivor the id of the survivor
     */
    void caught(UUID survivor);

    /**
     * The patrolling creek picked out a survivor.
     *
     * @param survivor the id of the survivor
     */
    void selected(UUID survivor);

    /**
     * The creek vanished on purpose, in front of whoever is nearby.
     *
     * @param where where it vanished
     */
    void vanished(Pos where);
}
