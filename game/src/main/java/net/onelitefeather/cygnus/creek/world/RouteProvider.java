package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Decides where the creek walks next.
 * <p>
 * For now the waypoints come from the map's pages and spawns. Hand-placed waypoints from the
 * setup can be added later as another implementation.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface RouteProvider {

    /**
     * Picks the next point.
     *
     * @param current the creek's position
     * @param allowed filters the points the creek may go to
     * @param random  the random source
     * @return the next point, or empty if none is allowed
     */
    Optional<Pos> next(Pos current, Predicate<Pos> allowed, RandomGenerator random);
}
