package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.Attributes;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Traces a player joining: one span from the login until the first spawn.
 * <p>
 * A join is spread over several threads and packets - the login and the configuration run
 * asynchronously, the ResourcePack answers arrive on the network thread, the spawn on the tick
 * thread - so the span is kept per player UUID and the stations are span events on it. That makes the
 * time between two stations visible in the trace without a child span for each.
 * </p>
 * <p>
 * Every join span ends: at the first spawn, or, for a player who leaves or is turned away before
 * that, on the disconnect. The player is identified by UUID only.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class JoinTracer {

    /** The join reached the first spawn. */
    public static final String OUTCOME_SPAWNED = "spawned";
    /** The player left, or was kicked, before the first spawn. */
    public static final String OUTCOME_ABANDONED = "abandoned";

    private final CygnusTracing tracing;
    private final Map<UUID, TraceStep> joins = new ConcurrentHashMap<>();

    /**
     * Creates the tracer.
     *
     * @param tracing where the spans go
     */
    public JoinTracer(CygnusTracing tracing) {
        this.tracing = tracing;
    }

    /**
     * Opens the join span. A join still open for the same player, which means a connection that never
     * reached the disconnect, is ended as abandoned first.
     *
     * @param player the joining player
     */
    public void loginStarted(UUID player) {
        TraceStep previous = this.joins.put(player, this.tracing.root(CygnusAttributes.SPAN_PLAYER_JOIN)
                .set(CygnusAttributes.PLAYER_UUID, player.toString()));
        if (previous != null) {
            previous.set(CygnusAttributes.JOIN_OUTCOME, OUTCOME_ABANDONED).close();
        }
    }

    /**
     * Records that the player reached the configuration. Opens the span if the login was not seen.
     *
     * @param player the joining player
     */
    public void configurationStarted(UUID player) {
        TraceStep join = this.joins.get(player);
        if (join == null) {
            loginStarted(player);
            join = this.joins.get(player);
        }
        if (join != null) {
            join.span().addEvent(CygnusAttributes.EVENT_JOIN_CONFIGURATION);
        }
    }

    /**
     * Records what the client reported about the ResourcePack.
     *
     * @param player the joining player
     * @param status what the client reported
     */
    public void resourcePackStatus(UUID player, ResourcePackStatus status) {
        TraceStep join = this.joins.get(player);
        if (join != null) {
            join.span().addEvent(CygnusAttributes.EVENT_JOIN_RESOURCEPACK, Attributes.of(
                    CygnusAttributes.RESOURCEPACK_STATUS, status.name()));
        }
    }

    /**
     * Ends the join span at the first spawn.
     *
     * @param player the player who spawned
     */
    public void spawned(UUID player) {
        end(player, OUTCOME_SPAWNED);
    }

    /**
     * Ends the join span of a player who left before it ended. Does nothing for a player whose join
     * already finished, which is nearly every disconnect.
     *
     * @param player the player who left
     */
    public void disconnected(UUID player) {
        end(player, OUTCOME_ABANDONED);
    }

    private void end(UUID player, String outcome) {
        TraceStep join = this.joins.remove(player);
        if (join != null) {
            join.set(CygnusAttributes.JOIN_OUTCOME, outcome).close();
        }
    }

    /**
     * Hooks the join events onto this tracer.
     * <p>
     * Register it before the listeners that can turn a player away, so the span exists when the kick
     * ends the join.
     * </p>
     *
     * @param node the node to listen on
     */
    public void register(EventNode<? super Event> node) {
        node.addListener(AsyncPlayerPreLoginEvent.class, event -> loginStarted(event.getGameProfile().uuid()));
        node.addListener(AsyncPlayerConfigurationEvent.class, event -> configurationStarted(event.getPlayer().getUuid()));
        node.addListener(PlayerResourcePackStatusEvent.class,
                event -> resourcePackStatus(event.getPlayer().getUuid(), event.getStatus()));
        node.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                spawned(event.getPlayer().getUuid());
            }
        });
        node.addListener(PlayerDisconnectEvent.class, event -> disconnected(event.getPlayer().getUuid()));
    }
}
