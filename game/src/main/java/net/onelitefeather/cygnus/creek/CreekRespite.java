package net.onelitefeather.cygnus.creek;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The survivors the creek leaves alone for a while, and until when.
 * <p>
 * A spared survivor is neither picked out on patrol, nor stalked, nor hunted. A stalk or hunt that
 * is running on them ends on the next step. The patrolling creek still shows itself to them and
 * still hides from their eyes, so it does not blink out of their sight.
 * </p>
 * <p>
 * The creek and the lure step on the scheduler thread, but a round can end from elsewhere, so the
 * map is a concurrent one.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class CreekRespite {

    private final Map<UUID, Long> until = new ConcurrentHashMap<>();

    /**
     * Spares a survivor until the given time. A later call replaces the earlier one.
     *
     * @param survivor the survivor to spare
     * @param until    when the creek may take them again, in milliseconds
     */
    public void spare(UUID survivor, long until) {
        this.until.put(survivor, until);
    }

    /**
     * Tells whether the creek leaves a survivor alone right now.
     *
     * @param survivor the survivor to check
     * @param now      the current time in milliseconds
     * @return {@code true} until the time they were spared until
     */
    public boolean spares(UUID survivor, long now) {
        return now < this.until.getOrDefault(survivor, Long.MIN_VALUE);
    }

    /**
     * Forgets every spared survivor, for the end of a round.
     */
    public void clear() {
        this.until.clear();
    }
}
