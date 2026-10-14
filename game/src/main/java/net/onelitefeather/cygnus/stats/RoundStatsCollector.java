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
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Collects what every participant does in a round and hands it to the {@link StatsRepository} when
 * the round ends.
 * <p>
 * Everything between {@link #roundStarted(Map)} and {@link #gameFinished(GameFinishEvent.Reason)}
 * only touches an in-memory map, so event handling never does I/O; the repository moves the write off
 * the tick thread. Only players on a team at the start are tracked, a spectator who joins later gets
 * no row. The methods that take plain values are what the tests drive, {@link #register} adapts the
 * game's events onto them.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class RoundStatsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoundStatsCollector.class);

    private final StatsRepository repository;
    private final Clock clock;
    private final ZoneId zone;
    private final Supplier<@Nullable String> roundIds;
    private final Map<UUID, RoundStats.Builder> participants = new HashMap<>();

    /**
     * Creates the collector.
     *
     * @param repository where finished rounds go
     * @param clock      the source of every timestamp
     * @param zone       the zone whose calendar decides the year of a round
     * @param roundIds   the id of the running round as the round telemetry knows it
     */
    public RoundStatsCollector(StatsRepository repository, Clock clock, ZoneId zone, Supplier<@Nullable String> roundIds) {
        this.repository = repository;
        this.clock = clock;
        this.zone = zone;
        this.roundIds = roundIds;
    }

    /**
     * Starts tracking the given players. A round still tracked from before is dropped.
     *
     * @param players every member of the Slender and Survivor teams with the role they start in
     */
    public synchronized void roundStarted(Map<UUID, Role> players) {
        this.participants.clear();
        Instant started = this.clock.instant();
        players.forEach((uuid, role) -> this.participants.put(uuid, new RoundStats.Builder(uuid, role, started)));
    }

    /**
     * Counts a found page for its finder.
     *
     * @param finder the survivor who found it
     * @param found  how many pages were found including this one
     * @param max    how many pages the round needs
     */
    public synchronized void pageFound(UUID finder, int found, int max) {
        RoundStats.Builder builder = active(finder);
        if (builder != null) {
            builder.pageFound(found >= max);
        }
    }

    /**
     * Records a death and credits the kill.
     *
     * @param victim the player who died
     * @param killer the Slender who gets the credit, or {@code null} for nobody
     */
    public synchronized void playerDied(UUID victim, @Nullable UUID killer) {
        RoundStats.Builder dead = active(victim);
        if (dead == null || dead.role() != Role.SURVIVOR) {
            return;
        }
        dead.died(this.clock.instant());
        if (killer != null) {
            RoundStats.Builder credited = active(killer);
            if (credited != null) {
                credited.kill();
            }
        }
    }

    /**
     * Adds damage a participant took.
     *
     * @param player the player who was hit
     * @param points the vitality points lost
     */
    public synchronized void damageTaken(UUID player, double points) {
        RoundStats.Builder builder = active(player);
        if (builder != null) {
            builder.damage(points);
        }
    }

    /**
     * Counts a painkiller a participant took.
     *
     * @param player the player who took it
     */
    public synchronized void painkillerUsed(UUID player) {
        RoundStats.Builder builder = active(player);
        if (builder != null) {
            builder.painkiller();
        }
    }

    /**
     * Freezes a participant who quit before the round ended: their outcome is
     * {@link RoundStats.Outcome#ABANDONED} and their play time ends now.
     *
     * @param player the player who left
     */
    public synchronized void playerLeft(UUID player) {
        RoundStats.Builder builder = active(player);
        if (builder != null) {
            builder.left(this.clock.instant());
        }
    }

    /**
     * Ends the round: resolves every outcome, hands the round to the repository and clears the state.
     * A failing repository is logged and does not reach the caller.
     *
     * @param reason why the round ended
     */
    public synchronized void gameFinished(GameFinishEvent.Reason reason) {
        if (this.participants.isEmpty()) {
            return;
        }
        Instant finished = this.clock.instant();
        int pagesAgainst = this.participants.values().stream()
                .filter(builder -> builder.role() == Role.SURVIVOR)
                .mapToInt(RoundStats.Builder::pagesFound)
                .sum();
        List<RoundStats> stats = new ArrayList<>(this.participants.size());
        this.participants.values().forEach(builder -> stats.add(builder.build(reason, finished, pagesAgainst)));
        this.participants.clear();
        UUID roundId = roundId(this.roundIds.get());
        try {
            this.repository.saveRound(new FinishedRound(roundId, finished, finished.atZone(this.zone).getYear(), stats));
        } catch (RuntimeException exception) {
            LOGGER.error("Handing round {} to the statistics failed", roundId, exception);
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
                event -> pageFound(event.finder().getUuid(), event.foundCount(), event.maxPages()));
        node.addListener(PlayerDeathEvent.class, event -> playerDied(event.getPlayer().getUuid(), slender.get()));
        node.addListener(PlayerDisconnectEvent.class, event -> playerLeft(event.getPlayer().getUuid()));
        node.addListener(PlayerDamagedEvent.class,
                event -> damageTaken(event.getPlayer().getUuid(), event.getPoints()));
        node.addListener(PainkillerUsedEvent.class, event -> painkillerUsed(event.getPlayer().getUuid()));
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

    /**
     * Reads a participant who is still counting: one who is not tracked or who quit counts nothing.
     */
    private RoundStats.@Nullable Builder active(UUID player) {
        RoundStats.Builder builder = this.participants.get(player);
        return builder == null || builder.frozen() ? null : builder;
    }
}
