package net.onelitefeather.cygnus.creek;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
import net.onelitefeather.cygnus.creek.consequence.PatrolConsequence;
import net.onelitefeather.cygnus.creek.debug.CreekDebug;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.world.CreekPaths;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.Ground;
import net.onelitefeather.cygnus.creek.world.InstanceGround;
import net.onelitefeather.cygnus.creek.world.PathRoute;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Brings the creek into a round and takes it out again at the end.
 * <p>
 * Like {@code SlenderGazeService}, it listens for the start and end of a round on its own. It does
 * not need to hear about deaths or disconnects: those players are simply missing from the
 * survivors on the next step, so a variant whose target is gone ends within 100 ms anyway.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreekService.class);

    /** How long one step lasts, in milliseconds. */
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
    private volatile @Nullable CreekRound round;
    private final RepeatingTask task = new RepeatingTask(this::tick);
    private volatile @Nullable Creek creek;
    private volatile @Nullable CreekVariants variants;

    /**
     * Sets up the service.
     *
     * @param config      the settings
     * @param survivors   supplies the survivors of the round
     * @param instance    supplies the instance of the round, or {@code null} while there is none
     * @param routes      supplies the creek routes of the current map; without any, the creek stays away
     * @param bodies      puts a creek body into the world
     * @param dread       rates how scared each survivor is
     * @param consequence what happens on a catch
     * @param clock       the round's clock
     * @param random      the random source
     * @param debug       the debug line for playtests
     */
    public CreekService(CreekConfig config, Supplier<Set<Player>> survivors,
                          Supplier<? extends @Nullable Instance> instance, Supplier<List<CreekRoute>> routes,
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
    }

    /**
     * Listens for the start and end of a round.
     *
     * @param node the node to listen on
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(GameStartEvent.class, _ -> this.start());
        node.addListener(GameFinishEvent.class, _ -> this.stop());
    }

    /**
     * Puts the creek into the world, hidden at first, and starts stepping it.
     */
    void start() {
        if (this.creek != null) return;

        Instance world = this.instance.get();
        CreekPaths paths = CreekPaths.of(this.routes.get(), this.config.routeLinkDistance());
        if (world == null || paths.isEmpty()) {
            LOGGER.warn("The map has no usable creek routes, so the creek stays away this round");
            return;
        }
        // Every creek walks with its own cursor, so this one belongs to the patrolling creek alone.
        PathRoute pathRoute = new PathRoute(paths);
        CreekRound round = new CreekRound(this.sight, this.spots, this.consequence,
                new PatrolConsequence(paths::allPoints, this.ground, this.random), this.config, this.random);
        this.round = round;
        List<Pos> points = paths.allPoints();

        this.clock.start();
        Pos point = points.get(this.random.nextInt(points.size()));
        CreekBody body = this.bodies.apply(world, this.ground.settle(point).orElse(point));
        // Start hidden. The creek and its variants only show up once the survivors have had time to
        // spread out, the creek somewhere far away from all of them.
        long showsUp = this.clock.now() + this.config.vanishMinSeconds() * 1000L;
        this.creek = new Creek(body, pathRoute, round, new VanishState(showsUp));
        this.variants = new CreekVariants(this.config, this.spots, this.random, showsUp,
                (spot, state) -> new Creek(this.bodies.apply(world, spot), new PathRoute(paths), round, state));
        this.debug.setActive(true);
        this.task.start(TICK_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Runs one step: first the patrolling creek, then the variants. Survivors with a variant do not
     * see the patrolling creek, and it does not pick them out.
     * <p>
     * Once only one survivor is left and {@code activeWithLastSurvivor} is off, the patrolling creek
     * is gone for good and every variant ends.
     * </p>
     */
    void tick() {
        Creek current = this.creek;
        if (current == null) return;
        // One snapshot for every creek: where the survivors are and how scared they are is the same for all.
        SurvivorSnapshot survivors = SurvivorSnapshot.take(this.survivors.get(), this.dread);
        long now = this.clock.now();
        CreekVariants currentVariants = this.variants;
        // Count every survivor here: hiding the patrol from the haunted is not being down to the last one.
        if (!this.config.activeWithLastSurvivor() && survivors.size() <= 1) {
            current.vanishForGood(survivors, now);
            if (currentVariants != null) currentVariants.stop();
        } else {
            // A haunted survivor already has a creek of their own, so the patrolling one leaves them be.
            Set<UUID> haunted = currentVariants == null ? Set.of() : currentVariants.running().keySet();
            current.tick(survivors, haunted, now);
            if (currentVariants != null) currentVariants.tick(survivors, now);
        }
        if (this.debug.hasWatchers()) {
            Function<UUID, String> names = id -> nameOf(survivors, id);
            List<SurvivorView> views = current.lastViews();
            Component line = CreekDebug.line(current.state(), current.body().position(), views, names,
                    current.route().describe(), now);
            if (currentVariants != null) {
                List<CreekState> states = currentVariants.running().values().stream().map(Creek::state).toList();
                line = line.append(CreekDebug.variants(states, CreekVariants.capacity(views.size()), names, now));
            }
            this.debug.show(line);
        }
    }

    private static String nameOf(SurvivorSnapshot survivors, UUID id) {
        Player player = survivors.player(id);
        return player != null ? player.getUsername() : id.toString().substring(0, 8);
    }

    /**
     * Takes the creek and every variant out of the world and clears anything still running on the survivors.
     */
    void stop() {
        this.task.stop();
        CreekVariants currentVariants = this.variants;
        this.variants = null;
        if (currentVariants != null) currentVariants.stop();
        Creek current = this.creek;
        this.creek = null;
        this.clock.reset();
        if (current != null) current.remove();
        this.consequence.cleanUp();
        CreekRound currentRound = this.round;
        this.round = null;
        if (currentRound != null) currentRound.patrol().cleanUp();
        this.debug.setActive(false);
    }

    @Nullable Creek creek() {
        return this.creek;
    }

    @Nullable CreekVariants variants() {
        return this.variants;
    }

}
