package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Decides where the creek walks next.
 * <p>
 * {@link PathRoute} walks the hand-placed routes of a map.
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
     * @param current where the creek is
     * @param allowed which points the creek may go to
     * @param random  the random source
     * @return the next point and its pause, or empty if none is allowed
     */
    Optional<RouteStep> next(Pos current, Predicate<Pos> allowed, RandomGenerator random);

    /**
     * Every point this route can hand out. Used to find a place to appear or come back.
     *
     * @return the points, possibly none
     */
    default List<Pos> points() {
        return List.of();
    }

    /**
     * Describes where the creek is on this route, for the debug line.
     *
     * @return a short description, or an empty string if there is nothing to show
     */
    default String describe() {
        return "";
    }
}
