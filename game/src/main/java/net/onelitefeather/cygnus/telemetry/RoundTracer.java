package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Owns the trace of a round: one root span for the round, one child per phase, and span events for
 * what happens in between.
 * <p>
 * A round is not a single call stack - it lives from the lobby opening to the restart ending and is
 * driven by the tick thread, so the spans are started and ended by lifecycle hooks
 * ({@link TracedPhaseSeries}) instead of a surrounding {@code try}. The events are added to the round
 * span rather than made spans of their own: they are points in time, and a round has few of them.
 * </p>
 * <p>
 * The methods that take plain values are what the tests drive; {@link #register(EventNode)} adapts the
 * game's events onto them. All methods are safe to call with no round running, in which case the
 * event is dropped - a page can be found, in theory, between two rounds.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class RoundTracer {

    private final CygnusTracing tracing;
    private final Supplier<String> roundIds;
    private @Nullable TraceStep round;
    private @Nullable TraceStep phase;
    // Snapshot read without the lock by other threads: the phase span while one runs, the round span
    // between phases, null with no round. Written only under the lock, on every change.
    private volatile @Nullable Context current;
    private volatile @Nullable SpanContext latestRound;
    private @Nullable SpanContext startup;
    // The span API cannot be read back, so whether gameFinished already set the reason is kept here.
    private boolean endReasonSet;
    /** More incoming traces than this are not linked: a cookie is client data, the links must stay bounded. */
    static final int MAX_LINKS = 16;

    private final Set<String> linked = new HashSet<>();
    private int droppedLinks;
    private final List<Runnable> boundaryListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates the tracer.
     *
     * @param tracing  where the spans go
     * @param roundIds hands out the id of a new round
     */
    public RoundTracer(CygnusTracing tracing, Supplier<String> roundIds) {
        this.tracing = tracing;
        this.roundIds = roundIds;
    }

    /**
     * Creates the tracer with a random UUID as the round id.
     *
     * @param tracing where the spans go
     */
    public RoundTracer(CygnusTracing tracing) {
        this(tracing, () -> UUID.randomUUID().toString());
    }

    /**
     * Opens the round span. A round still open from before is aborted first, so a span can never be
     * left behind by a series that was restarted.
     */
    synchronized void roundStarted() {
        abort("restarted");
        this.endReasonSet = false;
        this.linked.clear();
        this.droppedLinks = 0;
        notifyBoundary();
        TraceStep started = this.tracing.root(CygnusAttributes.SPAN_ROUND)
                .set(CygnusAttributes.ROUND_ID, this.roundIds.get());
        this.round = started;
        this.latestRound = started.span().getSpanContext();
        this.current = Context.root().with(started.span());
        SpanContext wiredBy = this.startup;
        if (wiredBy != null) {
            this.startup = null;
            started.span().addLink(wiredBy, Attributes.of(CygnusAttributes.LINK_KIND, CygnusAttributes.LINK_STARTUP));
        }
    }

    /**
     * Remembers the startup span, which the first round links to: the round is started by the startup,
     * but the startup ends before it, so it cannot be its parent.
     *
     * @param context the context of the startup span
     */
    public synchronized void startupContext(SpanContext context) {
        this.startup = context;
    }

    /**
     * Returns the context new work should hang below: the running phase, the round between two
     * phases, or {@code null} when no round runs. Cheap and safe from any thread; a span created
     * from it may outlive the phase, which tracing allows.
     *
     * @return the context, or {@code null}
     */
    @Nullable Context currentContext() {
        return this.current;
    }

    /**
     * Returns the context of the running round, or of the last one when none runs; for a span that
     * only links to it.
     *
     * @return the context, or {@code null} if there was no round yet
     */
    @Nullable SpanContext latestRoundContext() {
        return this.latestRound;
    }

    /**
     * Opens the span of a phase below the round. Ends the previous phase span if it is still open.
     *
     * @param phaseName the name of the phase that starts
     */
    synchronized void phaseStarted(String phaseName) {
        endPhase();
        if (this.round == null) {
            return;
        }
        TraceStep started = this.round.child(CygnusAttributes.SPAN_PHASE_PREFIX + phaseName.toLowerCase(Locale.ROOT))
                .set(CygnusAttributes.PHASE_NAME, phaseName);
        this.phase = started;
        this.current = Context.root().with(started.span());
    }

    /**
     * Ends the span of the running phase, if there is one.
     */
    synchronized void phaseEnded() {
        endPhase();
    }

    /**
     * Records that the running phase threw, and ends its span.
     *
     * @param throwable what the phase threw
     */
    synchronized void phaseFailed(Throwable throwable) {
        TraceStep current = this.phase;
        if (current != null) {
            current.fail(throwable);
        }
        endPhase();
    }

    /**
     * Ends the round span after its last phase.
     */
    synchronized void roundEnded() {
        endPhase();
        TraceStep current = this.round;
        this.round = null;
        this.current = null;
        if (current != null) {
            if (this.droppedLinks > 0) {
                current.set(CygnusAttributes.LINKS_DROPPED, (long) this.droppedLinks);
            }
            current.close();
        }
        notifyBoundary();
    }

    /**
     * Registers something that has to forget its per-round state when a round starts or ends.
     *
     * @param listener called on every boundary, on the thread that starts or ends the round; must be quick
     */
    void onBoundary(Runnable listener) {
        this.boundaryListeners.add(listener);
    }

    private void notifyBoundary() {
        for (Runnable listener : this.boundaryListeners) {
            listener.run();
        }
    }

    /**
     * Ends whatever is open because the round did not reach its end, a shutdown in the middle of a
     * round being the usual cause. An end reason set by {@link #gameFinished(String)} is kept.
     * Does nothing when no round is open.
     *
     * @param reason why the round ends early
     */
    public synchronized void abort(String reason) {
        TraceStep current = this.round;
        if (current == null) {
            return;
        }
        if (!this.endReasonSet) {
            current.set(CygnusAttributes.GAME_END_REASON, reason);
        }
        roundEnded();
    }

    /**
     * Adds the start of the game to the round.
     */
    public synchronized void gameStarted() {
        addEvent(CygnusAttributes.EVENT_GAME_START, Attributes.empty());
    }

    /**
     * Adds a found page to the round, with how far the round is.
     *
     * @param finder the survivor who found it
     * @param found  how many pages were found including this one
     * @param max    how many pages the round needs
     */
    public synchronized void pageFound(UUID finder, int found, int max) {
        addEvent(CygnusAttributes.EVENT_PAGE_FOUND, Attributes.builder()
                .put(CygnusAttributes.PLAYER_UUID, finder.toString())
                .put(CygnusAttributes.PAGES_FOUND, (long) found)
                .put(CygnusAttributes.PAGES_MAX, (long) max)
                .build());
    }

    /**
     * Adds a death to the round.
     *
     * @param player the player who died
     * @param role   the role the player had at that moment
     */
    public synchronized void playerDied(UUID player, String role) {
        addEvent(CygnusAttributes.EVENT_PLAYER_DEATH, Attributes.builder()
                .put(CygnusAttributes.PLAYER_UUID, player.toString())
                .put(CygnusAttributes.PLAYER_ROLE, role)
                .build());
    }

    /**
     * Adds the revive of the slender to the round.
     *
     * @param player the player who became the slender
     */
    public synchronized void slenderRevived(UUID player) {
        addEvent(CygnusAttributes.EVENT_SLENDER_REVIVE, Attributes.builder()
                .put(CygnusAttributes.PLAYER_UUID, player.toString())
                .build());
    }

    /**
     * Adds the end of the game to the round and keeps its reason on the round span itself, so a
     * trace search can filter on it.
     *
     * @param reason why the game ended
     */
    public synchronized void gameFinished(String reason) {
        TraceStep current = this.round;
        if (current == null) {
            return;
        }
        current.set(CygnusAttributes.GAME_END_REASON, reason);
        this.endReasonSet = true;
        addEvent(CygnusAttributes.EVENT_GAME_FINISH, Attributes.builder()
                .put(CygnusAttributes.GAME_END_REASON, reason)
                .build());
    }

    /**
     * Starts a span below the running phase (the round between two phases), for a single action.
     *
     * @param name the span name
     * @return the open span, or {@code null} when no round is running, in which case the action is
     * not traced
     */
    @Nullable TraceStep actionSpan(String name) {
        Context parent = this.current;
        return parent == null ? null : this.tracing.step(name, parent);
    }

    /**
     * Returns the span context of the running round, for writing it to the players' cookie.
     *
     * @return the context, or {@code null} when no round is running
     */
    synchronized @Nullable SpanContext roundContext() {
        TraceStep current = this.round;
        return current == null ? null : current.span().getSpanContext();
    }

    /**
     * Links the running round to the round trace a joining player's cookie remembered. One link per
     * distinct trace id: a lobby full of players who all come from the same round adds one, not
     * thirteen. At most {@value #MAX_LINKS} per round; the rest are counted in
     * {@code cygnus.links.dropped}.
     *
     * @param previous the span context read from a cookie
     */
    synchronized void linkPrevious(SpanContext previous) {
        TraceStep current = this.round;
        if (current == null || current.span().getSpanContext().getTraceId().equals(previous.getTraceId())
                || this.linked.contains(previous.getTraceId())) {
            return;
        }
        if (this.linked.size() >= MAX_LINKS) {
            this.droppedLinks++;
            return;
        }
        this.linked.add(previous.getTraceId());
        current.span().addLink(previous, Attributes.of(CygnusAttributes.LINK_KIND, CygnusAttributes.LINK_PREVIOUS_ROUND));
    }

    /**
     * Hooks the game's events onto this tracer.
     * <p>
     * Register it <em>before</em> the listener that handles the same event: the death listener strips
     * the player's team tag, so the role is only readable if this one runs first.
     * </p>
     *
     * @param node the node to listen on
     */
    public void register(EventNode<? super Event> node) {
        node.addListener(GameStartEvent.class, _ -> gameStarted());
        node.addListener(GameFinishEvent.class, event -> gameFinished(event.reason().name()));
        node.addListener(PageFoundEvent.class,
                event -> pageFound(event.finder().getUuid(), event.foundCount(), event.maxPages()));
        node.addListener(SlenderReviveEvent.class, event -> slenderRevived(event.getPlayer().getUuid()));
        node.addListener(PlayerDeathEvent.class,
                event -> playerDied(event.getPlayer().getUuid(), PlayerRoles.of(event.getPlayer())));
    }

    private void addEvent(String name, Attributes attributes) {
        TraceStep current = this.round;
        if (current != null) {
            current.span().addEvent(name, attributes);
        }
    }

    private void endPhase() {
        TraceStep ended = this.phase;
        this.phase = null;
        TraceStep running = this.round;
        // Back to the round first, so nothing created from now on attaches to the closed phase.
        this.current = running == null ? null : Context.root().with(running.span());
        if (ended != null) {
            ended.close();
        }
    }
}
