package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.Attributes;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.team.TeamHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
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
    // The span API cannot be read back, so whether gameFinished already set the reason is kept here.
    private boolean endReasonSet;

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
        this.round = this.tracing.root(CygnusAttributes.SPAN_ROUND)
                .set(CygnusAttributes.ROUND_ID, this.roundIds.get());
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
        this.phase = this.round.child(CygnusAttributes.SPAN_PHASE_PREFIX + phaseName.toLowerCase(Locale.ROOT))
                .set(CygnusAttributes.PHASE_NAME, phaseName);
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
        if (current != null) {
            current.close();
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
                event -> playerDied(event.getPlayer().getUuid(), roleOf(event.getPlayer())));
    }

    private static String roleOf(Player player) {
        if (TeamHelper.isSlenderTeam(player)) {
            return "slender";
        }
        if (TeamHelper.isSurvivorTeam(player)) {
            return "survivor";
        }
        return TeamHelper.isSpectatorTeam(player) ? "spectator" : "none";
    }

    private void addEvent(String name, Attributes attributes) {
        TraceStep current = this.round;
        if (current != null) {
            current.span().addEvent(name, attributes);
        }
    }

    private void endPhase() {
        TraceStep current = this.phase;
        this.phase = null;
        if (current != null) {
            current.close();
        }
    }
}
