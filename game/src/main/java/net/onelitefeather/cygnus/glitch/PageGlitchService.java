package net.onelitefeather.cygnus.glitch;

import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.gaze.GazeSink;
import net.onelitefeather.cygnus.gaze.SlenderGaze;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Tears the slender's own screen apart as the survivors take his pages.
 *
 * <p>He is the only player in a round without a page counter: the survivors watch theirs climb, he
 * only learns the score when it ends. This is that counter, told as a picture coming apart rather
 * than as a number - the same worn-tape look the survivors get from seeing him, turned back on him
 * and driven by the round instead of by a line of sight.</p>
 *
 * <p>Two things carry it. A baseline sits on his screen for the rest of the round. It eases in:
 * it stays quiet through the first half of the pages and only climbs steeply towards the end, so
 * the worst level is saved for the last page instead of being reached with a third of the round
 * still to go. Every single find pulses one level above that for
 * {@link GameConfig.PageGlitch#pulseSeconds()} before falling back. Until the first page goes there
 * is nothing at all: {@link SlenderGaze#NONE} rather than the weakest level, because an untouched
 * round has nothing to say yet.</p>
 *
 * <p>Both are capped at {@link GameConfig.PageGlitch#maxLevel()}. Playtests showed the higher levels
 * left the slender blind, so the default cap is the weakest level: from the first page on he sees
 * that level and the pulse has no visible step above it.</p>
 *
 * <p>The glitch is only ever on his screen while he is invisible. While he is visible and hunting
 * his screen is completely clean ({@link SlenderGaze#NONE}). The round's progress and pulse keep
 * running underneath, so the level is right the moment he turns invisible again. A toggle arrives as
 * a {@link StaminaStateChangeEvent} and is applied at once rather than on the next second.</p>
 *
 * <p>The level travels through a {@link GazeSink}, the same seam {@code SlenderGazeService} uses,
 * so what reaches the client is decided in one place. The sink is deliberately shared with the
 * gaze rather than a second one of its own: a player has one signal carrier, and two would draw
 * two full-screen quads over each other. Slender and survivors are disjoint, so the two services
 * never address the same player - except across a hand-over, which is why {@link #tick()} watches
 * for the slender changing and moves the effect over.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * PageGlitchService service = new PageGlitchService(config, signal, () -> currentSlender);
 * eventNode.addChild(service.node());
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.2.0
 * @since 2.15.0
 */
public final class PageGlitchService implements GameFeature {

    /** How often the service looks at the round, in seconds. */
    private static final long TICK_SECONDS = 1L;

    private final EventNode<Event> node = EventNode.all("page-glitch");

    private final GameConfig.PageGlitch config;
    private final GazeSink sink;
    private final Supplier<@Nullable Player> slender;
    private final Predicate<Player> invisible;
    private final RepeatingTask task;

    /** How far the round has got, between {@code 0} and {@code 1}. */
    private float progress;

    /** Seconds left of the pulse a find started, {@code 0} while none is running. */
    private int pulseSecondsLeft;

    /** The slender the effect currently sits on, {@code null} while it sits on nobody. */
    private @Nullable Player attached;

    /** The level last reported to the sink, so an unchanged one is never sent twice. */
    private int reported = SlenderGaze.NONE;

    /**
     * Creates a new instance of the {@link PageGlitchService}.
     *
     * @param config  the configuration holding the switch and the pulse
     * @param sink    where the level is signalled to
     * @param slender supplies the current slender, or {@code null} while there is none; he counts as
     *                invisible
     */
    PageGlitchService(GameConfig.PageGlitch config, GazeSink sink, Supplier<@Nullable Player> slender) {
        this(config, sink, slender, _ -> true);
    }

    /**
     * Creates a new instance of the {@link PageGlitchService}.
     *
     * @param config    the configuration holding the switch, the pulse and the cap
     * @param sink      where the level is signalled to
     * @param slender   supplies the current slender, or {@code null} while there is none
     * @param invisible tells whether the given slender is invisible right now; the glitch is only
     *                  shown while it is
     * @since 2.14.1
     */
    public PageGlitchService(GameConfig.PageGlitch config, GazeSink sink, Supplier<@Nullable Player> slender,
                             Predicate<Player> invisible) {
        this(config, sink, slender, invisible, TickSections.NONE);
    }

    /**
     * Creates a new instance of the {@link PageGlitchService} with its tick measured for the slow
     * tick report.
     *
     * @param config    the configuration holding the switch, the pulse and the cap
     * @param sink      where the level is signalled to
     * @param slender   supplies the current slender, or {@code null} while there is none
     * @param invisible tells whether the given slender is invisible right now
     * @param sections  measures how long each run takes
     * @since 2.15.0
     */
    public PageGlitchService(GameConfig.PageGlitch config, GazeSink sink, Supplier<@Nullable Player> slender,
                             Predicate<Player> invisible, TickSections sections) {
        this.task = new RepeatingTask(sections.wrap(TickSectionNames.PAGE_GLITCH, this::tick));
        this.config = config;
        this.sink = sink;
        this.slender = slender;
        this.invisible = invisible;
        this.registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Hooks the service into the round's lifecycle.
     */
    private void registerListeners() {
        this.node.addListener(GameStartEvent.class, event -> this.start());
        this.node.addListener(PageFoundEvent.class, this::onPageFound);
        // The tag the predicate reads is flipped before this event is fired, so the new state is
        // already visible here and the glitch moves the moment he toggles. Outside a round there is
        // nothing to show, and applying would attach the carrier to a slender after the round ended.
        this.node.addListener(StaminaStateChangeEvent.class, event -> {
            if (this.isRunning()) this.apply();
        });
        this.node.addListener(GameFinishEvent.class, event -> this.stop());
    }

    /**
     * Starts a round with an untouched screen. Does nothing while the glitch is turned off.
     */
    public void start() {
        if (!this.config.enabled()) return;
        this.progress = 0.0F;
        this.pulseSecondsLeft = 0;
        this.task.start(TICK_SECONDS, ChronoUnit.SECONDS);
        this.apply();
    }

    /**
     * Stops the effect, takes it off whoever is wearing it and forgets the round.
     */
    public void stop() {
        this.task.stop();
        this.release();
        this.progress = 0.0F;
        this.pulseSecondsLeft = 0;
    }

    /**
     * @return {@code true} while the service is watching a round
     */
    public boolean isRunning() {
        return this.task.isRunning();
    }

    /**
     * Answers a find with a pulse and raises the baseline behind it.
     *
     * @param event the find
     */
    void onPageFound(PageFoundEvent event) {
        if (!this.config.enabled()) return;
        this.progress = shareOf(event.foundCount(), event.maxPages());
        this.pulseSecondsLeft = this.config.pulseSeconds();
        // apply() rather than tick(): a find is not a second passing, and running the clock here
        // would cut the pulse a second short of what it was configured for.
        this.apply();
    }

    /**
     * Runs one second of the round: lets a running pulse decay, then draws.
     */
    void tick() {
        if (!this.config.enabled()) return;
        if (this.pulseSecondsLeft > 0) {
            this.pulseSecondsLeft--;
        }
        this.apply();
    }

    /**
     * Puts the current level on the current slender's screen, moving the effect across if the
     * slender has changed.
     */
    private void apply() {
        if (!this.config.enabled()) return;

        Player currentSlender = this.slender.get();
        if (currentSlender == null) {
            this.release();
            return;
        }

        if (!currentSlender.equals(this.attached)) {
            // A hand-over, from SlenderReviveEvent. The old slender is a survivor's problem now and
            // must not keep a torn screen; the round's state follows whoever took his place.
            this.release();
            // Without the colour shift: the glitch alone tells him how far the round has got, while
            // the purple wash and a darkened world on top of it would take the sight he hunts with.
            this.sink.attach(currentSlender, false);
            this.attached = currentSlender;
        }

        int level = this.invisible.test(currentSlender) ? this.level() : SlenderGaze.NONE;
        if (level == this.reported) return;
        this.reported = level;
        this.sink.level(currentSlender, level);
    }

    /**
     * Takes the effect off whoever is wearing it, if anybody is.
     */
    private void release() {
        if (this.attached == null) return;
        this.sink.detach(this.attached);
        this.attached = null;
        this.reported = SlenderGaze.NONE;
    }

    /**
     * Works out the level his screen sits at right now. The baseline follows {@link #baselineOf}, so
     * it starts slowly and a pulse on top of it is what lets a find stand out early in the round.
     *
     * <p>Baseline and pulse are both clamped to the configured cap.</p>
     *
     * @return a level between {@code 0} and the configured cap, or {@link SlenderGaze#NONE} while no
     *         page has been found
     */
    private int level() {
        if (this.progress <= 0.0F) {
            // A pulse cannot fire before a find, so there is no baseline to raise here.
            return SlenderGaze.NONE;
        }

        int cap = Math.min(this.config.maxLevel(), SlenderGaze.LEVELS - 1);
        int baseline = Math.min(baselineOf(this.progress), cap);
        if (this.pulseSecondsLeft <= 0) return baseline;
        return Math.min(baseline + 1, cap);
    }

    /**
     * Works out the level the round has settled at once the pulse is gone.
     *
     * <p>The progress is squared so the curve eases in. A straight line (or plain rounding) put the
     * baseline at the second level from half the pages of a small round, and every pulse after that
     * reached the worst level, long before the end. Squared and rounded down, an eight page round
     * holds the weakest level up to the fourth page and only reaches the worst one with the last.</p>
     *
     * @param progress how far the round has got, above {@code 0} and at most {@code 1}
     * @return a level between {@code 0} and {@link SlenderGaze#LEVELS} minus one
     */
    static int baselineOf(float progress) {
        return (int) Math.floor(progress * progress * (SlenderGaze.LEVELS - 1));
    }

    /**
     * Works out how far along a round is.
     *
     * @param foundCount how many pages are gone
     * @param maxPages   how many the round needs, {@code 0} or less counting as done
     * @return the share, between {@code 0} and {@code 1}
     */
    private static float shareOf(int foundCount, int maxPages) {
        if (maxPages <= 0) return 1.0F;
        return Math.clamp((float) foundCount / maxPages, 0.0F, 1.0F);
    }
}
