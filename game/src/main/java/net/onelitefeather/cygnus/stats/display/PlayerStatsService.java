package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.stats.KnownPlayer;
import net.onelitefeather.cygnus.stats.StatsRepository;
import net.onelitefeather.cygnus.stats.YearStatsView;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Shows players their statistics: the chat overview, the menu, the cached menu of the lobby item and the
 * extra line of the round summary.
 * <p>
 * Every read runs on the repository's executor, never on the tick thread. The answers come back on the
 * injected server-thread executor, which is where the messages are sent and the menus are opened, so
 * nothing here touches a player from the database thread. The cache holds the current year of the
 * players who are online and nothing else: a player is tracked from the {@linkplain #preload(Player)
 * preload} on join until {@link #forget(Player)} on quit, and a round end drops the entries with
 * {@link #invalidate()}. A load that was started before either one is dropped when it completes.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class PlayerStatsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStatsService.class);

    private final StatsRepository repository;
    private final Clock clock;
    private final ZoneId zone;
    private final ConnectionManager connections;
    private final Executor serverThread;
    private final Map<UUID, YearStatsView> cache = new ConcurrentHashMap<>();
    private final Set<UUID> tracked = ConcurrentHashMap.newKeySet();
    private final AtomicInteger epoch = new AtomicInteger();

    /**
     * Creates the service.
     *
     * @param repository  where the statistics are read from
     * @param clock       the source of the current time
     * @param zone        the zone whose calendar decides the current year
     * @param connections finds the online players
     * @param serverThread runs a task on the server thread, for example the scheduler's next tick
     */
    public PlayerStatsService(StatsRepository repository, Clock clock, ZoneId zone, ConnectionManager connections,
                              Executor serverThread) {
        this.repository = repository;
        this.clock = clock;
        this.zone = zone;
        this.connections = connections;
        this.serverThread = serverThread;
    }

    /**
     * Tells whether statistics are available at all.
     *
     * @return {@code false} when they are off or the database could not be opened
     */
    public boolean enabled() {
        return this.repository.enabled();
    }

    /**
     * Returns the current calendar year in the configured zone.
     *
     * @return the year
     */
    public int currentYear() {
        return LocalDate.now(this.clock.withZone(this.zone)).getYear();
    }

    /**
     * Returns the names of the players who are online, for tab completion.
     *
     * @return the names
     */
    public List<String> onlineNames() {
        return this.connections.getOnlinePlayers().stream().map(Player::getUsername).toList();
    }

    /**
     * Returns what the cache holds for a player.
     *
     * @param uuid the player
     * @return the cached year, empty when nothing is cached
     */
    public Optional<YearStatsView> cachedView(UUID uuid) {
        return Optional.ofNullable(this.cache.get(uuid));
    }

    /**
     * Prints the overview of a player's year to the sender.
     *
     * @param sender the one who asked
     * @param target the name to look up, {@code null} for the sender themselves
     * @param year   the calendar year
     * @return completes when the sender has been answered
     */
    public CompletableFuture<Void> showOverview(CommandSender sender, @Nullable String target, int year) {
        return lookup(sender, target, year, (player, view) -> sender.sendMessage(StatsOverview.chat(player.name(), view)));
    }

    /**
     * Opens the menu of a player's year for the viewer.
     *
     * @param viewer the one who asked
     * @param target the name to look up, {@code null} for the viewer themselves
     * @param year   the calendar year
     * @return completes when the viewer has been answered
     */
    public CompletableFuture<Void> openMenu(Player viewer, @Nullable String target, int year) {
        return lookup(viewer, target, year, (player, view) -> viewer.openInventory(StatsMenu.of(player.name(), view)));
    }

    /**
     * Opens the player's own menu for the current year, at once when the year is cached. Otherwise the
     * player is told that it loads and the menu follows when it is there. A player without a round in the
     * year gets a menu of zeros.
     *
     * @param player the player who clicked the lobby item
     * @return completes when the player has been answered
     */
    public CompletableFuture<Void> openOwnMenu(Player player) {
        if (!enabled()) {
            player.sendMessage(StatsMessages.notAvailable());
            return CompletableFuture.completedFuture(null);
        }
        int year = currentYear();
        YearStatsView cached = this.cache.get(player.getUuid());
        if (cached != null && cached.year() == year) {
            player.openInventory(StatsMenu.of(player.getUsername(), cached));
            return CompletableFuture.completedFuture(null);
        }
        player.sendMessage(StatsMessages.loading());
        int started = this.epoch.get();
        return load(player.getUuid(), year)
                .thenAcceptAsync(view -> {
                    if (isOnline(player)) {
                        remember(player.getUuid(), view, started);
                        player.openInventory(StatsMenu.of(player.getUsername(), view));
                    }
                }, this.serverThread)
                .exceptionally(failure -> {
                    LOGGER.error("Loading the statistics of {} failed", player.getUsername(), failure);
                    this.serverThread.execute(() -> player.sendMessage(StatsMessages.loadFailed()));
                    return null;
                });
    }

    /**
     * Starts loading a player's current year so the first click on the lobby item is instant. Does
     * nothing while statistics are off. A failure is logged and leaves the cache empty, the click then
     * loads again.
     *
     * @param player the player who joined
     */
    public void preload(Player player) {
        if (!enabled()) {
            return;
        }
        UUID uuid = player.getUuid();
        this.tracked.add(uuid);
        int started = this.epoch.get();
        load(uuid, currentYear())
                .thenAcceptAsync(view -> remember(uuid, view, started), this.serverThread)
                .exceptionally(failure -> {
                    LOGGER.warn("Preloading the statistics of {} failed: {}", player.getUsername(), failure.toString());
                    return null;
                });
    }

    /**
     * Stops tracking a player who left and drops their cached year.
     *
     * @param player the player who left
     */
    public void forget(Player player) {
        this.tracked.remove(player.getUuid());
        this.cache.remove(player.getUuid());
    }

    /**
     * Drops every cached year and discards the loads that are still on their way, because the numbers
     * they carry are about to change with the round that just ended.
     */
    public void invalidate() {
        this.epoch.incrementAndGet();
        this.cache.clear();
    }

    /**
     * Takes the year totals a committed round produced: refreshes the cache and sends every online
     * participant the extra line of the round summary. Safe to call from any thread, the work is done on
     * the server thread.
     *
     * @param totals the committed year row of each participant
     */
    public void roundSaved(Map<UUID, YearStatsView> totals) {
        this.serverThread.execute(() -> totals.forEach((uuid, view) -> {
            Player player = this.connections.getOnlinePlayerByUuid(uuid);
            if (player == null) {
                return;
            }
            if (this.tracked.contains(uuid) && view.year() == currentYear()) {
                this.cache.put(uuid, view);
            }
            player.sendMessage(StatsMessages.roundTotals(view));
        }));
    }

    private boolean isOnline(Player player) {
        return !player.isRemoved() && this.connections.getOnlinePlayerByUuid(player.getUuid()) != null;
    }

    private void remember(UUID uuid, YearStatsView view, int startedAt) {
        if (this.tracked.contains(uuid) && this.epoch.get() == startedAt) {
            this.cache.put(uuid, view);
        }
    }

    private CompletableFuture<YearStatsView> load(UUID uuid, int year) {
        return this.repository.findYear(uuid, year).thenApply(view -> view.orElseGet(() -> YearStatsView.empty(uuid, year)));
    }

    /**
     * The three answers a lookup can come to.
     */
    private sealed interface Outcome {

        record Unknown(String name) implements Outcome {
        }

        record NoRounds(KnownPlayer player, boolean self, List<Integer> years) implements Outcome {
        }

        record Found(KnownPlayer player, YearStatsView view) implements Outcome {
        }
    }

    private CompletableFuture<Void> lookup(CommandSender sender, @Nullable String target, int year,
                                           BiConsumer<KnownPlayer, YearStatsView> found) {
        if (!enabled()) {
            sender.sendMessage(StatsMessages.notAvailable());
            return CompletableFuture.completedFuture(null);
        }
        if (target == null && !(sender instanceof Player)) {
            sender.sendMessage(StatsMessages.onlyPlayers());
            return CompletableFuture.completedFuture(null);
        }
        String typed = target == null ? ((Player) sender).getUsername() : target;
        return resolve(sender, target)
                .thenCompose(known -> known.map(player -> read(sender, player, year)).orElseGet(
                        () -> CompletableFuture.completedFuture(new Outcome.Unknown(typed))))
                .thenAcceptAsync(outcome -> answer(sender, outcome, year, found), this.serverThread)
                .exceptionally(failure -> {
                    LOGGER.error("Looking up the statistics of {} for {} failed", typed, sender.getClass().getSimpleName(), failure);
                    this.serverThread.execute(() -> sender.sendMessage(StatsMessages.loadFailed()));
                    return null;
                });
    }

    /**
     * Finds the player a lookup is about: the sender, an online player by name, or the last known name in
     * the database.
     */
    private CompletableFuture<Optional<KnownPlayer>> resolve(CommandSender sender, @Nullable String target) {
        if (target == null) {
            Player self = (Player) sender;
            return CompletableFuture.completedFuture(Optional.of(new KnownPlayer(self.getUuid(), self.getUsername())));
        }
        Player online = this.connections.getOnlinePlayerByUsername(target);
        if (online != null) {
            return CompletableFuture.completedFuture(Optional.of(new KnownPlayer(online.getUuid(), online.getUsername())));
        }
        return this.repository.findPlayerByName(target);
    }

    private CompletableFuture<Outcome> read(CommandSender sender, KnownPlayer player, int year) {
        boolean self = sender instanceof Player viewer && viewer.getUuid().equals(player.uuid());
        return this.repository.findYear(player.uuid(), year).thenCompose(view -> view
                .<CompletableFuture<Outcome>>map(found -> CompletableFuture.completedFuture(new Outcome.Found(player, found)))
                .orElseGet(() -> this.repository.availableYears(player.uuid())
                        .thenApply(years -> new Outcome.NoRounds(player, self, years))));
    }

    private void answer(CommandSender sender, Outcome outcome, int year, BiConsumer<KnownPlayer, YearStatsView> found) {
        Component message = switch (outcome) {
            case Outcome.Unknown(String name) -> StatsMessages.unknownPlayer(name);
            case Outcome.NoRounds(KnownPlayer player, boolean self, List<Integer> years) -> self
                    ? StatsMessages.selfWithoutRounds(year, years)
                    : StatsMessages.noStatisticsInYear(player.name(), year, years);
            case Outcome.Found(KnownPlayer player, YearStatsView view) -> {
                found.accept(player, view);
                yield null;
            }
        };
        if (message != null) {
            sender.sendMessage(message);
        }
    }
}
