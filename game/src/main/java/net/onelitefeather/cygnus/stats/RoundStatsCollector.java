package net.onelitefeather.cygnus.stats;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.PainkillerUsedEvent;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The game-side adapter of the statistics: translates the game's events into calls on the
 * {@link RoundStatsTracker} and hands the finished round to the {@link StatsRepository}.
 * <p>
 * Everything between {@link #roundStarted(Map)} and {@link #gameFinished(GameFinishEvent.Reason)}
 * only touches the tracker's in-memory state, so event handling never does I/O; the repository moves
 * the write off the tick thread. Only players on a team at the start are tracked, a spectator who
 * joins later gets no row. {@link #register} adapts the game's events onto these methods.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class RoundStatsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoundStatsCollector.class);

    private final StatsRepository repository;
    private final RoundStatsTracker tracker;
    private final Supplier<@Nullable String> roundIds;
    private final Function<UUID, @Nullable String> names;
    private final Consumer<Map<UUID, YearStatsView>> totalsHandler;

    /**
     * Creates the collector without names and without a hand-over of the committed totals.
     *
     * @param repository where finished rounds go
     * @param clock      the source of every timestamp
     * @param zone       the zone whose calendar decides the year of a round
     * @param roundIds   the id of the running round as the round telemetry knows it
     */
    public RoundStatsCollector(StatsRepository repository, Clock clock, ZoneId zone, Supplier<@Nullable String> roundIds) {
        this(repository, clock, zone, roundIds, _ -> null, _ -> {
        });
    }

    /**
     * Creates the collector.
     *
     * @param repository    where finished rounds go
     * @param clock         the source of every timestamp
     * @param zone          the zone whose calendar decides the year of a round
     * @param roundIds      the id of the running round as the round telemetry knows it
     * @param names         reads the name of a participant when the round starts, {@code null} for an unknown one
     * @param totalsHandler receives the year totals the repository committed for the round's participants. It
     *                      runs on the thread that finished the write, not on the tick thread, and is not called
     *                      when the write failed
     */
    public RoundStatsCollector(StatsRepository repository, Clock clock, ZoneId zone, Supplier<@Nullable String> roundIds,
                               Function<UUID, @Nullable String> names, Consumer<Map<UUID, YearStatsView>> totalsHandler) {
        this.repository = repository;
        this.tracker = new RoundStatsTracker(clock, zone);
        this.roundIds = roundIds;
        this.names = names;
        this.totalsHandler = totalsHandler;
    }

    /**
     * Starts tracking the given players. A round still tracked from before is dropped.
     *
     * @param players every member of the Slender and Survivor teams with the role they start in
     */
    public void roundStarted(Map<UUID, Role> players) {
        Map<UUID, String> started = new HashMap<>();
        players.keySet().forEach(uuid -> {
            String name = this.names.apply(uuid);
            if (name != null) {
                started.put(uuid, name);
            }
        });
        this.tracker.roundStarted(players, started);
    }

    /**
     * Ends the round: hands it to the repository and clears the state. A failing repository is logged
     * and does not reach the caller. Once the write is committed, the totals go to the totals handler;
     * a failed write has been logged and counted by the repository and is skipped here.
     *
     * @param reason why the round ended
     */
    public void gameFinished(GameFinishEvent.Reason reason) {
        this.tracker.finish(FinishReason.valueOf(reason.name()), () -> roundId(this.roundIds.get())).ifPresent(round -> {
            try {
                this.repository.saveRound(round).whenComplete((totals, failure) -> {
                    if (failure == null) {
                        handOn(totals);
                    }
                });
            } catch (RuntimeException exception) {
                LOGGER.error("Handing round {} to the statistics failed", round.roundId(), exception);
            }
        });
    }

    private void handOn(Map<UUID, YearStatsView> totals) {
        try {
            this.totalsHandler.accept(totals);
        } catch (RuntimeException exception) {
            LOGGER.error("Showing the committed statistics failed", exception);
        }
    }

    /**
     * Hooks the game's events onto the collector.
     * <p>
     * Register it <em>before</em> the listeners that handle the same events: a quit or a death can end
     * the round from inside their handler, and the collector has to know about it by then.
     * </p>
     *
     * @param node         the node to listen on
     * @param participants reads every member of the Slender and Survivor teams with their role
     * @param slender      reads the UUID of the Slender who gets kill credit, {@code null} when there is none
     */
    public void register(EventNode<? super Event> node, Supplier<Map<UUID, Role>> participants,
                         Supplier<@Nullable UUID> slender) {
        node.addListener(GameStartEvent.class, _ -> roundStarted(participants.get()));
        node.addListener(GameFinishEvent.class, event -> gameFinished(event.reason()));
        node.addListener(PageFoundEvent.class,
                event -> this.tracker.pageFound(event.finder().getUuid(), event.foundCount(), event.maxPages()));
        node.addListener(PlayerDeathEvent.class,
                event -> this.tracker.playerDied(event.getPlayer().getUuid(), slender.get()));
        node.addListener(PlayerDisconnectEvent.class, event -> this.tracker.playerLeft(event.getPlayer().getUuid()));
        node.addListener(PlayerDamagedEvent.class,
                event -> this.tracker.damageTaken(event.getPlayer().getUuid(), event.getPoints()));
        node.addListener(PainkillerUsedEvent.class,
                event -> this.tracker.painkillerUsed(event.getPlayer().getUuid()));
        node.addListener(SlenderReviveEvent.class,
                event -> this.tracker.slenderTakenOver(event.getPlayer().getUuid()));
    }

    /**
     * Reads the id of the round as a UUID. The telemetry hands out a UUID string in production; anything
     * else is replaced by a random one so the write still has a key.
     *
     * @param id the id from the round telemetry
     * @return the id as UUID
     */
    static UUID roundId(@Nullable String id) {
        if (id != null) {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Round id {} is not a UUID, using a random one for the statistics", id);
            }
        }
        return UUID.randomUUID();
    }
}
