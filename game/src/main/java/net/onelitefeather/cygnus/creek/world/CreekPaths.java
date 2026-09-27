package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The creek routes of a map, with the links between them worked out once up front.
 * <p>
 * An end of a route is linked to the nearest point of every other route that is at most the link
 * distance away. That point may be the other route's end, or somewhere in its middle, where the two
 * routes cross. Links work both ways. Two points in the middle of routes never link, and the two
 * ends of the same route never link to each other.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekPaths {

    /**
     * One point of a route.
     *
     * @param route the route's index
     * @param index the point's index within the route
     */
    public record Node(int route, int index) {
    }

    private final List<CreekRoute> routes;
    private final Map<Node, List<Node>> links;
    private final List<Pos> allPoints;

    private CreekPaths(List<CreekRoute> routes, Map<Node, List<Node>> links) {
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
        Map<Node, List<Node>> links = new HashMap<>();
        for (int route = 0; route < frozenRoutes.size(); route++) {
            int lastIndex = frozenRoutes.get(route).points().size() - 1;
            for (int endIndex : new int[]{0, lastIndex}) {
                Node end = new Node(route, endIndex);
                for (int other = 0; other < frozenRoutes.size(); other++) {
                    if (other == route) continue;
                    Node nearest = nearest(frozenRoutes, other, toPos(position(frozenRoutes, end)), linkDistance);
                    if (nearest == null) continue;
                    link(links, end, nearest);
                    link(links, nearest, end);
                }
            }
        }
        return new CreekPaths(frozenRoutes, links);
    }

    /**
     * The point of a route closest to a position, as long as it is within the distance.
     */
    private static @Nullable Node nearest(List<CreekRoute> routes, int route, Pos position, double distance) {
        List<CreekWaypoint> points = routes.get(route).points();
        Node nearest = null;
        double best = distance;
        for (int index = 0; index < points.size(); index++) {
            double away = toPos(points.get(index).position()).distance(position);
            if (away <= best) {
                best = away;
                nearest = new Node(route, index);
            }
        }
        return nearest;
    }

    /**
     * Adds a link once. Two ends close together are found from both sides.
     */
    private static void link(Map<Node, List<Node>> links, Node from, Node to) {
        List<Node> linked = links.computeIfAbsent(from, _ -> new ArrayList<>());
        if (!linked.contains(to)) linked.add(to);
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
    public List<Node> links(Node node) {
        return List.copyOf(this.links.getOrDefault(node, List.of()));
    }

    /**
     * Every point of every route.
     *
     * @return the points, unmodifiable
     */
    public List<Pos> allPoints() {
        return this.allPoints;
    }

    private static Vec position(List<CreekRoute> routes, Node node) {
        return routes.get(node.route()).points().get(node.index()).position();
    }

    private static Pos toPos(Vec point) {
        return new Pos(point.x(), point.y(), point.z());
    }
}
