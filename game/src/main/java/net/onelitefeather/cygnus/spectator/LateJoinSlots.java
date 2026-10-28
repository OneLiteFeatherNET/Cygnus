package net.onelitefeather.cygnus.spectator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory bookkeeping for the places of players who join a running round as spectators.
 * <p>
 * Only late joiners hold a place. A player who dies during the round is never added here, so
 * dying does not give up or consume a late-join place. A limit of {@code 0} disables late joining.
 * </p>
 * <p>
 * All methods are synchronized on this instance, so the check for a free place and the insert
 * of the player happen in one step and two players can never take the last place together.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class LateJoinSlots {

    private final int limit;
    private final Set<UUID> holders = new HashSet<>();

    /**
     * Creates a new bookkeeping with the given number of places.
     *
     * @param limit the number of places, {@code 0} disables late joining
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    public LateJoinSlots(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("The late join limit must not be negative: " + limit);
        }
        this.limit = limit;
    }

    /**
     * Returns whether late joining is enabled at all.
     *
     * @return {@code true} if the limit is greater than {@code 0}
     */
    public boolean enabled() {
        return limit > 0;
    }

    /**
     * Reserves a place for the player if one is free. A player who already holds a place keeps it
     * without using a second one.
     *
     * @param player the joining player
     * @return {@code true} if the player now holds a place, {@code false} if all places are taken or late joining is disabled
     */
    public synchronized boolean tryReserve(UUID player) {
        if (holders.contains(player)) {
            return true;
        }
        if (holders.size() >= limit) {
            return false;
        }
        return holders.add(player);
    }

    /**
     * Frees the place of the player. Does nothing if the player holds no place.
     *
     * @param player the player who leaves the late-join places
     */
    public synchronized void release(UUID player) {
        holders.remove(player);
    }

    /**
     * Returns whether the player currently holds a place.
     *
     * @param player the player to check
     * @return {@code true} if the player holds a place
     */
    public synchronized boolean holds(UUID player) {
        return holders.contains(player);
    }

    /**
     * Returns the number of places currently taken.
     *
     * @return the count of players holding a place
     */
    public synchronized int size() {
        return holders.size();
    }

    /**
     * Frees all places. Called when a new round starts.
     */
    public synchronized void clear() {
        holders.clear();
    }
}
