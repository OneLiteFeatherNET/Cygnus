package net.onelitefeather.cygnus.common.creek;

import net.minestom.server.coordinate.Vec;

import java.util.List;

/**
 * A named path for the creek. The first point is the start, the last one the end.
 *
 * @param name   the route's name, used in logs and in the debug line
 * @param points the waypoints in walking order
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record CreekRoute(String name, List<CreekWaypoint> points) {

    /** A route needs at least a start and an end. */
    public static final int MIN_POINTS = 2;

    /**
     * Fills in what a hand-written file may leave out, so a broken route can be reported by name
     * instead of breaking the whole file.
     */
    @SuppressWarnings("ConstantValue")
    public CreekRoute {
        name = name == null ? "" : name.trim();
        points = points == null ? List.of() : List.copyOf(points);
    }

    /**
     * Creates a route whose points have no pauses.
     *
     * @param name      the route's name
     * @param positions where the creek's feet stand, in walking order
     * @return the route
     */
    public static CreekRoute ofPositions(String name, List<Vec> positions) {
        return new CreekRoute(name, positions.stream().map(CreekWaypoint::of).toList());
    }
}
