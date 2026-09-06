package net.onelitefeather.cygnus.noise;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * Lets the slender hear the round slipping away from him.
 *
 * <p>He is the only player with no page counter in front of him: the survivors watch theirs go up,
 * he only ever learns the score when the round ends. This service is that counter, told as sound
 * rather than as a number - a tape hiss that sits closer together and louder the fewer pages are
 * left, the way the static in Slender: The Eight Pages tightens with every page.</p>
 *
 * <p>Two things carry it. A carpet runs for as long as the round does and closes the gap between
 * its bursts from {@link GameConfig#slenderStaticQuietInterval()} down to
 * {@link GameConfig#slenderStaticFranticInterval()} as the pages disappear. On top of it, every
 * single find lands as a burst of its own, so he knows a page went the moment it went instead of
 * on the next beat of the carpet.</p>
 *
 * <p>Everything is played with {@link Sound.Emitter#self()} on the slender's own entity, so it
 * reaches nobody else and carries no direction - a survivor must not be able to hear how close the
 * slender is to a page, and the slender must not be able to place himself by it either.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * SlenderStaticService service = new SlenderStaticService(config, () -> currentSlender);
 * service.registerListener(eventNode);
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.14.0
 */
public final class SlenderStaticService {

    /** How often the carpet is looked at, in seconds. */
    private static final long TICK_SECONDS = 1L;

    /** The pitch the static plays at while no page has been found. */
    private static final float FRESH_PITCH = 1.0F;

    /** The pitch it has dropped to once every page is gone: a tape that has been running too long. */
    private static final float WORN_PITCH = 0.7F;

    private final GameConfig config;
    private final Supplier<@Nullable Player> slender;
    private final SoundEvent sound;
    private final RepeatingTask task = new RepeatingTask(this::tick);

    /** How far the round has got, between {@code 0} and {@code 1}. */
    private float progress;

    /** Seconds left until the carpet plays again. */
    private int secondsUntilBurst;

    /**
     * Creates a new instance of the {@link SlenderStaticService}.
     *
     * @param config  the configuration holding the sound, the intervals and the volumes
     * @param slender supplies the current slender, or {@code null} while there is none
     */
    public SlenderStaticService(GameConfig config, Supplier<@Nullable Player> slender) {
        this.config = config;
        this.slender = slender;
        this.sound = SoundEvent.of(config.slenderStaticSound(), null);
        this.secondsUntilBurst = this.interval();
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
     * Starts the carpet from the top. Does nothing while the static is turned off.
     */
    public void start() {
        if (!this.config.slenderStaticEnabled()) return;
        this.reset();
        this.task.start(TICK_SECONDS, ChronoUnit.SECONDS);
    }

    /**
     * Stops the carpet and forgets how far the round had got.
     */
    public void stop() {
        this.task.stop();
        this.reset();
    }

    /**
     * @return {@code true} while the carpet is running
     */
    public boolean isRunning() {
        return this.task.isRunning();
    }

    /**
     * Answers a find with a burst and tightens the carpet behind it.
     *
     * @param event the find
     */
    void onPageFound(PageFoundEvent event) {
        if (!this.config.slenderStaticEnabled()) return;
        this.progress = shareOf(event.foundCount(), event.maxPages());
        // The burst restarts the gap so it does not land on top of the carpet's next beat, which
        // would read as one long noise rather than as two separate things happening.
        this.secondsUntilBurst = this.interval();
        this.play();
    }

    /**
     * Runs one second of the carpet.
     */
    void tick() {
        if (--this.secondsUntilBurst > 0) return;
        this.secondsUntilBurst = this.interval();
        this.play();
    }

    /**
     * Plays the static to the current slender, unless the feature is off or there is no slender to
     * play it to.
     */
    private void play() {
        if (!this.config.slenderStaticEnabled()) return;
        Player currentSlender = this.slender.get();
        if (currentSlender == null) return;
        currentSlender.playSound(
                Sound.sound(this.sound, Sound.Source.MASTER, this.volume(), this.pitch()),
                Sound.Emitter.self()
        );
    }

    /**
     * Puts the carpet back to how a round starts.
     */
    private void reset() {
        this.progress = 0.0F;
        this.secondsUntilBurst = this.interval();
    }

    /**
     * @return the seconds between two bursts at the current progress
     */
    private int interval() {
        return Math.round(lerp(
                this.config.slenderStaticQuietInterval(),
                this.config.slenderStaticFranticInterval(),
                this.progress));
    }

    /**
     * @return the volume of a burst at the current progress
     */
    private float volume() {
        return lerp(this.config.slenderStaticMinVolume(), this.config.slenderStaticMaxVolume(), this.progress);
    }

    /**
     * @return the pitch of a burst at the current progress
     */
    private float pitch() {
        return lerp(FRESH_PITCH, WORN_PITCH, this.progress);
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

    /**
     * Reads a value off the line between two ends.
     *
     * @param start the value at {@code share} 0
     * @param end   the value at {@code share} 1
     * @param share where between the two to read
     * @return the value at that point
     */
    private static float lerp(float start, float end, float share) {
        return start + (end - start) * share;
    }
}
