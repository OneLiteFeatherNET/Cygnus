package net.onelitefeather.cygnus.sanity;

/**
 * One survivor's fear.
 * <p>
 * The value wears off on its own. Instead of a task lowering it every tick, each read or write
 * first takes off what wore off since the last one. The creek asks for it every step anyway.
 * </p>
 * <p>
 * The creek reads and writes from the scheduler thread while deaths and pages arrive from others,
 * so every method is synchronized.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
final class Fear {

    private static final long NEVER = Long.MIN_VALUE;

    private final double decayPerSecond;
    private double value;
    private long updatedAt;
    private long lastSightingAt;

    /**
     * Creates a calm survivor's fear.
     *
     * @param decayPerSecond how much wears off per second
     * @param now            the current time in milliseconds
     */
    Fear(double decayPerSecond, long now) {
        this.decayPerSecond = decayPerSecond;
        this.updatedAt = now;
        this.lastSightingAt = NEVER;
    }

    /**
     * Reads the fear.
     *
     * @param now   the current time in milliseconds
     * @param floor the lowest the fear may be right now
     * @return the fear, between {@code floor} and {@code 1}
     */
    synchronized double read(long now, double floor) {
        this.settle(now, floor);
        return this.value;
    }

    /**
     * Adds to the fear.
     *
     * @param gain  how much to add
     * @param now   the current time in milliseconds
     * @param floor the lowest the fear may be right now
     */
    synchronized void add(double gain, long now, double floor) {
        this.settle(now, floor);
        this.value = Math.min(1.0D, this.value + gain);
    }

    /**
     * Counts a sighting, unless the last counted one is too recent.
     *
     * @param now            the current time in milliseconds
     * @param cooldownMillis the shortest gap between two counted sightings
     * @return {@code true} if this sighting counts
     */
    synchronized boolean trySighting(long now, long cooldownMillis) {
        if (this.lastSightingAt != NEVER && now - this.lastSightingAt < cooldownMillis) return false;
        this.lastSightingAt = now;
        return true;
    }

    /**
     * Takes off what wore off since the last update and lifts the value to the floor.
     */
    private void settle(long now, double floor) {
        long elapsed = now - this.updatedAt;
        if (elapsed > 0) {
            this.value = Math.max(0.0D, this.value - this.decayPerSecond * elapsed / 1000.0D);
            this.updatedAt = now;
        }
        this.value = Math.clamp(Math.max(this.value, floor), 0.0D, 1.0D);
    }
}
