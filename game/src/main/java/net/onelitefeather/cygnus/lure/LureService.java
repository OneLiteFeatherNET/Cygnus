package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.RepeatingTask;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Lures lone survivors with a dead teammate.
 * <p>
 * Every step it looks at each living survivor. One who is alone, in a round where somebody has
 * died, is rolled for now and then. On a hit a dead survivor's figure appears just outside their
 * view, looking like a living player. If they look at it long enough, it is right in front of
 * their face: the jump scare of {@link JumpScareManager}, followed by the {@link LureConsequence}.
 * While a figure waits, the turnaround scare is held back for that survivor, so turning towards the
 * figure does not set it off first.
 * </p>
 * <p>
 * The steps run on the scheduler thread, the end of a round may come from elsewhere, so the two
 * are synchronized.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class LureService {

    /** How often the lure steps, in milliseconds. Fine enough for the gaze to feel immediate. */
    static final long STEP_MILLIS = 100L;

    /** How long the follow-up waits for the phantom to dissolve: the jump scare's 50 ticks. */
    static final long FOLLOW_UP_MILLIS = 2500L;

    /** How long the figure waits to be noticed before it is gone, in milliseconds. */
    static final long WAIT_MILLIS = 20_000L;

    /** How long the survivor has to look at the figure before it strikes, in milliseconds. */
    static final long GAZE_MILLIS = 700L;

    /** How far off the view centre the figure still counts as looked at, in degrees. */
    private static final int GAZE_ANGLE = 35;

    /** How much farther than the figure can stand the gaze still reaches, in blocks. */
    private static final int GAZE_SLACK = 8;

    private final LureConfig config;
    private final Supplier<? extends Collection<Player>> survivors;
    private final JumpScareManager jumpScares;
    private final LureSpotFinder spots;
    private final LureConsequence consequence;
    private final LongSupplier clock;
    private final RandomGenerator random;
    private final CreekSight gaze;
    private final RepeatingTask task;
    private final Map<UUID, LureState> states = new HashMap<>();
    private boolean stopped;

    /**
     * Sets up the service, with its step measured for the slow tick report.
     *
     * @param config      the settings
     * @param survivors   supplies the living survivors of the round
     * @param jumpScares  holds the corpses and plays the scare
     * @param spots       finds where the figure stands
     * @param consequence what the figure does to the survivor once it has struck
     * @param clock       supplies the current time in milliseconds
     * @param random      the random source
     * @param sections    measures how long each step takes
     */
    public LureService(LureConfig config, Supplier<? extends Collection<Player>> survivors,
                       JumpScareManager jumpScares, LureSpotFinder spots, LureConsequence consequence,
                       LongSupplier clock, RandomGenerator random, TickSections sections) {
        this.config = config;
        this.survivors = survivors;
        this.jumpScares = jumpScares;
        this.spots = spots;
        this.consequence = consequence;
        this.clock = clock;
        this.random = random;
        this.gaze = new CreekSight((int) LureSpotFinder.MAX_DISTANCE + GAZE_SLACK, GAZE_ANGLE);
        this.task = new RepeatingTask(sections.wrap(TickSectionNames.LURE, this::tick));
    }

    /**
     * Starts stepping the lures.
     */
    public synchronized void start() {
        this.stopped = false;
        this.task.start(STEP_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Stops stepping, takes away every figure, forgets every lure and cleans up the consequence, so
     * the next round starts clean.
     */
    public synchronized void stop() {
        // A step already waiting for the lock would otherwise run after the stop and lure again.
        this.stopped = true;
        this.task.stop();
        this.states.forEach(this::endWait);
        this.states.clear();
        this.consequence.cleanUp();
    }

    /**
     * Tells whether the lures are being stepped.
     *
     * @return {@code true} between start and stop
     */
    boolean isRunning() {
        return this.task.isRunning();
    }

    /**
     * Tells which figure waits for a survivor.
     *
     * @param survivor the survivor
     * @return the figure, or empty if none waits for them
     */
    synchronized Optional<LureFigure> figureOf(UUID survivor) {
        LureState state = this.states.get(survivor);
        return state == null ? Optional.empty() : Optional.ofNullable(state.figure());
    }

    /**
     * Runs one step for every living survivor.
     */
    synchronized void tick() {
        if (this.stopped) return;
        long now = this.clock.getAsLong();
        List<Player> living = List.copyOf(this.survivors.get());
        Set<UUID> present = new HashSet<>();
        for (Player survivor : living) {
            present.add(survivor.getUuid());
        }
        // Whoever left the round takes their figure along.
        this.states.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) return false;
            this.endWait(entry.getKey(), entry.getValue());
            return true;
        });

        for (Player survivor : living) {
            LureState state = this.states.computeIfAbsent(survivor.getUuid(), _ -> new LureState());
            switch (state.phase()) {
                case IDLE -> this.roll(survivor, state, living, now);
                case WAITING -> this.wait(survivor, state, living, now);
                case USED -> {
                    if (state.followUpDue(now)) this.consequence.followUp(survivor);
                }
            }
        }
    }

    private void roll(Player survivor, LureState state, List<Player> living, long now) {
        if (!this.alone(survivor, living)) return;
        // Never right after the turnaround scare, the two should not hit back to back.
        if (this.jumpScares.isCoolingDown(survivor.getUuid())) return;
        if (!state.rollDue(now)) return;
        if (this.random.nextDouble() >= this.config.rollChance()) return;
        Optional<DeadPlayerMannequin> corpse = this.jumpScares.randomCorpse(this.random);
        if (corpse.isEmpty()) return;
        Optional<Pos> spot = this.spots.find(survivor.getPosition(), this.random);
        if (spot.isEmpty()) return;
        state.waiting(LureFigure.show(survivor, corpse.get(), spot.get()), now + WAIT_MILLIS);
        this.jumpScares.pauseTurnaround(survivor.getUuid());
    }

    private void wait(Player survivor, LureState state, List<Player> living, long now) {
        LureFigure figure = state.figure();
        if (figure == null) return;
        if (!this.alone(survivor, living) || state.waitedOut(now)) {
            this.endWait(survivor.getUuid(), state);
            return;
        }
        long looked = state.gaze(this.looksAt(survivor, figure), now);
        if (looked < GAZE_MILLIS) return;
        // Pressed against a wall there is no room in front of the face. The figure keeps waiting.
        if (!this.jumpScares.scare(survivor, figure.identity(), figure.skin())) return;
        figure.remove();
        this.jumpScares.resumeTurnaround(survivor.getUuid());
        this.consequence.struck(survivor, now);
        state.used(now + FOLLOW_UP_MILLIS);
    }

    /**
     * Takes a waiting figure away and lets the turnaround scare reach the survivor again.
     */
    private void endWait(UUID survivor, LureState state) {
        if (state.phase() != LureState.Phase.WAITING) return;
        LureFigure figure = state.figure();
        if (figure != null) figure.remove();
        this.jumpScares.resumeTurnaround(survivor);
        state.idle();
    }

    private boolean alone(Player survivor, List<Player> living) {
        Instance world = survivor.getInstance();
        for (Player other : living) {
            if (other == survivor || other.getInstance() != world) continue;
            if (other.getPosition().distance(survivor.getPosition()) <= this.config.aloneRadius()) return false;
        }
        return true;
    }

    private boolean looksAt(Player survivor, LureFigure figure) {
        Instance world = survivor.getInstance();
        if (world == null) return false;
        Pos eyes = survivor.getPosition().add(0, SurvivorView.EYE_HEIGHT, 0);
        Pos head = figure.position().add(0, SurvivorView.EYE_HEIGHT, 0);
        return this.gaze.inView(eyes, head) && LureSpotFinder.clearLine(world, eyes, head);
    }
}
