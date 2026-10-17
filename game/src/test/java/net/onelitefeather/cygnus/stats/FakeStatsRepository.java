package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An in-memory {@link StatsRepository} for the game's tests. Every future it returns is already
 * completed, so tests need no executor and no waiting, unless {@link #deferReads} asks for pending
 * reads that the test completes itself with {@link #completeReads()}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class FakeStatsRepository implements StatsRepository {

    /** The rounds handed to {@link #saveRound(FinishedRound)}. */
    public final List<FinishedRound> rounds = new ArrayList<>();
    /** What {@link #enabled()} answers. */
    public boolean enabled = true;
    /** When set, {@link #saveRound(FinishedRound)} throws it. */
    public @Nullable RuntimeException saveThrows;
    /** When set, {@link #saveRound(FinishedRound)} returns a future that failed with it. */
    public @Nullable Throwable saveFails;
    /** Computes the committed totals {@link #saveRound(FinishedRound)} completes with. */
    public Function<FinishedRound, Map<UUID, YearStatsView>> totals = _ -> Map.of();
    /** When set, every read returns a future that failed with it. */
    public @Nullable Throwable readFails;
    /** When {@code true}, reads stay pending until {@link #completeReads()}. */
    public boolean deferReads;
    /** How often {@link #findYear(UUID, int)} was called. */
    public int yearReads;
    /** How often {@link #findPlayerByName(String)} was called. */
    public int nameReads;

    private final Map<String, KnownPlayer> names = new HashMap<>();
    private final Map<UUID, Map<Integer, YearStatsView>> views = new HashMap<>();
    private final Deque<Runnable> deferred = new ArrayDeque<>();

    /**
     * Teaches the repository a player's last known name.
     *
     * @param uuid the player
     * @param name the name
     */
    public void knowPlayer(UUID uuid, String name) {
        this.names.put(name.toLowerCase(Locale.ROOT), new KnownPlayer(uuid, name));
    }

    /**
     * Stores a year row.
     *
     * @param view the year of a player
     */
    public void store(YearStatsView view) {
        this.views.computeIfAbsent(view.playerUuid(), _ -> new HashMap<>()).put(view.year(), view);
    }

    /**
     * Completes every read that was deferred, in the order it was made.
     */
    public void completeReads() {
        while (!this.deferred.isEmpty()) {
            this.deferred.poll().run();
        }
    }

    @Override
    public boolean enabled() {
        return this.enabled;
    }

    @Override
    public CompletableFuture<Map<UUID, YearStatsView>> saveRound(FinishedRound round) {
        if (this.saveThrows != null) {
            throw this.saveThrows;
        }
        this.rounds.add(round);
        if (this.saveFails != null) {
            return CompletableFuture.failedFuture(this.saveFails);
        }
        return CompletableFuture.completedFuture(this.totals.apply(round));
    }

    @Override
    public CompletableFuture<Optional<YearStatsView>> findYear(UUID playerUuid, int year) {
        this.yearReads++;
        return read(() -> Optional.ofNullable(this.views.getOrDefault(playerUuid, Map.of()).get(year)));
    }

    @Override
    public CompletableFuture<Optional<KnownPlayer>> findPlayerByName(String name) {
        this.nameReads++;
        return read(() -> Optional.ofNullable(this.names.get(name.toLowerCase(Locale.ROOT))));
    }

    @Override
    public CompletableFuture<List<Integer>> availableYears(UUID playerUuid) {
        return read(() -> List.copyOf(new TreeSet<>(this.views.getOrDefault(playerUuid, Map.of()).keySet()).descendingSet()));
    }

    @Override
    public int deleteByPlayer(UUID playerUuid) {
        return 0;
    }

    @Override
    public void close() {
    }

    private <T> CompletableFuture<T> read(Supplier<T> supplier) {
        if (this.readFails != null) {
            return CompletableFuture.failedFuture(this.readFails);
        }
        if (!this.deferReads) {
            return CompletableFuture.completedFuture(supplier.get());
        }
        CompletableFuture<T> pending = new CompletableFuture<>();
        this.deferred.add(() -> pending.complete(supplier.get()));
        return pending;
    }
}
