package net.onelitefeather.cygnus.creek;

import java.util.function.LongSupplier;

/**
 * The time source of the creek. Tests swap it for a clock they control.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.15.0
 */
public final class RoundClock {

    private final LongSupplier clock;

    /**
     * Sets up the clock.
     *
     * @param clock supplies the current time in milliseconds
     */
    public RoundClock(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * The current time.
     *
     * @return the time in milliseconds
     */
    public long now() {
        return this.clock.getAsLong();
    }
}
