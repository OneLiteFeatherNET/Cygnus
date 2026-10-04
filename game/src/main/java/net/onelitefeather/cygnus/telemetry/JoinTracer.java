package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Traces a player joining: one span from the login until the first spawn.
 * <p>
 * A join is spread over several threads and packets - the login and the configuration run
 * asynchronously, the ResourcePack answers arrive on the network thread, the spawn on the tick
 * thread - so the span is kept per player UUID and the stations are span events on it. That makes the
 * time between two stations visible in the trace without a child span for each.
 * </p>
 * <p>
 * Every join span ends, by one of three routes: at the first spawn; on the disconnect of the
 * connection that opened it; or, for a connection that dies before a player exists (so no disconnect
 * ever comes), when {@link #sweep()} finds it older than the timeout and ends it as {@code timeout}. The sweep is driven by the
 * server tick and a clock that is injected, so nothing waits on wall time.
 * </p>
 * <p>
 * Each entry remembers the connection it belongs to. A player who reconnects under the same UUID
 * therefore cannot have the new join ended by the late disconnect of the old connection: that event
 * only ends its own span. The player is identified by UUID only.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.15.0
 */
public final class JoinTracer {

    /** The join reached the first spawn. */
    public static final String OUTCOME_SPAWNED = "spawned";
    /** The player left, was kicked, or timed out before the first spawn. */
    public static final String OUTCOME_ABANDONED = "abandoned";
    /** The join was still open when {@link #sweep()} gave up on it. */
    public static final String OUTCOME_TIMEOUT = "timeout";

    /** How long a join may stay open before {@link #sweep()} gives up on it. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    private final CygnusTracing tracing;
    private final LongSupplier nanoTime;
    private final long timeoutNanos;
    private final Map<UUID, Join> joins = new ConcurrentHashMap<>();

    /**
     * Creates the tracer with the default timeout and the system's monotonic clock.
     *
     * @param tracing where the spans go
     */
    public JoinTracer(CygnusTracing tracing) {
        this(tracing, System::nanoTime, DEFAULT_TIMEOUT);
    }

    /**
     * Creates the tracer.
     *
     * @param tracing  where the spans go
     * @param nanoTime the monotonic clock the age of a join is measured with
     * @param timeout  how long a join may stay open before {@link #sweep()} ends it
     */
    public JoinTracer(CygnusTracing tracing, LongSupplier nanoTime, Duration timeout) {
        this.tracing = tracing;
        this.nanoTime = nanoTime;
        this.timeoutNanos = timeout.toNanos();
    }

    /**
     * Opens the join span. A join still open for the same UUID, which belongs to an older connection,
     * is ended as abandoned first.
     *
     * @param player     the joining player
     * @param connection the connection the login arrived on
     */
    public void loginStarted(UUID player, Object connection) {
        Join created = new Join(connection, this.nanoTime.getAsLong(),
                this.tracing.root(CygnusAttributes.SPAN_PLAYER_JOIN)
                        .set(CygnusAttributes.PLAYER_UUID, player.toString()));
        Join previous = this.joins.put(player, created);
        if (previous != null) {
            previous.end(OUTCOME_ABANDONED);
        }
    }

    /**
     * Records that the player reached the configuration. Does nothing when no join is open for this
     * connection: a configuration that arrives after the disconnect must not open a span nobody
     * would ever end.
     *
     * @param player     the joining player
     * @param connection the connection of the configuration
     */
    public void configurationStarted(UUID player, Object connection) {
        Join join = ownJoin(player, connection);
        if (join != null) {
            join.step.span().addEvent(CygnusAttributes.EVENT_JOIN_CONFIGURATION);
        }
    }

    /**
     * Links the join to the round trace the player's cookie remembered, so the trace of this join and
     * the trace of the round the player came from can be followed into each other.
     *
     * @param player     the joining player
     * @param connection the connection of the join
     * @param previous   the span context read from the cookie
     */
    public void link(UUID player, Object connection, SpanContext previous) {
        Join join = ownJoin(player, connection);
        if (join != null) {
            join.step.span().addLink(previous, Attributes.of(
                    CygnusAttributes.LINK_KIND, CygnusAttributes.LINK_PREVIOUS_ROUND));
        }
    }

    /**
     * Records what the client reported about the ResourcePack.
     *
     * @param player     the joining player
     * @param connection the connection the report arrived on
     * @param status     what the client reported
     */
    public void resourcePackStatus(UUID player, Object connection, ResourcePackStatus status) {
        Join join = ownJoin(player, connection);
        if (join != null) {
            join.step.span().addEvent(CygnusAttributes.EVENT_JOIN_RESOURCEPACK, Attributes.of(
                    CygnusAttributes.RESOURCEPACK_STATUS, status.name()));
        }
    }

    /**
     * Ends the join span at the first spawn.
     *
     * @param player     the player who spawned
     * @param connection the connection of the player
     */
    public void spawned(UUID player, Object connection) {
        end(player, connection, OUTCOME_SPAWNED);
    }

    /**
     * Ends the join span of a connection that went away before it ended. Does nothing for a join that
     * already finished, which is nearly every disconnect, and nothing for a join of a newer connection.
     *
     * @param player     the player who left
     * @param connection the connection that closed
     */
    public void disconnected(UUID player, Object connection) {
        end(player, connection, OUTCOME_ABANDONED);
    }

    /**
     * Ends every join that has been open longer than the timeout, as timeout. Those are logins whose
     * connection died before a player existed, so no disconnect will ever name them.
     */
    public void sweep() {
        if (this.joins.isEmpty()) {
            return;
        }
        long now = this.nanoTime.getAsLong();
        this.joins.forEach((player, join) -> {
            if (now - join.startedNanos >= this.timeoutNanos && this.joins.remove(player, join)) {
                join.end(OUTCOME_TIMEOUT);
            }
        });
    }

    private Join ownJoin(UUID player, Object connection) {
        Join join = this.joins.get(player);
        return join != null && join.connection == connection ? join : null;
    }

    private void end(UUID player, Object connection, String outcome) {
        Join join = ownJoin(player, connection);
        // remove(key, value): if a newer login replaced the entry in between, this does nothing
        if (join != null && this.joins.remove(player, join)) {
            join.end(outcome);
        }
    }

    /**
     * Hooks the join events onto this tracer, and the sweep onto the server tick.
     * <p>
     * Register it before the listeners that can turn a player away, so the span exists when the kick
     * ends the join.
     * </p>
     *
     * @param node the node to listen on
     */
    public void register(EventNode<? super Event> node) {
        node.addListener(AsyncPlayerPreLoginEvent.class,
                event -> loginStarted(event.getGameProfile().uuid(), event.getConnection()));
        node.addListener(AsyncPlayerConfigurationEvent.class,
                event -> configurationStarted(event.getPlayer().getUuid(), event.getPlayer().getPlayerConnection()));
        node.addListener(PlayerResourcePackStatusEvent.class, event -> resourcePackStatus(
                event.getPlayer().getUuid(), event.getPlayer().getPlayerConnection(), event.getStatus()));
        node.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                spawned(event.getPlayer().getUuid(), event.getPlayer().getPlayerConnection());
            }
        });
        node.addListener(PlayerDisconnectEvent.class,
                event -> disconnected(event.getPlayer().getUuid(), event.getPlayer().getPlayerConnection()));
        node.addListener(ServerTickMonitorEvent.class, _ -> sweep());
    }

    private record Join(Object connection, long startedNanos, TraceStep step) {

        void end(String outcome) {
            this.step.set(CygnusAttributes.JOIN_OUTCOME, outcome).close();
        }
    }
}
