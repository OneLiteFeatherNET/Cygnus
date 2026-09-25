package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Picks random waypoints from the map's page positions and survivor spawns.
 * <p>
 * The points are fetched on every step, because pages move during a round.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class RandomPointRoute implements RouteProvider {

    /** Points closer than this are skipped, in blocks. */
    static final double MIN_STEP = 4.0D;

    private final Supplier<List<Pos>> points;

    /**
     * Creates the route.
     *
     * @param points supplies the waypoints
     */
    public RandomPointRoute(Supplier<List<Pos>> points) {
        this.points = points;
    }

    /**
     * Returns the current waypoints.
     *
     * @return the points, possibly empty
     */
    @Override
    public List<Pos> points() {
        return this.points.get();
    }

    @Override
    public Optional<Pos> next(Pos current, Predicate<Pos> allowed, RandomGenerator random) {
        List<Pos> candidates = this.points.get().stream()
                .filter(point -> point.distance(current) >= MIN_STEP)
                .filter(allowed)
                .toList();
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }
}
