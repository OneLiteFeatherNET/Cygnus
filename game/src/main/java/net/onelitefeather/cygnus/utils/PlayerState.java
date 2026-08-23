package net.onelitefeather.cygnus.utils;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Keeps one value per player, keyed by {@link Player#getUuid()}.
 * <p>
 * {@code EquipmentScreenOverlay}, {@code TunnelVisionService}, {@code BloodSplatterService},
 * {@code SlenderGazeService} and {@code TunnelVisionCommand} each hand-rolled their own
 * {@code Map<UUID, X>} field for this, disagreeing along the way on {@link ConcurrentHashMap} versus
 * {@link java.util.LinkedHashMap}. This type settles that: it is backed by a
 * {@code ConcurrentHashMap}, because state that outlives a single tick has to survive being written
 * from a scheduler task and cleared from a disconnect or death listener in the same round, and
 * nothing in this project pins both of those to the same thread. Three of the five call sites this
 * type replaces already reached for {@code ConcurrentHashMap} for exactly that reason; the other two
 * used a {@code LinkedHashMap} only for its insertion order, which none of the five ever relied on.
 * Correctness under a race a caller does not control beats an ordering guarantee nobody asked for.
 * </p>
 *
 * @param <V> the kind of value tracked per player
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class PlayerState<V> {

    private final Map<UUID, V> values = new ConcurrentHashMap<>();

    /**
     * Stores a value for the given player, replacing whatever was tracked before.
     *
     * @param player the player to store a value for
     * @param value  the value to store
     */
    public void put(Player player, V value) {
        this.values.put(player.getUuid(), value);
    }

    /**
     * Reads the value tracked for the given player.
     *
     * @param player the player to read
     * @return the tracked value, or {@code null} if none is tracked
     */
    public @Nullable V get(Player player) {
        return this.values.get(player.getUuid());
    }

    /**
     * Reads the value tracked for the given player, computing and storing one first if none is
     * tracked yet.
     *
     * @param player the player to read
     * @param supplier supplies the value to store when none is tracked yet
     * @return the tracked value, existing or freshly computed
     */
    public V computeIfAbsent(Player player, Supplier<V> supplier) {
        return this.values.computeIfAbsent(player.getUuid(), _ -> supplier.get());
    }

    /**
     * Stops tracking the given player.
     *
     * @param player the player to forget
     * @return the value that was tracked for them, or {@code null} if none was
     */
    public @Nullable V remove(Player player) {
        return this.values.remove(player.getUuid());
    }

    /**
     * The tracked values, without the players they belong to.
     * <p>
     * The returned collection is a live view over the backing map: removing through its iterator
     * also stops tracking that player, which is what lets a caller fade values out one by one while
     * walking them, the way {@code BloodSplatterService} does.
     * </p>
     *
     * @return a live view over the tracked values
     */
    public Collection<V> values() {
        return this.values.values();
    }

    /**
     * @return {@code true} if no player is currently tracked
     */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    /**
     * Stops tracking every player.
     */
    public void clear() {
        this.values.clear();
    }
}
