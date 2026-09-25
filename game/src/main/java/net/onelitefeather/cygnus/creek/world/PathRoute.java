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
 * point, for example after a teleport, it rejoins at the nearest point.
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
    public Optional<Pos> next(Pos current, Predicate<Pos> allowed, RandomGenerator random) {
        Cursor at = this.cursor;
        if (at == null || current.distance(this.point(at)) > REJOIN_DISTANCE) {
            return this.rejoin(current, allowed, random);
        }
        Cursor ahead = this.advance(at, random);
        if (allowed.test(this.point(ahead))) return this.moveTo(ahead);

        Cursor back = this.behind(at);
        if (back != null && allowed.test(this.point(back))) return this.moveTo(back);
        return Optional.empty();
    }

    @Override
    public String describe() {
        Cursor at = this.cursor;
        if (at == null) return "";
        return this.paths.route(at.route()).name() + " " + (at.index() + 1) + "/" + this.paths.pointCount(at.route())
                + " " + (at.direction() > 0 ? "→" : "←");
    }

    private Optional<Pos> rejoin(Pos current, Predicate<Pos> allowed, RandomGenerator random) {
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

        int last = this.paths.pointCount(best.route()) - 1;
        // at an end there is only one way into the route
        int direction = best.index() == 0 ? 1 : best.index() == last ? -1 : (random.nextBoolean() ? 1 : -1);
        return this.moveTo(new Cursor(best.route(), best.index(), direction));
    }

    private Cursor advance(Cursor at, RandomGenerator random) {
        int nextIndex = at.index() + at.direction();
        if (nextIndex >= 0 && nextIndex < this.paths.pointCount(at.route())) {
            return new Cursor(at.route(), nextIndex, at.direction());
        }
        // walking backwards ends at the start, walking forwards at the end
        List<CreekPaths.End> links = this.paths.links(new CreekPaths.End(at.route(), at.direction() < 0));
        int choice = random.nextInt(links.size() + 1);
        if (choice == links.size()) {
            Cursor back = this.behind(at);
            return back != null ? back : at;
        }
        CreekPaths.End link = links.get(choice);
        return link.atStart()
                ? new Cursor(link.route(), 0, 1)
                : new Cursor(link.route(), this.paths.pointCount(link.route()) - 1, -1);
    }

    private @Nullable Cursor behind(Cursor at) {
        int direction = -at.direction();
        int index = at.index() + direction;
        if (index < 0 || index >= this.paths.pointCount(at.route())) return null;
        return new Cursor(at.route(), index, direction);
    }

    private Optional<Pos> moveTo(Cursor next) {
        this.cursor = next;
        return Optional.of(this.point(next));
    }

    private Pos point(Cursor at) {
        return this.paths.point(at.route(), at.index());
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
