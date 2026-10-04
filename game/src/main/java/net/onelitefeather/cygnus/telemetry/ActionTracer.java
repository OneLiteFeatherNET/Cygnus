package net.onelitefeather.cygnus.telemetry;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.common.page.event.PageExpiredEvent;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.common.page.event.PageSpawnedEvent;
import net.onelitefeather.cygnus.disclaimer.EpilepsyDisclaimer;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Turns the things that happen in a round into spans, so that "what happened, how and where" can be
 * asked of the traces.
 * <p>
 * <b>A span per action, not an event on the round span.</b> Each action is a short child of the
 * round span, named after its type ({@code cygnus.action.page.found}) and carrying the player and
 * the place as attributes. A trace backend can search spans by name and attribute, which answers
 * "every page found near this spot" or "everything player X did" directly; events on one long span
 * pile up on it, are capped per span by the SDK, and are far harder to filter on. The cost is one
 * span per action - a handful per minute, since movement and anything per tick are deliberately
 * <em>not</em> traced.
 * </p>
 * <p>
 * The actions are taken from events that already exist; the methods taking an {@link Actor} are what
 * the tests drive, {@link #register(EventNode, Supplier)} adapts the events onto them. A span is only
 * created while a round runs, otherwise the action has no trace to belong to.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class ActionTracer {

    private static final double ROUNDING = 10.0D;

    private final RoundTracer rounds;
    private final Clock clock;
    private final Supplier<@Nullable String> mapName;
    private final Map<UUID, Spawn> pages = new ConcurrentHashMap<>();
    // Players who already answered the disclaimer this round: any client can send the click packets
    // at any rate, so a second answer is not an action worth a span.
    private final Set<UUID> answered = ConcurrentHashMap.newKeySet();
    private final List<Runnable> resetListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates the tracer.
     *
     * @param rounds  owns the round the actions are children of
     * @param clock   dates the spawn of a page, to work out how long it was out
     * @param mapName supplies the name of the map being played, or {@code null} while none is loaded
     */
    public ActionTracer(RoundTracer rounds, Clock clock, Supplier<@Nullable String> mapName) {
        this.rounds = rounds;
        this.clock = clock;
        this.mapName = mapName;
        // Page ids and answers belong to one round; forgetting them keeps the maps bounded by a round.
        rounds.onBoundary(this::reset);
    }

    /**
     * Registers something that has to forget its per-round state along with this tracer.
     *
     * @param listener called when a round starts or ends
     */
    void onReset(Runnable listener) {
        this.resetListeners.add(listener);
    }

    private void reset() {
        this.pages.clear();
        this.answered.clear();
        for (Runnable listener : this.resetListeners) {
            listener.run();
        }
    }

    /**
     * Runs a listener body so that nothing it does can reach the game: a snapshot of a player that
     * is mid-disconnect, or anything else going wrong, only costs the span.
     */
    static void safely(Runnable body) {
        try {
            body.run();
        } catch (RuntimeException ignored) {
            // observing must not change what happens
        }
    }

    /**
     * Who did something, and where they were at that moment. A snapshot, so a span records the place
     * of the action and not wherever the player is by the time the span is built.
     *
     * @param uuid the player
     * @param role the role the player had, see {@link PlayerRoles}
     * @param x    the x coordinate
     * @param y    the y coordinate
     * @param z    the z coordinate
     */
    public record Actor(UUID uuid, String role, double x, double y, double z) {

        /**
         * Snapshots a player.
         *
         * @param player the player
         * @return who they are and where they stand right now
         */
        public static Actor of(Player player) {
            Pos position = player.getPosition();
            return new Actor(player.getUuid(), PlayerRoles.of(player), position.x(), position.y(), position.z());
        }

        double distanceTo(double otherX, double otherY, double otherZ) {
            return Math.sqrt(Math.pow(x - otherX, 2) + Math.pow(y - otherY, 2) + Math.pow(z - otherZ, 2));
        }
    }

    /**
     * A page appeared on a spot.
     *
     * @param pageId    the page
     * @param spot      where it stands
     * @param relocated whether an existing page was moved on, as opposed to the first placement
     */
    public void pageSpawned(UUID pageId, Pos spot, boolean relocated) {
        this.pages.put(pageId, new Spawn(this.clock.instant(), spot));
        emit(CygnusAttributes.ACTION_PAGE_SPAWN, null, step -> {
            step.set(CygnusAttributes.PAGE_ID, pageId.toString());
            step.set(CygnusAttributes.PAGE_RELOCATED, relocated);
            position(step, spot.x(), spot.y(), spot.z());
        });
    }

    /**
     * A survivor claimed a page.
     *
     * @param finder the survivor, where they stood
     * @param pageId the page, or {@code null} when it is not known
     * @param index  how many pages were found including this one
     * @param max    how many pages the round needs
     */
    public void pageFound(Actor finder, @Nullable UUID pageId, int index, int max) {
        Spawn spawn = pageId == null ? null : this.pages.get(pageId);
        emit(CygnusAttributes.ACTION_PAGE_FOUND, finder, step -> {
            step.set(CygnusAttributes.PAGE_INDEX, (long) index);
            step.set(CygnusAttributes.PAGES_MAX, (long) max);
            if (pageId != null) {
                step.set(CygnusAttributes.PAGE_ID, pageId.toString());
            }
            if (spawn != null) {
                step.set(CygnusAttributes.PAGE_OUT_MS, outMillis(spawn));
                step.set(CygnusAttributes.PAGE_SPOT_X, round(spawn.spot.x()));
                step.set(CygnusAttributes.PAGE_SPOT_Y, round(spawn.spot.y()));
                step.set(CygnusAttributes.PAGE_SPOT_Z, round(spawn.spot.z()));
                step.set(CygnusAttributes.DISTANCE,
                        round(finder.distanceTo(spawn.spot.x(), spawn.spot.y(), spawn.spot.z())));
            }
        });
    }

    /**
     * A page ran out of time before anyone found it.
     *
     * @param pageId the page
     */
    public void pageExpired(UUID pageId) {
        Spawn spawn = this.pages.get(pageId);
        emit(CygnusAttributes.ACTION_PAGE_EXPIRED, null, step -> {
            step.set(CygnusAttributes.PAGE_ID, pageId.toString());
            if (spawn != null) {
                step.set(CygnusAttributes.PAGE_OUT_MS, outMillis(spawn));
                position(step, spawn.spot.x(), spawn.spot.y(), spawn.spot.z());
            }
        });
    }

    /**
     * A player died. A survivor's death is attributed to the slender - the only source of damage -
     * with the distance between the two at that moment.
     *
     * @param victim  the player who died
     * @param slender the slender of the round, or {@code null} if there is none
     */
    public void playerDied(Actor victim, @Nullable Actor slender) {
        emit(CygnusAttributes.ACTION_PLAYER_DEATH, victim, step -> {
            if (slender != null && "survivor".equals(victim.role())) {
                step.set(CygnusAttributes.KILLER_UUID, slender.uuid().toString());
                step.set(CygnusAttributes.DISTANCE, round(victim.distanceTo(slender.x(), slender.y(), slender.z())));
            }
        });
    }

    /**
     * A survivor took over as the slender.
     *
     * @param player the new slender
     */
    public void slenderRevived(Actor player) {
        emit(CygnusAttributes.ACTION_SLENDER_REVIVE, player, step -> {
        });
    }

    /**
     * The slender's bar changed state, which is what using the slender's ability does.
     *
     * @param player the slender
     * @param state  the new state
     */
    public void staminaState(Actor player, String state) {
        emit(CygnusAttributes.ACTION_SLENDER_STAMINA, player, step -> step.set(CygnusAttributes.STAMINA_STATE, state));
    }

    /**
     * A player became a spectator.
     *
     * @param player the spectator
     */
    public void spectatorJoined(Actor player) {
        emit(CygnusAttributes.ACTION_SPECTATOR_JOIN, player, step -> {
        });
    }

    /**
     * A player answered the epilepsy disclaimer.
     *
     * @param player       the player
     * @param acknowledged {@code true} for taking note of it, {@code false} for declining
     */
    public void disclaimer(Actor player, boolean acknowledged) {
        if (!this.answered.add(player.uuid())) {
            return;
        }
        emit(acknowledged ? CygnusAttributes.ACTION_DISCLAIMER_ACKNOWLEDGE : CygnusAttributes.ACTION_DISCLAIMER_DECLINE,
                player, step -> {
                });
    }

    /**
     * The creek did something to a survivor.
     *
     * @param action one of the {@code ACTION_CREEK_} span names
     * @param player the survivor, where they stood
     */
    public void creek(String action, Actor player) {
        emit(action, player, step -> {
        });
    }

    /**
     * Hooks the game's events onto this tracer.
     * <p>
     * Register it <em>before</em> the listeners that act on the same events: the death listener strips
     * the team tag the role is read from, and the page listeners move the page the position is read from.
     * </p>
     *
     * @param node    the node to listen on
     * @param slender supplies the slender of the round, or {@code null} if there is none
     */
    public void register(EventNode<? super Event> node, Supplier<@Nullable Player> slender) {
        node.addListener(PageSpawnedEvent.class,
                event -> safely(() -> pageSpawned(event.pageId(), event.position(), event.relocated())));
        node.addListener(PageFoundEvent.class, event -> safely(() ->
                pageFound(Actor.of(event.finder()), event.pageId(), event.foundCount(), event.maxPages())));
        node.addListener(PageExpiredEvent.class, event -> safely(() -> pageExpired(event.entity().getHitBoxUUID())));
        node.addListener(PlayerDeathEvent.class, event -> safely(() -> {
            Player slenderPlayer = slender.get();
            playerDied(Actor.of(event.getPlayer()), slenderPlayer == null ? null : Actor.of(slenderPlayer));
        }));
        node.addListener(SlenderReviveEvent.class, event -> safely(() -> slenderRevived(Actor.of(event.getPlayer()))));
        node.addListener(StaminaStateChangeEvent.class,
                event -> safely(() -> staminaState(Actor.of(event.getPlayer()), event.getState().name())));
        node.addListener(SpectatorAddEvent.class, event -> safely(() -> spectatorJoined(Actor.of(event.getPlayer()))));
        node.addListener(PlayerCustomClickEvent.class, event -> safely(() -> {
            if (EpilepsyDisclaimer.ACKNOWLEDGE_KEY.equals(event.getKey())) {
                disclaimer(Actor.of(event.getPlayer()), true);
            } else if (EpilepsyDisclaimer.DECLINE_KEY.equals(event.getKey())) {
                disclaimer(Actor.of(event.getPlayer()), false);
            }
        }));
    }

    private void emit(String name, @Nullable Actor actor, Consumer<TraceStep> details) {
        TraceStep step = this.rounds.actionSpan(name);
        if (step == null) {
            return;
        }
        try {
            if (actor != null) {
                step.set(CygnusAttributes.PLAYER_UUID, actor.uuid().toString());
                step.set(CygnusAttributes.PLAYER_ROLE, actor.role());
                position(step, actor.x(), actor.y(), actor.z());
            }
            String map = this.mapName.get();
            if (map != null) {
                step.set(CygnusAttributes.MAP, map);
            }
            details.accept(step);
        } catch (RuntimeException exception) {
            step.fail(exception);
        } finally {
            step.close();
        }
    }

    private long outMillis(Spawn spawn) {
        return Duration.between(spawn.at, this.clock.instant()).toMillis();
    }

    private static void position(TraceStep step, double x, double y, double z) {
        step.set(CygnusAttributes.POSITION_X, round(x));
        step.set(CygnusAttributes.POSITION_Y, round(y));
        step.set(CygnusAttributes.POSITION_Z, round(z));
    }

    /** Rounded to 0.1 block: precise enough to find a spot, coarse enough to group by. */
    private static double round(double value) {
        return Math.round(value * ROUNDING) / ROUNDING;
    }

    private record Spawn(Instant at, Pos spot) {
    }
}
