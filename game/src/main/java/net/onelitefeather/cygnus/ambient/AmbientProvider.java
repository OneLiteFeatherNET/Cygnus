package net.onelitefeather.cygnus.ambient;

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

import static net.onelitefeather.cygnus.common.util.Helper.getRandomPitchValue;

/**
 * Provides ambient effects for a {@link Team} during a game phase.
 *
 * <p>The provider runs a repeating task that plays cave sounds every
 * {@value #AMBIENT_SOUND_INTERVAL_SECONDS} seconds and applies a blindness effect with additional
 * sounds, simulating a "lights out" scenario. The gap between two blackout events is randomized
 * between {@value #MIN_BLACKOUT_INTERVAL_SECONDS} and {@value #MAX_BLACKOUT_INTERVAL_SECONDS}
 * seconds, re-rolled after every blackout, so the event stays unpredictable instead of landing on
 * a fixed rhythm players learn to expect.</p>
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
 * @version 2.2.0
 * @since 1.0.0
 */
public final class AmbientProvider {

    private static final int AMBIENT_SOUND_INTERVAL_SECONDS = 15;
    private static final int MIN_BLACKOUT_INTERVAL_SECONDS = 100;
    private static final int MAX_BLACKOUT_INTERVAL_SECONDS = 220;

    private static final TimedPotion POTION_EFFECT =
            new TimedPotion(new Potion(PotionEffect.BLINDNESS, (byte) 1, 200), 200);
    private static final Sound[] SOUNDS = {
            Sound.sound(SoundEvent.AMBIENT_CAVE, Sound.Source.MASTER, 1F, 1F),
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.MASTER, 1F, 0F),
            Sound.sound(SoundEvent.BLOCK_FIRE_EXTINGUISH, Sound.Source.MASTER, 1F, 1F),
            Sound.sound(SoundEvent.ENTITY_WITHER_SPAWN, Sound.Source.MASTER, 5F, 0.7F)
    };

    private final Team team;
    private final RepeatingTask task = new RepeatingTask(this::tick);
    private final RandomGenerator random;
    private int currentTicks;
    private int ticksSinceLastBlackout;
    private int nextBlackoutIn;

    /**
     * Creates a new instance of the {@link AmbientProvider}.
     * @param team which is involved
     */
    public AmbientProvider(Team team) {
        this(team, ThreadLocalRandom.current());
    }

    /**
     * Creates a new instance with an injectable random source, so tests can force a specific
     * blackout interval instead of depending on {@link ThreadLocalRandom}.
     *
     * @param team   which is involved
     * @param random the source used to roll the gap between blackout events
     */
    AmbientProvider(Team team, RandomGenerator random) {
        this.team = team;
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
     * Executes the ambient logic for one second of game time.
     *
     * <p>Plays a cave sound every {@value #AMBIENT_SOUND_INTERVAL_SECONDS} seconds, and separately
     * counts down to the next blackout; once the rolled interval elapses the blackout fires and a
     * new interval is rolled for the following one.</p>
     */
    public void tick() {
        List<Player> players = List.copyOf(this.team.getPlayers());

        if (currentTicks % AMBIENT_SOUND_INTERVAL_SECONDS == 0) {
            playCaveSound(players);
        }

        ticksSinceLastBlackout++;
        if (ticksSinceLastBlackout >= nextBlackoutIn) {
            triggerBlackout(players);
            ticksSinceLastBlackout = 0;
            nextBlackoutIn = randomBlackoutInterval();
        }

        currentTicks++;
    }

    /**
     * Plays a single cave ambience sound, with a randomized pitch, to every given player.
     *
     * @param players the players to play the sound to
     */
    private void playCaveSound(List<Player> players) {
        Sound caveSound = Sound.sound(SoundEvent.AMBIENT_CAVE, Sound.Source.MASTER, 1F, getRandomPitchValue());
        for (Player player : players) {
            player.playSound(caveSound, player.getPosition());
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
            player.playSound(SOUNDS[1], player.getPosition());
            player.playSound(SOUNDS[2], player.getPosition());
            player.playSound(SOUNDS[3], player.getPosition());
            player.sendMessage(Messages.LIGHT_WENT_OUT);
        }
    }
}
