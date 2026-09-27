package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.creek.CreekLinks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Walks the creek along the routes of a map.
 * <p>
 * It remembers the route, the point and the direction. At the end of a route it picks at random
 * between turning around and any route linked to that end. Where another route begins or ends in
 * the middle of this one, it picks between walking on and turning onto that route. If the creek
 * ends up far from its last point, after a teleport for example, it joins back in at the nearest
 * point. Each step brings the pause of its point along, except for an end the creek is just leaving
 * or a point it just stepped over to from another route.
 * </p>
 * <p>
 * Because it remembers where its creek is, every creek needs a walker of its own. The
 * {@link CreekPaths} behind it can be shared.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PathRoute implements RouteProvider {

    /** Farther than this from its last waypoint, the creek must have been moved and joins back in, in blocks. */
    static final double REJOIN_DISTANCE = 8.0D;

    private final CreekPaths paths;
    private @Nullable Cursor cursor;

    /**
     * Sets up the route walker.
     *
     * @param paths the routes of the map
     */
    public PathRoute(CreekPaths paths) {
        this.paths = paths;
    }

    @Override
    public List<Pos> points() {
        return this.paths.allPoints();
    }

    @Override
    public Optional<RouteStep> next(Pos current, Predicate<Pos> allowed, RandomGenerator random) {
        Cursor last = this.cursor;
        if (last == null || current.distance(this.point(last)) > REJOIN_DISTANCE) {
            return this.rejoin(current, allowed, random);
        }
        Cursor ahead = this.advance(last, random);
        if (allowed.test(this.point(ahead))) return this.moveTo(ahead);

        Cursor back = this.behind(last);
        if (back != null && allowed.test(this.point(back))) return this.moveTo(back);
        return Optional.empty();
    }

    @Override
    public String describe() {
        Cursor last = this.cursor;
        if (last == null) return "";
        return this.paths.route(last.route()).name() + " " + (last.index() + 1) + "/" + this.paths.pointCount(last.route())
                + " " + (last.direction() > 0 ? "→" : "←");
    }

    private Optional<RouteStep> rejoin(Pos current, Predicate<Pos> allowed, RandomGenerator random) {
        CreekLinks.Node best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int route = 0; route < this.paths.size(); route++) {
            for (int index = 0; index < this.paths.pointCount(route); index++) {
                Pos point = this.paths.point(route, index);
                if (!allowed.test(point)) continue;
                double distance = point.distance(current);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new CreekLinks.Node(route, index);
                }
            }
        }
        if (best == null) return Optional.empty();
        return this.moveTo(this.enter(best, false, random));
    }

    /**
     * Picks the next waypoint. The last choice is always to carry on the way it is going, or to
     * turn around at the end of a route; every link from here is another choice.
     */
    private Cursor advance(Cursor from, RandomGenerator random) {
        int nextIndex = from.index() + from.direction();
        boolean atEnd = nextIndex < 0 || nextIndex >= this.paths.pointCount(from.route());
        // An end only offers its links once the creek gets there. In the middle, a crossing offers
        // them on the way through, unless the creek has only just come over from the other route.
        boolean crossing = !atEnd && !from.viaLink() && !this.isEnd(from);
        List<CreekLinks.Node> links = atEnd || crossing ? this.paths.links(this.node(from)) : List.of();
        int choice = atEnd || !links.isEmpty() ? random.nextInt(links.size() + 1) : 0;
        if (choice < links.size()) return this.enter(links.get(choice), true, random);
        if (!atEnd) return new Cursor(from.route(), nextIndex, from.direction(), false);
        Cursor back = this.behind(from);
        return back != null ? back : from;
    }

    /**
     * Puts the creek on a point. At an end there is only one way into the route, in the middle it
     * picks a way at random.
     */
    private Cursor enter(CreekLinks.Node node, boolean viaLink, RandomGenerator random) {
        int lastIndex = this.paths.pointCount(node.route()) - 1;
        int direction = node.index() == 0 ? 1 : node.index() == lastIndex ? -1 : (random.nextBoolean() ? 1 : -1);
        return new Cursor(node.route(), node.index(), direction, viaLink);
    }

    private @Nullable Cursor behind(Cursor from) {
        int direction = -from.direction();
        int index = from.index() + direction;
        if (index < 0 || index >= this.paths.pointCount(from.route())) return null;
        return new Cursor(from.route(), index, direction, false);
    }

    private Optional<RouteStep> moveTo(Cursor next) {
        this.cursor = next;
        return Optional.of(new RouteStep(this.point(next), this.pauseAt(next), this.isDeadEnd(next)));
    }

    /**
     * Tells whether the creek is heading for the end of a route that no other route is linked to.
     * Walking away from an end does not count.
     */
    private boolean isDeadEnd(Cursor cursor) {
        return this.reachesEnd(cursor) && this.paths.links(this.node(cursor)).isEmpty();
    }

    /**
     * An end the creek walks away from is where it sets off, not where it rests. Neither is a point
     * it just stepped over to from another route: it is where it already was.
     */
    private int pauseAt(Cursor cursor) {
        boolean leavingEnd = this.isEnd(cursor) && !this.reachesEnd(cursor);
        if (cursor.viaLink() || leavingEnd) return 0;
        return this.paths.pauseMillis(cursor.route(), cursor.index());
    }

    private Pos point(Cursor cursor) {
        return this.paths.point(cursor.route(), cursor.index());
    }

    private CreekLinks.Node node(Cursor cursor) {
        return new CreekLinks.Node(cursor.route(), cursor.index());
    }

    private boolean isEnd(Cursor cursor) {
        return cursor.index() == 0 || cursor.index() == this.paths.pointCount(cursor.route()) - 1;
    }

    /**
     * Tells whether the creek is walking into an end of its route rather than away from it.
     */
    private boolean reachesEnd(Cursor cursor) {
        return cursor.direction() > 0
                ? cursor.index() == this.paths.pointCount(cursor.route()) - 1
                : cursor.index() == 0;
    }

    /**
     * Where the creek is on the routes.
     *
     * @param route     the route's index
     * @param index     the index of the waypoint it was sent to last
     * @param direction {@code 1} towards the end, {@code -1} towards the start
     * @param viaLink   whether it just stepped over to this point from another route
     */
    private record Cursor(int route, int index, int direction, boolean viaLink) {
    }
}
