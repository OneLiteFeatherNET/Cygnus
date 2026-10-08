package net.onelitefeather.cygnus.camera;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Lets the resource pack play its "wake-up eyes" post effect when a round starts.
 * <p>
 * Vanilla clients only apply the {@code minecraft:post_effect/spider.json} effect while their camera spectates a
 * spider. For {@link #DURATION_TICKS} ticks after the camera switch, the player's camera is pointed at an invisible
 * spider that exists only for that player and sits at the player's eye position with the player's view.
 * The game mode is never changed: spectating only switches the camera.
 * </p>
 * <p>
 * The transition is started with the game instance the players are teleported to, not with the instance they stand
 * in now. Minestom applies {@code Player#setInstance} only after the target chunks loaded, so right after the
 * teleport call the player is still in the lobby. The spider is therefore spawned in the target instance only once
 * the player has actually arrived there ({@code player.getInstance() == target}), at the player's position at that
 * moment. Until then the transition waits, and leaving the lobby does not end it.
 * </p>
 * <p>
 * The camera is not switched when the transition starts. The client must first know the spider: the player must be
 * in the spider's instance, the spider must have been spawned and must be a viewer of the player. The switch then
 * waits {@link #CAMERA_DELAY_TICKS} more ticks so that the spider's spawn packets are sent before the camera packet.
 * If the transition is still not switched after {@link #PENDING_TIMEOUT_TICKS} ticks counted from its start, it is
 * abandoned. The {@link #DURATION_TICKS} are counted from the moment the camera is actually switched.
 * </p>
 * <p>
 * The transition ends early, with the spider removed and no error, when the player disconnects, or when the player
 * leaves the target instance after having arrived there. The end of the waiting phase does not end it. When the
 * round finishes ({@link GameFinishEvent}), every running transition is cancelled.
 * </p>
 * <p>
 * Usage:
 * </p>
 * <pre>{@code
 * WakeUpTransition transition = new WakeUpTransition();
 * transition.register(MinecraftServer.getGlobalEventHandler());
 * transition.start(players, gameInstance); // before or after the teleport
 * }</pre>
 *
 * @version 1.2.0
 * @since 2.16.0
 */
public final class WakeUpTransition {

    private static final Logger LOGGER = LoggerFactory.getLogger(WakeUpTransition.class);

    /**
     * How long the camera stays on the spider after the switch, in server ticks.
     */
    public static final int DURATION_TICKS = 100;

    /**
     * How many ticks after the spider became viewable for the player the camera waits before it switches, so the
     * spider's spawn packets reach the client before the camera packet.
     */
    public static final int CAMERA_DELAY_TICKS = 2;

    /**
     * How long a transition may wait, counted from its start, for the player to arrive in the target instance and
     * for the spider to become viewable, in server ticks. The lobby-to-game switch is a deferred
     * {@code Player#setInstance} that loads the game map's chunks and respawns the player, which takes well over the
     * 2 seconds (40 ticks) the first version allowed. 100 ticks (5 seconds) leaves room for that without keeping a
     * transition that cannot succeed running for long.
     */
    public static final int PENDING_TIMEOUT_TICKS = 100;

    private final Map<UUID, Transition> transitions = new HashMap<>();

    /**
     * Registers the listeners which end a transition early, and the cleanup when the round finishes.
     *
     * @param node the node to listen on; the global event handler in production
     */
    public void register(EventNode<Event> node) {
        node.addListener(GameFinishEvent.class, event -> cancelAll("game finished"));
        node.addListener(PlayerDisconnectEvent.class, event -> cancel(event.getPlayer(), "player disconnected"));
        node.addListener(RemoveEntityFromInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;
            Transition transition = transitions.get(player.getUuid());
            // Only the target instance counts: the lobby the player leaves on the way there is not the spider's place
            if (transition != null && transition.arrived && transition.instance == event.getInstance()) {
                cancel(player, "player left the instance");
            }
        });
    }

    /**
     * Starts the transition for every given player, see {@link #start(Player, Instance)}.
     *
     * @param players the players that are being moved into the game map
     * @param target  the game instance the players are moved into
     */
    public void start(Iterable<Player> players, Instance target) {
        for (Player player : players) {
            start(player, target);
        }
    }

    /**
     * Starts the transition for one player. A transition that is already running for the player is replaced.
     * The player does not have to be in the target yet: the spider is spawned once the player arrives there, at the
     * player's position at that moment. The camera switches once the spider is viewable for the player, see the class
     * documentation.
     *
     * @param player the player to point the camera from
     * @param target the instance the player is moved into, which is where the spider lives
     */
    public void start(Player player, Instance target) {
        Objects.requireNonNull(target, "target");
        cancel(player, "replaced by a new transition");

        Transition transition = new Transition(player, target);
        transitions.put(player.getUuid(), transition);
        if (transition.isAtTarget()) {
            spawnSpider(transition);
        } else {
            LOGGER.info("wake-up transition: waiting for {} to arrive in the game instance", player.getUsername());
        }
        transition.task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> tick(player.getUuid()))
                .repeat(TaskSchedule.tick(1))
                .schedule();
    }

    /**
     * Spawns the spider in the target instance at the player's current eye position and view. Runs once per
     * transition, when the player is in the target instance for the first time.
     */
    private static void spawnSpider(Transition transition) {
        Player player = transition.player;
        LOGGER.info("wake-up transition: {} arrived, spawning spider", player.getUsername());

        WakeUpSpider spider = new WakeUpSpider();
        Pos eye = player.getPosition();
        double spiderY = eye.y() + player.getEyeHeight() - spider.getEyeHeight();
        Pos spawn = eye.withY(spiderY);
        CompletableFuture<Void> spawned = Objects.requireNonNullElseGet(
                spider.setInstance(transition.instance, spawn), () -> CompletableFuture.completedFuture(null));
        spider.setView(spawn.yaw(), spawn.pitch(), spawn.yaw());
        spider.updateViewableRule(viewer -> viewer.getUuid().equals(player.getUuid()));
        LOGGER.info("wake-up transition: spider spawned for {}", player.getUsername());

        transition.spider = spider;
        transition.spawned = spawned;
        transition.arrived = true;
    }

    /**
     * Ends the transition for one player: the spider is removed and the scheduled checks are cancelled. If the
     * camera was already on the spider, it returns to the player. Does nothing if no transition is running for the
     * player.
     *
     * @param player the player whose transition should end
     */
    public void cancel(Player player) {
        cancel(player, "cancelled");
    }

    /**
     * Ends every running transition, see {@link #cancel(Player)}.
     */
    public void cancelAll() {
        cancelAll("cancelled");
    }

    /**
     * Checks whether a transition is running for the given player, pending or with the camera on the spider.
     *
     * @param player the player to check
     * @return {@code true} if a transition is running for this player
     */
    public boolean isRunning(Player player) {
        return transitions.containsKey(player.getUuid());
    }

    private void cancelAll(String reason) {
        for (Transition transition : new ArrayList<>(transitions.values())) {
            cancel(transition.player, reason);
        }
    }

    private void cancel(Player player, String reason) {
        Transition transition = transitions.remove(player.getUuid());
        if (transition == null) return;
        transition.task.cancel();
        if (transition.onSpider) {
            if (transition.player.isOnline()) {
                transition.player.stopSpectating();
            }
            LOGGER.info("wake-up transition: camera returned for {} ({})", transition.player.getUsername(), reason);
        } else {
            LOGGER.info("wake-up transition: abandoned for {} ({})", transition.player.getUsername(), reason);
        }
        if (transition.spider != null) {
            transition.spider.remove();
        }
    }

    /**
     * Runs once per server tick for one transition: waits for the spider to be viewable, switches the camera after
     * the delay, or counts the {@link #DURATION_TICKS} while the camera is on the spider.
     */
    private void tick(UUID playerId) {
        Transition transition = transitions.get(playerId);
        if (transition == null) return;

        if (transition.onSpider) {
            transition.ticksOnSpider++;
            if (transition.ticksOnSpider >= DURATION_TICKS) {
                cancel(transition.player, DURATION_TICKS + " ticks elapsed");
            }
            return;
        }

        transition.pendingTicks++;
        if (!transition.arrived && transition.isAtTarget()) {
            spawnSpider(transition);
        }
        if (!transition.isViewable()) {
            transition.viewableSince = -1;
            if (transition.pendingTicks >= PENDING_TIMEOUT_TICKS) {
                String reason = transition.arrived
                        ? "spider not viewable after " + PENDING_TIMEOUT_TICKS + " ticks"
                        : "player did not arrive in the game instance after " + PENDING_TIMEOUT_TICKS + " ticks";
                cancel(transition.player, reason);
            }
            return;
        }
        if (transition.viewableSince < 0) {
            transition.viewableSince = transition.pendingTicks;
        }
        if (transition.pendingTicks - transition.viewableSince >= CAMERA_DELAY_TICKS) {
            switchCamera(transition);
        }
    }

    private void switchCamera(Transition transition) {
        transition.player.spectate(transition.spider);
        transition.onSpider = true;
        transition.ticksOnSpider = 0;
        LOGGER.info("wake-up transition: camera switched to spider for {}", transition.player.getUsername());
    }

    private static final class Transition {

        private final Player player;
        private final Instance instance;
        private Task task;
        /** Set once the spider was spawned, which happens on the player's arrival in {@link #instance}. */
        private boolean arrived;
        private WakeUpSpider spider;
        private CompletableFuture<Void> spawned;
        private boolean onSpider;
        private int pendingTicks;
        private int viewableSince = -1;
        private int ticksOnSpider;

        private Transition(Player player, Instance instance) {
            this.player = player;
            this.instance = instance;
        }

        /**
         * Whether the player is in the target instance right now. Minestom sets the player's instance only when the
         * deferred switch has completed, so this is the arrival check.
         */
        private boolean isAtTarget() {
            return player.getInstance() == instance;
        }

        /**
         * The client can know the spider only when the spider was spawned, the player is in the spider's instance,
         * the spider's spawn is complete and the spider is a viewer of the player.
         */
        private boolean isViewable() {
            return arrived
                    && player.getInstance() == instance
                    && spawned.isDone()
                    && !spider.isRemoved()
                    && spider.getViewers().contains(player);
        }
    }

    /**
     * The invisible spider the camera looks through. It is a plain entity, so it has no AI, and it never
     * collides with other entities, so nobody can push it out of the player's view.
     */
    private static final class WakeUpSpider extends Entity {

        WakeUpSpider() {
            super(EntityType.SPIDER);
            setInvisible(true);
            setSilent(true);
            setNoGravity(true);
        }

        @Override
        public boolean hasEntityCollision() {
            return false;
        }
    }
}
