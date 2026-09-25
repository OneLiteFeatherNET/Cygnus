package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The creek routes of a map, with the links between their ends worked out once.
 * <p>
 * Two ends are linked when they are at most the link distance apart. The two ends of one route
 * never link to each other.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekPaths {

    /**
     * One end of a route.
     *
     * @param route   the route's index
     * @param atStart {@code true} for the first point, {@code false} for the last
     */
    public record End(int route, boolean atStart) {
    }

    private static final boolean[] BOTH_ENDS = {true, false};

    private final List<CreekRoute> routes;
    private final Map<End, List<End>> links;

    private CreekPaths(List<CreekRoute> routes, Map<End, List<End>> links) {
        this.routes = routes;
        this.links = links;
    }

    /**
     * Builds the paths and works out the links.
     *
     * @param routes       the valid routes of the map
     * @param linkDistance how close two ends have to be to count as linked, in blocks
     * @return the paths
     */
    public static CreekPaths of(List<CreekRoute> routes, double linkDistance) {
        List<CreekRoute> frozenRoutes = List.copyOf(routes);
        Map<End, List<End>> links = new HashMap<>();
        for (int firstRoute = 0; firstRoute < frozenRoutes.size(); firstRoute++) {
            for (int secondRoute = firstRoute + 1; secondRoute < frozenRoutes.size(); secondRoute++) {
                for (boolean firstAtStart : BOTH_ENDS) {
                    for (boolean secondAtStart : BOTH_ENDS) {
                        End first = new End(firstRoute, firstAtStart);
                        End second = new End(secondRoute, secondAtStart);
                        if (position(frozenRoutes, first).distance(position(frozenRoutes, second)) <= linkDistance) {
                            links.computeIfAbsent(first, _ -> new ArrayList<>()).add(second);
                            links.computeIfAbsent(second, _ -> new ArrayList<>()).add(first);
                        }
                    }
                }
            }
        }
        return new CreekPaths(frozenRoutes, links);
    }

    /**
     * Returns whether there are no routes.
     *
     * @return {@code true} without routes
     */
    public boolean isEmpty() {
        return this.routes.isEmpty();
    }

    /**
     * Returns the number of routes.
     *
     * @return the number of routes
     */
    public int size() {
        return this.routes.size();
    }

    /**
     * Returns a route.
     *
     * @param index the route's index
     * @return the route
     */
    public CreekRoute route(int index) {
        return this.routes.get(index);
    }

    /**
     * Returns how many points a route has.
     *
     * @param route the route's index
     * @return the number of points
     */
    public int pointCount(int route) {
        return this.routes.get(route).points().size();
    }

    /**
     * Returns one point of a route.
     *
     * @param route the route's index
     * @param index the point's index
     * @return the point
     */
    public Pos point(int route, int index) {
        return toPos(this.routes.get(route).points().get(index));
    }

    /**
     * Returns where an end of a route is.
     *
     * @param end the end
     * @return the first or last point of the route
     */
    public Pos position(End end) {
        return position(this.routes, end);
    }

    /**
     * Returns the ends linked to an end, in a fixed order.
     *
     * @param end the end
     * @return the linked ends, empty if there are none
     */
    public List<End> links(End end) {
        return List.copyOf(this.links.getOrDefault(end, List.of()));
    }

    /**
     * Returns every point of every route.
     *
     * @return the points
     */
    public List<Pos> allPoints() {
        List<Pos> points = new ArrayList<>();
        for (CreekRoute route : this.routes) {
            route.points().forEach(point -> points.add(toPos(point)));
        }
        return points;
    }

    private static Pos position(List<CreekRoute> routes, End end) {
        List<Vec> points = routes.get(end.route()).points();
        return toPos(end.atStart() ? points.getFirst() : points.getLast());
    }

    private static Pos toPos(Vec point) {
        return new Pos(point.x(), point.y(), point.z());
    }
}
