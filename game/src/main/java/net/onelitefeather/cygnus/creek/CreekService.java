package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchConsequence;
import net.onelitefeather.cygnus.creek.consequence.catching.CatchTrick;
import net.onelitefeather.cygnus.creek.consequence.CreekMarks;
import net.onelitefeather.cygnus.creek.consequence.PatrolHelper;
import net.onelitefeather.cygnus.creek.consequence.StalkSounds;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.creek.state.CreekTuning;
import net.onelitefeather.cygnus.creek.state.HuntCooldowns;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.DoneState;
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
import java.util.function.LongSupplier;
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
 * @version 1.2.0
 * @since 2.15.0
 */
public final class CreekService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreekService.class);

    /** How long one step lasts, in milliseconds. */
    static final int TICK_MILLIS = 100;

    private final CreekConfig config;
    private final CreekSettings settings;
    private final Supplier<Set<Player>> survivors;
    private final Supplier<? extends @Nullable Instance> instance;
    private final BiFunction<Instance, Pos, CreekBody> bodies;
    private final DreadSource dread;
    private final CreekWitness witness;
    private final CreekRespite respite;
    private final CatchConsequence consequence;
    private final CreekMarks marks;
    private final CreekPressure pressure;
    private final CatchTrick stareTrick;
    private final LongSupplier clock;
    private final RandomGenerator random;
    private final CreekSight sight;
    private final Ground ground;
    private final SpotFinder spots;
    private final Supplier<List<CreekRoute>> routes;
    private volatile @Nullable CreekRound round;
    private final RepeatingTask task;
    private volatile @Nullable Creek creek;
    private volatile @Nullable CreekVariants variants;

    /**
     * Sets up the service, with its step measured for the slow tick report.
     *
     * @param config      the settings
     * @param settings    the distances the map sets for the creek
     * @param survivors   supplies the survivors of the round
     * @param instance    supplies the instance of the round, or {@code null} while there is none
     * @param routes      supplies the creek routes of the current map; without any, the creek stays away
     * @param bodies      puts a creek body into the world
     * @param dread       rates how scared each survivor is
     * @param witness     hears about catches and sightings
     * @param respite     tells who the creek leaves alone for now
     * @param consequence what happens on a catch
     * @param marks       sometimes gives a survivor away to the slender outside of a catch
     * @param mood        decides when the creek's mood switches and to which
     * @param stareTrick  teleports a survivor at the end of a stare, for a mood that does so
     * @param clock       supplies the current time in milliseconds
     * @param random      the random source
     * @param sections    measures how long each step takes
     * @since 2.15.0
     */
    public CreekService(CreekConfig config, CreekSettings settings, Supplier<Set<Player>> survivors,
                          Supplier<? extends @Nullable Instance> instance, Supplier<List<CreekRoute>> routes,
                          BiFunction<Instance, Pos, CreekBody> bodies, DreadSource dread, CreekWitness witness,
                          CreekRespite respite, CatchConsequence consequence, CreekMarks marks, MoodClock mood, CatchTrick stareTrick, LongSupplier clock, RandomGenerator random,
                          TickSections sections) {
        this.task = new RepeatingTask(sections.wrap(TickSectionNames.CREEK, this::tick));
        this.config = config;
        this.settings = settings;
        this.survivors = survivors;
        this.instance = instance;
        this.bodies = bodies;
        this.dread = dread;
        this.witness = witness;
        this.respite = respite;
        this.consequence = consequence;
        this.marks = marks;
        this.pressure = new CreekPressure(config, mood);
        this.stareTrick = stareTrick;
        this.clock = clock;
        this.random = random;
        this.sight = new CreekSight(settings.sightRange(), config.sight().viewAngle());
        this.ground = new InstanceGround(instance);
        this.spots = new SpotFinder(settings.sightRange(), this.ground);
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
        node.addListener(PageFoundEvent.class, _ -> this.pressure.pageFound(this.clock.getAsLong()));
    }

    /**
     * Puts the creek into the world, hidden at first, and starts stepping it.
     */
    void start() {
        if (this.creek != null) return;

        Instance world = this.instance.get();
        CreekPaths paths = CreekPaths.of(this.routes.get(), this.settings.routeLinkDistance());
        if (world == null || paths.isEmpty()) {
            LOGGER.warn("The map has no usable creek routes, so the creek stays away this round");
            return;
        }
        // Every creek walks with its own cursor, so this one belongs to the patrolling creek alone.
        PathRoute pathRoute = new PathRoute(paths);
        Decoys decoys = new Decoys(this.bodies, this.sight, () -> this.tuning().revealMillis(), this.clock);
        CreekRound round = new CreekRound(this.sight, this.spots, this.consequence, this.marks, this.stareTrick, decoys, this.witness,
                new PatrolHelper(this.random), new StalkSounds(this.random), this.config, this.settings, this.random,
                new HuntCooldowns(this.config.hunt().cooldownSeconds() * 1000L), this::tuning);
        this.round = round;
        List<Pos> points = paths.allPoints();

        Pos point = points.get(this.random.nextInt(points.size()));
        CreekBody body = this.bodies.apply(world, this.ground.settle(point).orElse(point));
        // Start hidden. The creek and its variants only show up once the survivors have had time to
        // spread out, the creek somewhere far away from all of them.
        long showsUp = this.clock.getAsLong() + this.config.vanish().minSeconds() * 1000L;
        this.creek = new Creek(body, pathRoute, round, new VanishState(showsUp));
        this.variants = new CreekVariants(this.config, this.settings, this.spots, this.random, showsUp,
                (spot, state) -> new Creek(this.bodies.apply(world, spot), new PathRoute(paths), round, state),
                this::tuning);
        this.pressure.start(this.clock.getAsLong());
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
        long now = this.clock.getAsLong();
        SurvivorSnapshot survivors = SurvivorSnapshot.take(this.survivors.get(), this.dread, this.respite, now);
        if (this.pressure.step(survivors.size(), now)) this.announceSwitch();
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
        // Doppelgangers crumble or run out of time even once the creek is gone for good.
        CreekRound currentRound = this.round;
        if (currentRound != null) currentRound.decoys().tick(survivors, now);
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
        if (current != null) current.remove();
        this.consequence.cleanUp();
        this.marks.cleanUp();
        this.pressure.stop();
        CreekRound currentRound = this.round;
        this.round = null;
        if (currentRound != null) {
            currentRound.patrol().cleanUp();
            currentRound.decoys().clear();
        }
    }

    /**
     * Lets everyone in the round hear that something changed, without telling them what.
     */
    private void announceSwitch() {
        Instance world = this.instance.get();
        if (world == null) return;
        for (Player player : world.getPlayers()) {
            player.playSound(MoodClock.SWITCH_SOUND);
        }
    }

    /**
     * The values every creek of the round acts on right now.
     *
     * @return the values
     */
    CreekTuning tuning() {
        return this.pressure.tuning();
    }

    @Nullable Creek creek() {
        return this.creek;
    }

    @Nullable CreekVariants variants() {
        return this.variants;
    }

}
