package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekLinks;
import net.onelitefeather.cygnus.common.creek.CreekRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * The creek routes of a map, with the links between them worked out once up front, see
 * {@link CreekLinks}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekPaths {

    private final List<CreekRoute> routes;
    private final CreekLinks links;
    private final List<Pos> allPoints;

    private CreekPaths(List<CreekRoute> routes, CreekLinks links) {
        this.routes = routes;
        this.links = links;
        List<Pos> points = new ArrayList<>();
        for (CreekRoute route : routes) {
            route.points().forEach(point -> points.add(toPos(point.position())));
        }
        this.allPoints = List.copyOf(points);
    }

    /**
     * Takes the routes of a map and works out the links between them.
     *
     * @param routes       the valid routes of the map
     * @param linkDistance how close an end has to be to a point of another route to link to it, in blocks
     * @return the paths
     */
    public static CreekPaths of(List<CreekRoute> routes, double linkDistance) {
        List<CreekRoute> frozenRoutes = List.copyOf(routes);
        return new CreekPaths(frozenRoutes, CreekLinks.of(frozenRoutes, linkDistance));
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
     * The points linked to a point, always in the same order.
     *
     * @param node the point
     * @return the linked points, empty if there are none
     */
    public List<CreekLinks.Node> links(CreekLinks.Node node) {
        return this.links.of(node);
    }

    /**
     * Every point of every route.
     *
     * @return the points, unmodifiable
     */
    public List<Pos> allPoints() {
        return this.allPoints;
    }

    private static Pos toPos(Vec point) {
        return new Pos(point.x(), point.y(), point.z());
    }
}
