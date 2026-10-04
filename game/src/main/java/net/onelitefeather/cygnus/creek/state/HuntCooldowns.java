package net.onelitefeather.cygnus.creek.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers when each survivor's last hunt ended, so a survivor gets a breather before the next one.
 * <p>
 * Every creek of a round shares one instance: a variant must not hunt someone the moment another
 * creek let go of them. Without it, a caught survivor was scared enough to be hunted again by the
 * next stalk, and again after that.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public final class HuntCooldowns {

    private final long cooldownMillis;
    private final Map<UUID, Long> endedAt = new ConcurrentHashMap<>();

    /**
     * Creates an empty record.
     *
     * @param cooldownMillis how long a survivor is safe after a hunt ends, in milliseconds
     */
    public HuntCooldowns(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    /**
     * Creates a record without any breather, for tests and rounds that do not need one.
     *
     * @return the record
     */
    public static HuntCooldowns none() {
        return new HuntCooldowns(0L);
    }

    /**
     * A hunt on a survivor has ended, however it ended.
     *
     * @param survivor the survivor who was hunted
     * @param now      the current time in milliseconds
     */
    public void ended(UUID survivor, long now) {
        this.endedAt.put(survivor, now);
    }

    /**
     * Tells whether a survivor may be hunted again.
     *
     * @param survivor the survivor to check
     * @param now      the current time in milliseconds
     * @return {@code true} if they were never hunted or the breather is over
     */
    public boolean ready(UUID survivor, long now) {
        Long ended = this.endedAt.get(survivor);
        return ended == null || now - ended >= this.cooldownMillis;
    }
}
