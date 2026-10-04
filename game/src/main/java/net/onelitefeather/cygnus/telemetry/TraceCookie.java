package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.network.player.PlayerConnection;
import net.onelitefeather.cygnus.event.GameStartEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Carries the round's trace in a client cookie, so the trace of the next thing a player does - in
 * this service or after a switch to another backend - can point back at the round they played.
 * <p>
 * At the start of a round the round span's W3C {@code traceparent} is stored in the cookie
 * {@value #KEY} of every player. When a player joins, the cookie is requested back; if it holds a
 * valid {@code traceparent}, the join span and the running round get a <em>link</em> to it. A link,
 * not a parent: the two traces stay separate and stay complete, and a trace backend lets one be
 * followed into the other.
 * </p>
 * <p>
 * <b>The cookie is client-supplied, so it is only ever parsed.</b> It is capped at
 * {@value #MAX_BYTES} bytes, read as ASCII, handed to the W3C parser, and used only if that yields a
 * valid span context. Nothing else in it is read, logged or put into a span. The fetch never blocks
 * the configuration thread: the answer is handled when it arrives, and a client that does not answer
 * within the timeout is treated as having no cookie.
 * </p>
 * <p>
 * A cookie lives on the client. It reaches another backend only as long as the client's connection to
 * the proxy persists (Velocity keeps the connection when it moves a player between backends); once the
 * player disconnects from the network, it is gone with the session.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TraceCookie {

    /** The cookie's key. */
    public static final String KEY = "onelitefeather:trace";

    /** A traceparent is 55 bytes; this leaves room and rejects anything that is clearly not one. */
    static final int MAX_BYTES = 128;

    /** How long a join waits for the client to answer the cookie request. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceCookie.class);
    private static final W3CTraceContextPropagator PROPAGATOR = W3CTraceContextPropagator.getInstance();
    private static final String TRACEPARENT = "traceparent";

    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public @Nullable String get(@Nullable Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };
    private static final TextMapSetter<Map<String, String>> SETTER = Map::put;

    private final RoundTracer rounds;
    private final JoinTracer joins;

    /**
     * Creates the cookie handling.
     *
     * @param rounds owns the round whose context is written, and which is linked
     * @param joins  owns the join spans that are linked
     */
    public TraceCookie(RoundTracer rounds, JoinTracer joins) {
        this.rounds = rounds;
        this.joins = joins;
    }

    /**
     * Returns the bytes to store for the running round.
     *
     * @return the cookie value, or {@code null} when no round is running
     */
    @Nullable byte[] roundCookie() {
        SpanContext context = this.rounds.roundContext();
        if (context == null || !context.isValid()) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        PROPAGATOR.inject(Context.root().with(Span.wrap(context)), carrier, SETTER);
        String traceparent = carrier.get(TRACEPARENT);
        return traceparent == null ? null : traceparent.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Reads a cookie a client handed back.
     *
     * @param data the cookie value, {@code null} if the client had none
     * @return the remote span context it holds, or {@code null} if it is absent or not a valid
     * {@code traceparent}
     */
    static @Nullable SpanContext decode(byte @Nullable [] data) {
        if (data == null || data.length == 0 || data.length > MAX_BYTES) {
            return null;
        }
        Map<String, String> carrier = Map.of(TRACEPARENT, new String(data, StandardCharsets.US_ASCII));
        SpanContext context = Span.fromContext(PROPAGATOR.extract(Context.root(), carrier, GETTER)).getSpanContext();
        return context.isValid() ? context : null;
    }

    /**
     * Handles the cookie of a player who is joining: when it arrives and holds a valid context, links
     * the join and the running round to it. Never throws and never waits.
     *
     * @param player     the joining player
     * @param connection the connection of the join
     * @param cookie     the pending answer of the client
     */
    void joined(UUID player, Object connection, CompletableFuture<byte @Nullable []> cookie) {
        cookie.whenComplete((data, failure) -> {
            if (failure != null) {
                LOGGER.debug("The trace cookie of a joining player could not be read", failure);
                return;
            }
            SpanContext previous = decode(data);
            if (previous != null) {
                this.joins.link(player, connection, previous);
                this.rounds.linkPrevious(previous);
            }
        });
    }

    /**
     * Stores the round's context in the cookie of every online player. Does nothing if no round runs.
     */
    void storeForOnlinePlayers() {
        byte[] value = roundCookie();
        if (value == null) {
            return;
        }
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            try {
                player.getPlayerConnection().storeCookie(KEY, value);
            } catch (RuntimeException exception) {
                LOGGER.debug("Could not store the trace cookie for a player", exception);
            }
        }
    }

    /**
     * Hooks the cookie onto the game: written when the game starts, read when a player joins.
     *
     * @param node    the node to listen on
     * @param timeout how long to wait for a client's answer
     */
    public void register(EventNode<? super Event> node, Duration timeout) {
        node.addListener(GameStartEvent.class, _ -> storeForOnlinePlayers());
        node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            PlayerConnection connection = event.getPlayer().getPlayerConnection();
            try {
                // completeOnTimeout turns a client that never answers into "no cookie"; Minestom
                // itself waits for ever and keeps the pending request.
                CompletableFuture<byte @Nullable []> answer = connection.fetchCookie(KEY)
                        .completeOnTimeout(null, timeout.toMillis(), TimeUnit.MILLISECONDS);
                joined(event.getPlayer().getUuid(), connection, answer);
            } catch (RuntimeException exception) {
                LOGGER.debug("Could not request the trace cookie of a joining player", exception);
            }
        });
    }
}
