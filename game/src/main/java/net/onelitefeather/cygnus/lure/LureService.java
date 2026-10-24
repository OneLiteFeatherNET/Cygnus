package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.CreekRespite;
import net.onelitefeather.cygnus.creek.consequence.SlenderReveal;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Lures lone survivors with a dead teammate.
 * <p>
 * Every step it looks at each living survivor. One who is alone, in a round where somebody has
 * died, is rolled for now and then. On a hit a dead survivor's figure appears just outside their
 * view, looking like a living player. If they look at it long enough, it is right in front of
 * their face: the jump scare of {@link JumpScareManager}, a jump in fear, a breather from the creek
 * and, once the phantom has dissolved, a random follow-up. While a figure waits, the turnaround
 * scare is held back for that survivor, so turning towards the figure does not set it off first.
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

    /** How much farther than the figure can stand the gaze still reaches, in blocks. */
    private static final int GAZE_SLACK = 8;

    private final LureConfig config;
    private final Supplier<? extends Collection<Player>> survivors;
    private final Supplier<@Nullable Player> slender;
    private final JumpScareManager jumpScares;
    private final LureSpotFinder spots;
    private final CreekRespite respite;
    private final BiConsumer<UUID, Double> fear;
    private final SlenderReveal reveal;
    private final LongSupplier clock;
    private final RandomGenerator random;
    private final CreekSight gaze;
    private final RepeatingTask task;
    private final Map<UUID, LureState> states = new HashMap<>();

    /**
     * Sets up the service, with its step measured for the slow tick report.
     *
     * @param config     the settings
     * @param survivors  supplies the living survivors of the round
     * @param slender    supplies the slender, or {@code null} while there is none
     * @param jumpScares holds the corpses and plays the scare
     * @param spots      finds where the figure stands
     * @param respite    the breather from the creek, shared with it
     * @param fear       raises a survivor's fear by the given gain
     * @param reveal     shows a survivor to the slender for a betrayal
     * @param clock      supplies the current time in milliseconds
     * @param random     the random source
     * @param sections   measures how long each step takes
     */
    public LureService(LureConfig config, Supplier<? extends Collection<Player>> survivors,
                       Supplier<@Nullable Player> slender, JumpScareManager jumpScares, LureSpotFinder spots,
                       CreekRespite respite, BiConsumer<UUID, Double> fear, SlenderReveal reveal, LongSupplier clock,
                       RandomGenerator random, TickSections sections) {
        this.config = config;
        this.survivors = survivors;
        this.slender = slender;
        this.jumpScares = jumpScares;
        this.spots = spots;
        this.respite = respite;
        this.fear = fear;
        this.reveal = reveal;
        this.clock = clock;
        this.random = random;
        this.gaze = new CreekSight(config.maxDistance() + GAZE_SLACK, config.gazeAngle());
        this.task = new RepeatingTask(sections.wrap(TickSectionNames.LURE, this::tick));
    }

    /**
     * Starts stepping the lures.
     */
    public void start() {
        this.task.start(STEP_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Stops stepping, takes away every figure and forgets every lure and breather, so the next round
     * starts clean.
     */
    public synchronized void stop() {
        this.task.stop();
        this.states.forEach((id, state) -> this.endWait(id, state));
        this.states.clear();
        this.respite.clear();
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
                    if (state.followUpDue(now)) this.followUp(survivor);
                }
            }
        }
    }

    private void roll(Player survivor, LureState state, List<Player> living, long now) {
        if (!this.alone(survivor, living)) return;
        if (!state.rollDue(now, this.config.rollSeconds() * 1000L)) return;
        if (this.random.nextDouble() >= this.config.rollChance()) return;
        Optional<DeadPlayerMannequin> corpse = this.jumpScares.randomCorpse(this.random);
        if (corpse.isEmpty()) return;
        Optional<Pos> spot = this.spots.find(survivor.getPosition(), this.random);
        if (spot.isEmpty()) return;
        state.waiting(LureFigure.show(survivor, corpse.get(), spot.get()), now + this.config.waitSeconds() * 1000L);
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
        if (looked < this.config.gazeMillis()) return;
        // Pressed against a wall there is no room in front of the face. The figure keeps waiting.
        if (!this.jumpScares.scare(survivor, figure.identity(), figure.skin())) return;
        figure.remove();
        this.jumpScares.resumeTurnaround(survivor.getUuid());
        this.fear.accept(survivor.getUuid(), this.config.fearGain());
        this.respite.spare(survivor.getUuid(), now + this.config.respiteSeconds() * 1000L);
        state.used(now + FOLLOW_UP_MILLIS);
    }

    private void followUp(Player survivor) {
        int ticks = this.config.effectSeconds() * 20;
        switch (LureOutcome.roll(this.config, this.random.nextDouble())) {
            case SLOWNESS -> survivor.addEffect(new Potion(PotionEffect.SLOWNESS, (byte) 1, ticks));
            case BLINDNESS -> survivor.addEffect(new Potion(PotionEffect.BLINDNESS, (byte) 0, ticks));
            case BETRAYAL -> {
                Player hunter = this.slender.get();
                if (hunter != null) this.reveal.reveal(survivor, hunter);
            }
            case NOTHING -> {
                // The scare was all.
            }
        }
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
