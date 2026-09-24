package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

import java.util.Optional;

/**
 * Moves a candidate spot onto a floor the creek can stand on.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface Ground {

    /**
     * Finds the floor below or near a candidate spot.
     *
     * @param candidate the spot to check
     * @return the spot on the floor, or empty if there is no room to stand
     */
    Optional<Pos> settle(Pos candidate);
}
