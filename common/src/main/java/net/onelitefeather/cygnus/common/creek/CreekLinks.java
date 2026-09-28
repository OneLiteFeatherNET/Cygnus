package net.onelitefeather.cygnus.common.creek;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the creek routes of a map link up, worked out once up front.
 * <p>
 * An end of a route is linked to the nearest point of every other route that is at most the link
 * distance away. That point may be the other route's end, or somewhere in its middle, where the two
 * routes cross. Links work both ways. Two points in the middle of routes never link, and the two
 * ends of the same route never link to each other.
 * </p>
 * <p>
 * The game walks the creek along these links, and the setup marks them, so both always agree.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekLinks {

    /**
     * One point of a route.
     *
     * @param route the route's index
     * @param index the point's index within the route
     */
    public record Node(int route, int index) {
    }

    private final Map<Node, List<Node>> links;

    private CreekLinks(Map<Node, List<Node>> links) {
        this.links = links;
    }

    /**
     * Works out the links between the routes of a map.
     *
     * @param routes   the routes
     * @param distance how close an end has to be to a point of another route to link to it, in blocks
     * @return the links
     */
    public static CreekLinks of(List<CreekRoute> routes, double distance) {
        Map<Node, List<Node>> links = new HashMap<>();
        for (int route = 0; route < routes.size(); route++) {
            List<CreekWaypoint> points = routes.get(route).points();
            if (points.isEmpty()) continue;
            for (int endIndex : new int[]{0, points.size() - 1}) {
                Node end = new Node(route, endIndex);
                Vec position = points.get(endIndex).position();
                for (int other = 0; other < routes.size(); other++) {
                    if (other == route) continue;
                    Node nearest = nearest(routes.get(other), other, position, distance);
                    if (nearest == null) continue;
                    link(links, end, nearest);
                    link(links, nearest, end);
                }
            }
        }
        Map<Node, List<Node>> frozen = new HashMap<>();
        links.forEach((node, linked) -> frozen.put(node, List.copyOf(linked)));
        return new CreekLinks(Map.copyOf(frozen));
    }

    /**
     * The points linked to a point, always in the same order.
     *
     * @param node the point
     * @return the linked points, empty if there are none
     */
    public List<Node> of(Node node) {
        return this.links.getOrDefault(node, List.of());
    }

    /**
     * The point of a route closest to a position, as long as it is within the distance.
     */
    private static @Nullable Node nearest(CreekRoute route, int routeIndex, Vec position, double distance) {
        List<CreekWaypoint> points = route.points();
        Node nearest = null;
        double best = distance;
        for (int index = 0; index < points.size(); index++) {
            double away = points.get(index).position().distance(position);
            if (away <= best) {
                best = away;
                nearest = new Node(routeIndex, index);
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
}
