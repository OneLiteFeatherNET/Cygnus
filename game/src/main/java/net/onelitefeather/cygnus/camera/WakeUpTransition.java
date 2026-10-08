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
 * The camera is not switched when {@link #start(Player)} is called. The client must first know the spider: the
 * player must be in the spider's instance, the spider must have been spawned and must be a viewer of the player.
 * The switch then waits {@link #CAMERA_DELAY_TICKS} more ticks so that the spider's spawn packets are sent before
 * the camera packet. If the spider is still not viewable after {@link #PENDING_TIMEOUT_TICKS} ticks, the transition
 * is abandoned. The {@link #DURATION_TICKS} are counted from the moment the camera is actually switched.
 * </p>
 * <p>
 * The transition ends early, with the spider removed and no error, when the player disconnects or leaves the
 * instance the spider lives in. The end of the waiting phase does not end it. When the round finishes
 * ({@link GameFinishEvent}), every running transition is cancelled.
 * </p>
 * <p>
 * Usage:
 * </p>
 * <pre>{@code
 * WakeUpTransition transition = new WakeUpTransition();
 * transition.register(MinecraftServer.getGlobalEventHandler());
 * transition.start(players);
 * }</pre>
 *
 * @version 1.1.0
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
     * How long a transition may wait for the spider to become viewable, in server ticks.
     */
    public static final int PENDING_TIMEOUT_TICKS = 40;

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
            if (transition != null && transition.instance == event.getInstance()) {
                cancel(player, "player left the instance");
            }
        });
    }

    /**
     * Starts the transition for every given player who is currently in an instance.
     *
     * @param players the players that just entered the game map
     */
    public void start(Iterable<Player> players) {
        for (Player player : players) {
            start(player);
        }
    }

    /**
     * Starts the transition for one player. A transition that is already running for the player is replaced.
     * Does nothing if the player is not in an instance. The camera switches once the spider is viewable for the
     * player, see the class documentation.
     *
     * @param player the player to point the camera from
     */
    public void start(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) return;
        cancel(player, "replaced by a new transition");

        WakeUpSpider spider = new WakeUpSpider();
        Pos eye = player.getPosition();
        double spiderY = eye.y() + player.getEyeHeight() - spider.getEyeHeight();
        Pos spawn = eye.withY(spiderY);
        CompletableFuture<Void> spawned = Objects.requireNonNullElseGet(
                spider.setInstance(instance, spawn), () -> CompletableFuture.completedFuture(null));
        spider.setView(spawn.yaw(), spawn.pitch(), spawn.yaw());
        spider.updateViewableRule(viewer -> viewer.getUuid().equals(player.getUuid()));
        LOGGER.info("wake-up transition: spider spawned for {}", player.getUsername());

        Transition transition = new Transition(player, instance, spider, spawned);
        transition.task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> tick(player.getUuid()))
                .repeat(TaskSchedule.tick(1))
                .schedule();
        transitions.put(player.getUuid(), transition);
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
        transition.spider.remove();
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
        if (!transition.isViewable()) {
            transition.viewableSince = -1;
            if (transition.pendingTicks >= PENDING_TIMEOUT_TICKS) {
                cancel(transition.player, "spider not viewable after " + PENDING_TIMEOUT_TICKS + " ticks");
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
        private final Entity spider;
        private final CompletableFuture<Void> spawned;
        private Task task;
        private boolean onSpider;
        private int pendingTicks;
        private int viewableSince = -1;
        private int ticksOnSpider;

        private Transition(Player player, Instance instance, Entity spider, CompletableFuture<Void> spawned) {
            this.player = player;
            this.instance = instance;
            this.spider = spider;
            this.spawned = spawned;
        }

        /**
         * The client can know the spider only when the player is in the spider's instance, the spider's spawn is
         * complete and the spider is a viewer of the player.
         */
        private boolean isViewable() {
            return player.getInstance() == instance
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
