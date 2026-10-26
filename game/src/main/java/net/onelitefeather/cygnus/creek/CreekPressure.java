package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.state.Calm;
import net.onelitefeather.cygnus.creek.state.CreekMood;
import net.onelitefeather.cygnus.creek.state.CreekTuning;

/**
 * How hard the creek presses right now: the round's calm and the creek's mood, turned into the
 * values every creek of the round acts on.
 * <p>
 * The values only change when a survivor dies or the mood switches, a few times a round. Every
 * other step gets the very same {@link CreekTuning}.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
final class CreekPressure {

    private final CreekConfig config;
    private final MoodClock mood;
    /** The most survivors the round has had so far, which is what its calm is measured against. */
    private int started;
    private int alive;
    private volatile CreekTuning tuning;

    /**
     * Sets up the pressure.
     *
     * @param config the settings
     * @param mood   decides when the creek's mood switches and to which
     */
    CreekPressure(CreekConfig config, MoodClock mood) {
        this.config = config;
        this.mood = mood;
        this.forget();
    }

    /**
     * Starts a round: full, calmless and in the normal mood, with a fresh switch time.
     *
     * @param now the current time in milliseconds
     */
    void start(long now) {
        this.mood.start(now);
        this.forget();
    }

    /**
     * Takes one step: switches the mood once its time has come and follows the survivors who are
     * still alive.
     *
     * @param alive how many survivors are alive in this step
     * @param now   the current time in milliseconds
     * @return {@code true} in the step the mood switched
     */
    boolean step(int alive, long now) {
        boolean switched = this.mood.tick(now);
        // Measured against the fullest the round has been: survivors may still be joining in its first steps.
        int started = Math.max(this.started, alive);
        if (switched || started != this.started || alive != this.alive) {
            this.started = started;
            this.alive = alive;
            this.tuning = new CreekTuning(this.config, Calm.of(started, alive), this.mood.get());
        }
        return switched;
    }

    /**
     * A page was found, which pulls the mood switch forward.
     *
     * @param now the current time in milliseconds
     */
    void pageFound(long now) {
        this.mood.pageFound(now);
    }

    /**
     * Forgets the round: normal mood and no calm.
     */
    void stop() {
        this.mood.reset();
        this.forget();
    }

    /**
     * The values every creek of the round acts on right now.
     *
     * @return the values
     */
    CreekTuning tuning() {
        return this.tuning;
    }

    private void forget() {
        this.started = 0;
        this.alive = 0;
        this.tuning = new CreekTuning(this.config, Calm.NONE, CreekMood.NORMAL);
    }
}
