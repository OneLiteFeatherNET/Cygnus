package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Walks the creek along the routes of a map.
 * <p>
 * It remembers the route, the point and the direction. At the end of a route it picks at random
 * between turning around and every route linked to that end. If the creek is far from its last
 * point, for example after a teleport, it rejoins at the nearest point. Each step carries the pause
 * of its point, except for an end the creek walks away from.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PathRoute implements RouteProvider {

    /** Farther than this from its last waypoint, the creek was moved and rejoins, in blocks. */
    static final double REJOIN_DISTANCE = 8.0D;

    private final CreekPaths paths;
    private @Nullable Cursor cursor;

    /**
     * Creates the route walker.
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
        Cursor best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int route = 0; route < this.paths.size(); route++) {
            for (int index = 0; index < this.paths.pointCount(route); index++) {
                Pos point = this.paths.point(route, index);
                if (!allowed.test(point)) continue;
                double distance = point.distance(current);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new Cursor(route, index, 1);
                }
            }
        }
        if (best == null) return Optional.empty();

        int lastIndex = this.paths.pointCount(best.route()) - 1;
        // at an end there is only one way into the route
        int direction = best.index() == 0 ? 1 : best.index() == lastIndex ? -1 : (random.nextBoolean() ? 1 : -1);
        return this.moveTo(new Cursor(best.route(), best.index(), direction));
    }

    private Cursor advance(Cursor from, RandomGenerator random) {
        int nextIndex = from.index() + from.direction();
        if (nextIndex >= 0 && nextIndex < this.paths.pointCount(from.route())) {
            return new Cursor(from.route(), nextIndex, from.direction());
        }
        // walking backwards ends at the start, walking forwards at the end
        List<CreekPaths.End> links = this.paths.links(new CreekPaths.End(from.route(), from.direction() < 0));
        int choice = random.nextInt(links.size() + 1);
        if (choice == links.size()) {
            Cursor back = this.behind(from);
            return back != null ? back : from;
        }
        CreekPaths.End link = links.get(choice);
        return link.atStart()
                ? new Cursor(link.route(), 0, 1)
                : new Cursor(link.route(), this.paths.pointCount(link.route()) - 1, -1);
    }

    private @Nullable Cursor behind(Cursor from) {
        int direction = -from.direction();
        int index = from.index() + direction;
        if (index < 0 || index >= this.paths.pointCount(from.route())) return null;
        return new Cursor(from.route(), index, direction);
    }

    private Optional<RouteStep> moveTo(Cursor next) {
        this.cursor = next;
        return Optional.of(new RouteStep(this.point(next), this.pauseAt(next)));
    }

    /**
     * An end the creek walks away from is where it starts, not where it stops.
     */
    private int pauseAt(Cursor cursor) {
        int lastIndex = this.paths.pointCount(cursor.route()) - 1;
        boolean leavingStart = cursor.index() == 0 && cursor.direction() > 0;
        boolean leavingEnd = cursor.index() == lastIndex && cursor.direction() < 0;
        if (leavingStart || leavingEnd) return 0;
        return this.paths.pauseMillis(cursor.route(), cursor.index());
    }

    private Pos point(Cursor cursor) {
        return this.paths.point(cursor.route(), cursor.index());
    }

    /**
     * Where the creek is on the routes.
     *
     * @param route     the route's index
     * @param index     the index of the waypoint handed out last
     * @param direction {@code 1} towards the end, {@code -1} towards the start
     */
    private record Cursor(int route, int index, int direction) {
    }
}
