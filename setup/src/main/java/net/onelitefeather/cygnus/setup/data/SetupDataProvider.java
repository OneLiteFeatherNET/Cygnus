package net.onelitefeather.cygnus.setup.data;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to store the setup data for the players.
 * The data is used to store the setup mode and the map entry.
 *
 * @version 1.0.0
 * @see SetupData
 * @since 1.0.0
 */
public final class SetupDataProvider {

    private final ReentrantLock lock;
    private final Map<Player, SetupData> setupDataCache;

    /**
     * Constructs a new {@link SetupDataProvider} instance.
     * The provider is used to store the setup data for the players.
     */
    public SetupDataProvider() {
        this.setupDataCache = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    /**
     * Adds the setup data to the cache.
     *
     * @param player    the player to add the data for
     * @param setupData the data to add
     */
    public void addSetupData(@NotNull Player player, @NotNull SetupData setupData) {
        lock.lock();
        try {
            setupDataCache.put(player, setupData);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the setup data from the cache.
     *
     * @param player the player to remove the data from
     * @return {@code true} if the data was removed successfully
     */
    public boolean removeSetupData(@NotNull Player player) {
        lock.lock();
        try {
            return setupDataCache.remove(player) != null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the setup data for the given player.
     *
     * @param player the player to get the data from
     * @return the setup data or {@code null} if no data was found
     */
    public @Nullable SetupData getSetupData(@NotNull Player player) {
        lock.lock();
        try {
            return setupDataCache.get(player);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears the cache
     */
    public void clear() {
        lock.lock();
        try {
            setupDataCache.clear();
        } finally {
            lock.unlock();
        }
    }
}
