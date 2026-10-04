package net.onelitefeather.cygnus.sanity;

/**
 * One survivor's fear.
 * <p>
 * The value wears off on its own. Instead of a task lowering it every tick, each read or write
 * first takes off what wore off since the last one. The creek asks for it every step anyway.
 * </p>
 * <p>
 * Every jump leaves a scar: a share of it that never wears off again. The scar sits on top of the
 * floor the caller passes in, and both together never go above the cap, so a scarred survivor
 * stays tense without being hunted for good.
 * </p>
 * <p>
 * The creek reads and writes from the scheduler thread while deaths and pages arrive from others,
 * so every method is synchronized.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.16.0
 */
final class Fear {

    private static final long NEVER = Long.MIN_VALUE;

    /**
     * The longest gap between two stalk reports that still counts as one stalk. The creek reports
     * every 100 ms, so anything longer means the stalk had stopped in between.
     */
    static final long STALK_GAP_MILLIS = 1000L;

    private final double decayPerSecond;
    private final double residualShare;
    private final double floorCap;
    private double value;
    private double scar;
    private long updatedAt;
    private long lastSightingAt;
    private long lastStalkedAt;

    /**
     * Creates a calm survivor's fear.
     *
     * @param decayPerSecond how much wears off per second
     * @param residualShare  the share of every jump that never wears off again
     * @param floorCap       the highest the floor and the scar together may lift the value
     * @param now            the current time in milliseconds
     */
    Fear(double decayPerSecond, double residualShare, double floorCap, long now) {
        this.decayPerSecond = decayPerSecond;
        this.residualShare = residualShare;
        this.floorCap = floorCap;
        this.updatedAt = now;
        this.lastSightingAt = NEVER;
        this.lastStalkedAt = NEVER;
    }

    /**
     * Reads the fear.
     *
     * @param now   the current time in milliseconds
     * @param floor the lowest the fear may be right now, before the scar
     * @return the fear, between the floor and {@code 1}
     */
    synchronized double read(long now, double floor) {
        this.settle(now, floor);
        return this.value;
    }

    /**
     * Adds to the fear. A share of the gain stays for good.
     *
     * @param gain  how much to add
     * @param now   the current time in milliseconds
     * @param floor the lowest the fear may be right now, before the scar
     */
    synchronized void add(double gain, long now, double floor) {
        this.settle(now, floor);
        this.value = Math.min(1.0D, this.value + gain);
        this.scar = Math.min(1.0D, this.scar + gain * this.residualShare);
    }

    /**
     * A creek is stalking the survivor right now. The fear grows by the time since the last
     * report, as long as that report belongs to the same stalk. Unlike a jump, it leaves no scar:
     * the stalk itself is not the scare, what it leads to is.
     *
     * @param gainPerSecond how much the fear grows per second of stalking
     * @param now           the current time in milliseconds
     * @param floor         the lowest the fear may be right now, before the scar
     */
    synchronized void stalked(double gainPerSecond, long now, double floor) {
        long last = this.lastStalkedAt;
        this.lastStalkedAt = now;
        this.settle(now, floor);
        if (last == NEVER || now <= last || now - last > STALK_GAP_MILLIS) return;
        this.value = Math.min(1.0D, this.value + gainPerSecond * (now - last) / 1000.0D);
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
     * Takes off what wore off since the last update and lifts the value to the floor plus the
     * scar, capped.
     */
    private void settle(long now, double floor) {
        long elapsed = now - this.updatedAt;
        if (elapsed > 0) {
            this.value = Math.max(0.0D, this.value - this.decayPerSecond * elapsed / 1000.0D);
            this.updatedAt = now;
        }
        double lowest = Math.min(this.floorCap, floor + this.scar);
        this.value = Math.clamp(Math.max(this.value, lowest), 0.0D, 1.0D);
    }
}
