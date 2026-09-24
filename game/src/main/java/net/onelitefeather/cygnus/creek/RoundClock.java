package net.onelitefeather.cygnus.creek;

import java.util.function.LongSupplier;

/**
 * Tracks how long the current round has been running.
 * <p>
 * The service starts and resets it, and the dread rating reads it. Sharing one clock avoids a
 * dependency between the two.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class RoundClock {

    private final LongSupplier clock;
    private volatile long startedAt = -1L;

    /**
     * Creates the clock.
     *
     * @param clock supplies the current time in milliseconds
     */
    public RoundClock(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Returns the current time.
     *
     * @return the time in milliseconds
     */
    public long now() {
        return this.clock.getAsLong();
    }

    /**
     * Marks the start of a round.
     */
    public void start() {
        this.startedAt = this.clock.getAsLong();
    }

    /**
     * Resets the clock when the round ends.
     */
    public void reset() {
        this.startedAt = -1L;
    }

    /**
     * Returns how long the round has been running.
     *
     * @return the elapsed time in milliseconds, {@code 0} outside a round
     */
    public long elapsedMillis() {
        long start = this.startedAt;
        return start < 0 ? 0L : this.clock.getAsLong() - start;
    }
}
