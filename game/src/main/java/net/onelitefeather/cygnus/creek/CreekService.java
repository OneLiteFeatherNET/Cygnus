package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
import net.onelitefeather.cygnus.creek.debug.CreekDebug;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.world.CreekPaths;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.Ground;
import net.onelitefeather.cygnus.creek.world.InstanceGround;
import net.onelitefeather.cygnus.creek.world.PathRoute;
import net.onelitefeather.cygnus.creek.world.RandomPointRoute;
import net.onelitefeather.cygnus.creek.world.RouteProvider;
import net.onelitefeather.cygnus.creek.world.SpotFinder;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Adds the creek to a round and removes it at the end.
 * <p>
 * Like {@code SlenderGazeService}, it listens for the start and end of a round itself. There is
 * no listener for deaths or disconnects: those players drop out of the survivor list on the next
 * step, so a creek whose target is gone vanishes within 100 ms anyway.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreekService.class);

    /** Time between two steps, in milliseconds. */
    static final int TICK_MILLIS = 100;

    private final CreekConfig config;
    private final Supplier<Set<Player>> survivors;
    private final Supplier<? extends @Nullable Instance> instance;
    private final BiFunction<Instance, Pos, CreekBody> bodies;
    private final DreadSource dread;
    private final CatchConsequence consequence;
    private final RoundClock clock;
    private final RandomGenerator random;
    private final CreekDebug debug;
    private final CreekSight sight;
    private final Ground ground;
    private final SpotFinder spots;
    private final Supplier<List<CreekRoute>> routes;
    private final RandomPointRoute fallback;
    private RouteProvider route;
    private final RepeatingTask task = new RepeatingTask(this::tick);
    private volatile @Nullable Creek creek;

    /**
     * Creates the service.
     *
     * @param config      the settings
     * @param survivors   supplies the survivors of the round
     * @param instance    supplies the instance of the round, or {@code null} while there is none
     * @param routePoints supplies the waypoints
     * @param routes      supplies the creek routes of the current map
     * @param bodies      spawns the creek's body
     * @param dread       rates the survivors
     * @param consequence what happens on a catch
     * @param clock       the round's clock
     * @param random      the random source
     * @param debug       the debug line for playtests
     */
    public CreekService(CreekConfig config, Supplier<Set<Player>> survivors,
                          Supplier<? extends @Nullable Instance> instance, Supplier<List<Pos>> routePoints,
                          Supplier<List<CreekRoute>> routes,
                          BiFunction<Instance, Pos, CreekBody> bodies, DreadSource dread,
                          CatchConsequence consequence, RoundClock clock, RandomGenerator random,
                          CreekDebug debug) {
        this.config = config;
        this.survivors = survivors;
        this.instance = instance;
        this.bodies = bodies;
        this.dread = dread;
        this.consequence = consequence;
        this.clock = clock;
        this.random = random;
        this.debug = debug;
        this.sight = new CreekSight(config.sightRange(), config.sightViewAngle());
        this.ground = new InstanceGround(instance);
        this.spots = new SpotFinder(this.sight, this.ground);
        this.routes = routes;
        this.fallback = new RandomPointRoute(routePoints);
        this.route = this.fallback;
    }

    /**
     * Registers the listeners for the start and end of a round.
     *
     * @param node the node to register on
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(GameStartEvent.class, _ -> this.start());
        node.addListener(GameFinishEvent.class, _ -> this.stop());
    }

    /**
     * Spawns the creek (invisible at first) and starts the step task.
     */
    void start() {
        if (this.creek != null) return;

        Instance world = this.instance.get();
        CreekPaths paths = CreekPaths.of(this.routes.get(), this.config.routeLinkDistance());
        this.route = paths.isEmpty() ? this.fallback : new PathRoute(paths);
        List<Pos> points = this.route.points();
        if (world == null || points.isEmpty()) {
            LOGGER.warn("The map offers no creek routes, page positions or spawns to walk between, so the creek stays away this round");
            return;
        }

        this.clock.start();
        Pos point = points.get(this.random.nextInt(points.size()));
        CreekBody body = this.bodies.apply(world, this.ground.settle(point).orElse(point));
        // Start invisible. The creek shows up once the survivors had time to spread out, at a
        // spot far away from all of them.
        CreekState initial = new VanishState(this.clock.now() + this.config.vanishMinSeconds() * 1000L);
        this.creek = new Creek(body, this.sight, this.dread, this.route, this.spots, this.consequence,
                this.config, this.random, initial);
        this.debug.setActive(true);
        this.task.start(TICK_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Runs one step of the creek.
     */
    void tick() {
        Creek current = this.creek;
        if (current == null) return;
        Set<Player> players = this.survivors.get();
        current.tick(players, this.clock.now());
        if (this.debug.hasWatchers()) {
            this.debug.show(CreekDebug.line(current.state(), current.body().position(), current.lastViews(),
                    id -> nameOf(players, id), this.route.describe()));
        }
    }

    private static String nameOf(Set<Player> players, UUID id) {
        for (Player player : players) {
            if (player.getUuid().equals(id)) return player.getUsername();
        }
        return id.toString().substring(0, 8);
    }

    /**
     * Removes the creek and clears any remaining catch effects.
     */
    void stop() {
        this.task.stop();
        Creek current = this.creek;
        this.creek = null;
        this.clock.reset();
        if (current != null) current.remove();
        this.consequence.cleanUp();
        this.debug.setActive(false);
    }

    @Nullable Creek creek() {
        return this.creek;
    }

    RouteProvider route() {
        return this.route;
    }
}
