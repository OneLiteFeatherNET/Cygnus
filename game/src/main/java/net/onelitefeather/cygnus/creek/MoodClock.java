package net.onelitefeather.cygnus.creek;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.onelitefeather.cygnus.creek.state.CreekMood;

import java.util.List;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Decides when the creek's mood switches and which mood it switches to.
 * <p>
 * Every round starts {@link CreekMood#NORMAL}. The switch comes once, somewhere between
 * {@link #SWITCH_EARLIEST} and {@link #SWITCH_LATEST} of the game time. Every page found before it
 * keeps only {@link #PAGE_KEEPS} of the time still left, so a quick team meets the new mood
 * sooner. The mood is picked at random, and the same mood may come twice in a row.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class MoodClock implements Supplier<CreekMood> {

    /** The earliest switch, as a share of the game time. */
    static final double SWITCH_EARLIEST = 0.25D;

    /** The latest switch, as a share of the game time. */
    static final double SWITCH_LATEST = 0.5D;

    /** The share of the time left that a found page keeps. */
    static final double PAGE_KEEPS = 0.9D;

    /** What everyone hears at the switch. It does not tell the mood. */
    static final Sound SWITCH_SOUND = Sound.sound(Key.key("block.creaking_heart.spawn"), Sound.Source.HOSTILE, 1.0F, 0.5F);

    private static final List<CreekMood> MOODS = List.of(CreekMood.HUNTER, CreekMood.SHADOW, CreekMood.TRICKSTER);

    private final long gameTimeMillis;
    private final RandomGenerator random;
    private volatile boolean started;
    private volatile boolean switched;
    private volatile long switchAt;
    private volatile CreekMood mood = CreekMood.NORMAL;

    /**
     * Sets up the clock.
     *
     * @param gameTimeMillis how long a round lasts, in milliseconds
     * @param random         picks the switch time and the mood
     */
    public MoodClock(long gameTimeMillis, RandomGenerator random) {
        this.gameTimeMillis = gameTimeMillis;
        this.random = random;
    }

    /**
     * Starts a round with the normal creek and picks when it switches.
     *
     * @param now the current time in milliseconds
     */
    public void start(long now) {
        long earliest = Math.round(this.gameTimeMillis * SWITCH_EARLIEST);
        long spread = Math.round(this.gameTimeMillis * (SWITCH_LATEST - SWITCH_EARLIEST));
        this.mood = CreekMood.NORMAL;
        this.switched = false;
        this.switchAt = now + earliest + this.random.nextLong(spread + 1L);
        this.started = true;
    }

    /**
     * A page was found: the switch comes sooner, as long as it has not come yet.
     *
     * @param now the current time in milliseconds
     */
    public void pageFound(long now) {
        if (!this.started || this.switched) return;
        long left = Math.max(0L, this.switchAt - now);
        this.switchAt = now + Math.round(left * PAGE_KEEPS);
    }

    /**
     * Switches the mood once its time has come.
     *
     * @param now the current time in milliseconds
     * @return {@code true} in the step the mood switched, {@code false} otherwise
     */
    public boolean tick(long now) {
        if (!this.started || this.switched || now < this.switchAt) return false;
        this.mood = MOODS.get(this.random.nextInt(MOODS.size()));
        this.switched = true;
        return true;
    }

    /**
     * Forgets the round: the creek is normal and does not switch until the next start.
     */
    public void reset() {
        this.started = false;
        this.switched = false;
        this.mood = CreekMood.NORMAL;
    }

    /**
     * Tells the creek's mood right now.
     *
     * @return the mood
     */
    @Override
    public CreekMood get() {
        return this.mood;
    }

    long switchAt() {
        return this.switchAt;
    }
}
