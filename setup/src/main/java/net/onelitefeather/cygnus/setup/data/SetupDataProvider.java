package net.onelitefeather.cygnus.setup.data;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public final class SetupDataProvider {

    private final ReentrantLock lock;
    private final Map<Player, SetupData> setupDataCache;

    public SetupDataProvider() {
        this.setupDataCache = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    public void clear() {
        lock.lock();
        try {
            setupDataCache.clear();
        } finally {
            lock.unlock();
        }
    }

    public void addSetupData(@NotNull Player player, @NotNull SetupData setupData) {
        lock.lock();
        try {
            setupDataCache.put(player, setupData);
        } finally {
            lock.unlock();
        }
    }

    public @Nullable SetupData getSetupData(@NotNull Player player) {
        lock.lock();
        try {
            return setupDataCache.get(player);
        } finally {
            lock.unlock();
        }
    }

    public boolean removeSetupData(@NotNull Player player) {
        lock.lock();
        try {
            return setupDataCache.remove(player) != null;
        } finally {
            lock.unlock();
        }
    }
}
