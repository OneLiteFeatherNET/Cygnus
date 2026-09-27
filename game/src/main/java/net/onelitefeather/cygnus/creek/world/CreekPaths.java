package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The creek routes of a map, with the links between their ends worked out once up front.
 * <p>
 * Two ends are linked when they are at most the link distance apart. The two ends of the same route
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
     * @param atStart {@code true} for its first point, {@code false} for its last
     */
    public record End(int route, boolean atStart) {
    }

    private static final boolean[] BOTH_ENDS = {true, false};

    private final List<CreekRoute> routes;
    private final Map<End, List<End>> links;
    private final List<Pos> allPoints;

    private CreekPaths(List<CreekRoute> routes, Map<End, List<End>> links) {
        this.routes = routes;
        this.links = links;
        List<Pos> points = new ArrayList<>();
        for (CreekRoute route : routes) {
            route.points().forEach(point -> points.add(toPos(point.position())));
        }
        this.allPoints = List.copyOf(points);
    }

    /**
     * Takes the routes of a map and works out which ends are linked.
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
     * Tells whether the map has no routes at all.
     *
     * @return {@code true} without routes
     */
    public boolean isEmpty() {
        return this.routes.isEmpty();
    }

    /**
     * How many routes there are.
     *
     * @return the number of routes
     */
    public int size() {
        return this.routes.size();
    }

    /**
     * One of the routes.
     *
     * @param index the route's index
     * @return the route
     */
    public CreekRoute route(int index) {
        return this.routes.get(index);
    }

    /**
     * How many points a route has.
     *
     * @param route the route's index
     * @return the number of points
     */
    public int pointCount(int route) {
        return this.routes.get(route).points().size();
    }

    /**
     * One point of a route.
     *
     * @param route the route's index
     * @param index the point's index
     * @return the point
     */
    public Pos point(int route, int index) {
        return toPos(this.routes.get(route).points().get(index).position());
    }

    /**
     * How long the creek rests at one point of a route.
     *
     * @param route the route's index
     * @param index the point's index
     * @return the pause, in milliseconds
     */
    public int pauseMillis(int route, int index) {
        return this.routes.get(route).points().get(index).pauseMillis();
    }

    /**
     * Where an end of a route is.
     *
     * @param end the end
     * @return the route's first or last point
     */
    public Pos position(End end) {
        return position(this.routes, end);
    }

    /**
     * The ends linked to an end, always in the same order.
     *
     * @param end the end
     * @return the linked ends, empty if there are none
     */
    public List<End> links(End end) {
        return List.copyOf(this.links.getOrDefault(end, List.of()));
    }

    /**
     * Every point of every route.
     *
     * @return the points, unmodifiable
     */
    public List<Pos> allPoints() {
        return this.allPoints;
    }

    private static Pos position(List<CreekRoute> routes, End end) {
        List<CreekWaypoint> points = routes.get(end.route()).points();
        return toPos((end.atStart() ? points.getFirst() : points.getLast()).position());
    }

    private static Pos toPos(Vec point) {
        return new Pos(point.x(), point.y(), point.z());
    }
}
