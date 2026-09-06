package net.onelitefeather.cygnus.glitch;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.gaze.GazeSink;
import net.onelitefeather.cygnus.gaze.SlenderGaze;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * Tears the slender's own screen apart as the survivors take his pages.
 *
 * <p>He is the only player in a round without a page counter: the survivors watch theirs climb, he
 * only learns the score when it ends. This is that counter, told as a picture coming apart rather
 * than as a number - the same worn-tape look the survivors get from seeing him, turned back on him
 * and driven by the round instead of by a line of sight.</p>
 *
 * <p>Two things carry it. A baseline sits on his screen for the rest of the round and climbs a
 * level at a time as the pages disappear, and every single find pulses one level above that for
 * {@link GameConfig#pageGlitchPulseSeconds()} before falling back. Until the first page goes there
 * is nothing at all: {@link SlenderGaze#NONE} rather than the weakest level, because an untouched
 * round has nothing to say yet.</p>
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
 * service.registerListener(eventNode);
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PageGlitchService {

    /** How often the service looks at the round, in seconds. */
    private static final long TICK_SECONDS = 1L;

    private final GameConfig config;
    private final GazeSink sink;
    private final Supplier<@Nullable Player> slender;
    private final RepeatingTask task = new RepeatingTask(this::tick);

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
     * @param slender supplies the current slender, or {@code null} while there is none
     */
    public PageGlitchService(GameConfig config, GazeSink sink, Supplier<@Nullable Player> slender) {
        this.config = config;
        this.sink = sink;
        this.slender = slender;
    }

    /**
     * Hooks the service into the round's lifecycle.
     *
     * @param node the node to register on
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(GameStartEvent.class, event -> this.start());
        node.addListener(PageFoundEvent.class, this::onPageFound);
        node.addListener(GameFinishEvent.class, event -> this.stop());
    }

    /**
     * Starts a round with an untouched screen. Does nothing while the glitch is turned off.
     */
    public void start() {
        if (!this.config.pageGlitchEnabled()) return;
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
        if (!this.config.pageGlitchEnabled()) return;
        this.progress = shareOf(event.foundCount(), event.maxPages());
        this.pulseSecondsLeft = this.config.pageGlitchPulseSeconds();
        // apply() rather than tick(): a find is not a second passing, and running the clock here
        // would cut the pulse a second short of what it was configured for.
        this.apply();
    }

    /**
     * Runs one second of the round: lets a running pulse decay, then draws.
     */
    void tick() {
        if (!this.config.pageGlitchEnabled()) return;
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
        if (!this.config.pageGlitchEnabled()) return;

        Player currentSlender = this.slender.get();
        if (currentSlender == null) {
            this.release();
            return;
        }

        if (!currentSlender.equals(this.attached)) {
            // A hand-over, from SlenderReviveEvent. The old slender is a survivor's problem now and
            // must not keep a torn screen; the round's state follows whoever took his place.
            this.release();
            // Without the world tint: the veil tells him how far the round has got, while darkening
            // the world on top of it would take the sight he hunts with.
            this.sink.attach(currentSlender, false);
            this.attached = currentSlender;
        }

        int level = this.level();
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
     * Works out the level his screen sits at right now.
     *
     * @return a level between {@code 0} and {@link SlenderGaze#LEVELS} minus one, or
     *         {@link SlenderGaze#NONE} while no page has been found
     */
    private int level() {
        if (this.progress <= 0.0F) {
            // A pulse cannot fire before a find, so there is no baseline to raise here.
            return SlenderGaze.NONE;
        }

        int baseline = Math.round(this.progress * (SlenderGaze.LEVELS - 1));
        if (this.pulseSecondsLeft <= 0) return baseline;
        return Math.min(baseline + 1, SlenderGaze.LEVELS - 1);
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
