package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

import java.util.Optional;

/**
 * Puts a candidate spot onto a floor the creek can stand on.
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

    /**
     * Checks that no block stands between two points.
     *
     * @param from where the line starts, usually someone's eyes
     * @param to   where the line ends
     * @return {@code true} if nothing is in the way. Without blocks to check, nothing ever is.
     */
    default boolean open(Pos from, Pos to) {
        return true;
    }
}
