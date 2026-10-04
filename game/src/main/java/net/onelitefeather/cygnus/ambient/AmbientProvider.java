package net.onelitefeather.cygnus.ambient;

import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.theevilreaper.xerus.api.team.Team;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.utils.RepeatingTask;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Provides the global "lights out" blackout for a {@link Team} during a game phase.
 *
 * <p>The provider runs a repeating task that applies a blindness effect with a burst of sounds to
 * the whole team at once. The gap between two blackout events is randomized
 * between {@value #MIN_BLACKOUT_INTERVAL_SECONDS} and {@value #MAX_BLACKOUT_INTERVAL_SECONDS}
 * seconds, re-rolled after every blackout, so the event stays unpredictable instead of landing on
 * a fixed rhythm players learn to expect.</p>
 *
 * <p>The cave sounds are no longer played from here: they are paced per survivor by their fear,
 * see {@code CygnusPlayer#tickAmbient(double)}.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * AmbientProvider provider = new AmbientProvider();
 * provider.setTeam(team);
 * provider.startTask();
 * // ...
 * provider.stopTask();
 * }</pre>
 *
 * @author theEvilReaper
 * @version 3.1.0
 * @since 1.0.0
 */
public final class AmbientProvider {

    private static final int MIN_BLACKOUT_INTERVAL_SECONDS = 100;
    private static final int MAX_BLACKOUT_INTERVAL_SECONDS = 220;

    private static final int BLACKOUT_DURATION_TICKS = 200;

    private static final TimedPotion POTION_EFFECT =
            new TimedPotion(new Potion(PotionEffect.BLINDNESS, (byte) 1, BLACKOUT_DURATION_TICKS), BLACKOUT_DURATION_TICKS);
    private static final Sound[] BLACKOUT_SOUNDS = {
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.MASTER, 1F, 0F),
            Sound.sound(SoundEvent.BLOCK_FIRE_EXTINGUISH, Sound.Source.MASTER, 1F, 1F),
            Sound.sound(SoundEvent.ENTITY_WITHER_SPAWN, Sound.Source.MASTER, 5F, 0.7F)
    };

    private final Team team;
    private final BlackoutObserver observer;
    private final RepeatingTask task;
    private final RandomGenerator random;
    private int ticksSinceLastBlackout;
    private int nextBlackoutIn;

    /**
     * Creates a new instance of the {@link AmbientProvider}.
     * @param team which is involved
     */
    public AmbientProvider(Team team) {
        this(team, ThreadLocalRandom.current(), BlackoutObserver.NONE);
    }

    /**
     * Creates the provider with an observer for its blackouts.
     *
     * @param team     the team the lights go out for
     * @param observer hears about every blackout
     * @since 2.15.0
     */
    public AmbientProvider(Team team, BlackoutObserver observer) {
        this(team, ThreadLocalRandom.current(), observer);
    }

    /**
     * Creates the provider with its tick measured for the slow tick report.
     *
     * @param team     the team the lights go out for
     * @param observer hears about every blackout
     * @param sections measures how long each run takes
     * @since 2.15.0
     */
    public AmbientProvider(Team team, BlackoutObserver observer, TickSections sections) {
        this(team, ThreadLocalRandom.current(), observer, sections);
    }

    /**
     * Creates a new instance with an injectable random source, so tests can force a specific
     * blackout interval instead of depending on {@link ThreadLocalRandom}.
     *
     * @param team   which is involved
     * @param random the source used to roll the gap between blackout events
     */
    AmbientProvider(Team team, RandomGenerator random) {
        this(team, random, BlackoutObserver.NONE);
    }

    AmbientProvider(Team team, RandomGenerator random, BlackoutObserver observer) {
        this(team, random, observer, TickSections.NONE);
    }

    AmbientProvider(Team team, RandomGenerator random, BlackoutObserver observer, TickSections sections) {
        this.task = new RepeatingTask(sections.wrap(TickSectionNames.AMBIENT, this::tick));
        this.team = team;
        this.observer = observer;
        this.random = random;
        this.nextBlackoutIn = randomBlackoutInterval();
    }

    /**
     * Rolls a new gap, in seconds, until the next blackout event.
     *
     * @return a value between {@value #MIN_BLACKOUT_INTERVAL_SECONDS} and
     *         {@value #MAX_BLACKOUT_INTERVAL_SECONDS}, inclusive
     */
    private int randomBlackoutInterval() {
        return MIN_BLACKOUT_INTERVAL_SECONDS
                + random.nextInt(MAX_BLACKOUT_INTERVAL_SECONDS - MIN_BLACKOUT_INTERVAL_SECONDS + 1);
    }

    /**
     * Starts the ambient task. Does nothing if it is already running.
     */
    public void startTask() {
        this.task.start(1, ChronoUnit.SECONDS);
    }

    /**
     * Stops the ambient task. Does nothing if it is not running.
     */
    public void stopTask() {
        this.task.stop();
    }

    /**
     * Executes the blackout logic for one second of game time.
     *
     * <p>Counts down to the next blackout; once the rolled interval elapses the blackout fires and a
     * new interval is rolled for the following one.</p>
     */
    public void tick() {
        ticksSinceLastBlackout++;
        if (ticksSinceLastBlackout >= nextBlackoutIn) {
            List<Player> hit = List.copyOf(this.team.getPlayers());
            triggerBlackout(hit);
            ticksSinceLastBlackout = 0;
            nextBlackoutIn = randomBlackoutInterval();
            this.observer.blackout(hit, BLACKOUT_DURATION_TICKS, nextBlackoutIn);
        }
    }

    /**
     * Applies the "lights out" blackout to every given player: a short blindness effect, a burst
     * of sounds, and the {@link Messages#LIGHT_WENT_OUT} message.
     *
     * @param players the players affected by the blackout
     */
    private void triggerBlackout(List<Player> players) {
        for (Player player : players) {
            player.addEffect(POTION_EFFECT.potion());
            for (Sound sound : BLACKOUT_SOUNDS) {
                player.playSound(sound, player.getPosition());
            }
            player.sendMessage(Messages.LIGHT_WENT_OUT);
        }
    }
}
