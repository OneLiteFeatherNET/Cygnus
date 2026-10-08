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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Lets the resource pack play its "wake-up eyes" post effect in the lobby and in the game.
 * <p>
 * Vanilla clients only apply the {@code minecraft:post_effect/spider.json} effect while their camera spectates a
 * spider. A transition points the player's camera at an invisible spider that exists only for that player and sits
 * at the player's eye position with the player's view. The game mode is never changed: spectating only switches the
 * camera. There are two kinds of transition, see {@link Kind}:
 * </p>
 * <ul>
 *     <li>{@link Kind#CLOSE} starts in the lobby, when the waiting phase starts. The eyes close there. The camera has
 *     no duration: it stays on the lobby spider until the player leaves the lobby for the game.</li>
 *     <li>{@link Kind#OPEN} starts with the game instance the players are teleported to. The eyes open there. The camera
 *     returns after {@link #DURATION_TICKS} ticks counted from the switch.</li>
 * </ul>
 * <p>
 * The resource pack picks the animation from the client's world age, see {@link #worldAgeFor(Kind, long)}. Right
 * before the first camera switch of a batch (the players started by one {@link #close} or {@link #open} call) in an
 * instance, the world age of that instance is moved into the band of the kind. The batch sets it only once per
 * instance, so later players of the batch do not restart the others' animation. Minestom sends the new world age to
 * the instance's players at once when it is set.
 * </p>
 * <p>
 * A transition of the game kind waits for the player's arrival in the game instance. Minestom applies
 * {@code Player#setInstance} only after the target chunks loaded, so right after the teleport call the player is still
 * in the lobby. The spider is therefore spawned in the target instance only once the player has actually arrived there
 * ({@code player.getInstance() == target}), at the player's position at that moment. Until then the transition waits,
 * and leaving the lobby does not end it.
 * </p>
 * <p>
 * The camera is not switched when the transition starts. The client must first know the spider: the player must be
 * in the spider's instance, the spider must have been spawned and must be a viewer of the player. The switch then
 * waits {@link #CAMERA_DELAY_TICKS} more ticks so that the spider's spawn packets are sent before the camera packet.
 * If the transition is still not switched after {@link #PENDING_TIMEOUT_TICKS} ticks counted from its start, it is
 * abandoned.
 * </p>
 * <p>
 * A transition ends early, with the spider removed and no error, when the player disconnects, or when the player
 * leaves the instance the transition belongs to after having arrived there (for {@link Kind#CLOSE} that is the lobby,
 * the normal end). When the round finishes ({@link GameFinishEvent}), every running transition is cancelled. The end
 * of the waiting phase does not end a transition.
 * </p>
 * <p>
 * Usage:
 * </p>
 * <pre>{@code
 * WakeUpTransition transition = new WakeUpTransition();
 * transition.register(MinecraftServer.getGlobalEventHandler());
 * transition.close(lobbyPlayers, lobbyInstance);   // waiting phase start
 * transition.open(players, gameInstance);          // waiting phase tick 1, before or after the teleport
 * }</pre>
 *
 * @version 1.3.0
 * @since 2.16.0
 */
public final class WakeUpTransition {

    private static final Logger LOGGER = LoggerFactory.getLogger(WakeUpTransition.class);

    /**
     * How long the camera stays on the spider after the switch of an {@link Kind#OPEN} transition, in server ticks.
     * A {@link Kind#CLOSE} transition has no duration.
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

    /**
     * The length of one period of the resource pack's {@code GameTime} uniform, in world age ticks. The uniform is
     * {@code (worldAge % 24000) / 24000}, so an age in the band {@code [0, 6000)} plays the close animation and an age
     * in {@code [12000, 18000)} plays the open animation.
     */
    public static final long WORLD_AGE_PERIOD = 24000L;

    /**
     * The world age, modulo {@link #WORLD_AGE_PERIOD}, at which the open animation starts.
     */
    public static final long OPEN_BAND_START = 12000L;

    private final Map<TransitionKey, Transition> transitions = new HashMap<>();

    /**
     * What a transition does for the player.
     */
    public enum Kind {

        /** Eyes close in the lobby; the camera stays on the spider until the player leaves the lobby. */
        CLOSE("close", "the lobby"),
        /** Eyes open in the game instance; the camera returns {@link #DURATION_TICKS} ticks after the switch. */
        OPEN("open", "the game instance");

        private final String label;
        private final String place;

        Kind(String label, String place) {
            this.label = label;
            this.place = place;
        }

        /**
         * The name used in the log lines.
         *
         * @return the lower case name of the kind
         */
        public String label() {
            return label;
        }

        /**
         * The place the transition waits for the player in, used in the log lines.
         *
         * @return a description of the instance the kind belongs to
         */
        String place() {
            return place;
        }
    }

    /**
     * Registers the listeners which end a transition early, and the cleanup when the round finishes.
     *
     * @param node the node to listen on; the global event handler in production
     */
    public void register(EventNode<Event> node) {
        node.addListener(GameFinishEvent.class, event -> cancelAll("game finished"));
        node.addListener(PlayerDisconnectEvent.class, event -> cancelPlayer(event.getPlayer(), "player disconnected"));
        node.addListener(RemoveEntityFromInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;
            for (Kind kind : Kind.values()) {
                Transition transition = transitions.get(new TransitionKey(player.getUuid(), kind));
                // Only the instance the transition belongs to counts: the lobby the player leaves on the way to the
                // game is not the place of an open transition's spider
                if (transition != null && transition.arrived && transition.instance == event.getInstance()) {
                    cancel(transition, kind == Kind.CLOSE ? "teleported" : "player left the instance");
                }
            }
        });
    }

    /**
     * Starts a {@link Kind#CLOSE} transition for every given player, see {@link #close(Player, Instance)}.
     *
     * @param players the players in the lobby
     * @param lobby   the lobby instance the players are in and the spiders live in
     */
    public void close(Iterable<Player> players, Instance lobby) {
        start(Kind.CLOSE, players, lobby);
    }

    /**
     * Starts a {@link Kind#CLOSE} transition for one player. A close already running for the player is replaced.
     *
     * @param player the player whose eyes close
     * @param lobby  the lobby instance the player is in
     */
    public void close(Player player, Instance lobby) {
        close(List.of(player), lobby);
    }

    /**
     * Starts an {@link Kind#OPEN} transition for every given player, see {@link #open(Player, Instance)}.
     *
     * @param players the players that are being moved into the game map
     * @param game    the game instance the players are moved into
     */
    public void open(Iterable<Player> players, Instance game) {
        start(Kind.OPEN, players, game);
    }

    /**
     * Starts an {@link Kind#OPEN} transition for one player. An open already running for the player is replaced; a
     * running close is not affected. The player does not have to be in the game instance yet: the spider is spawned
     * once the player arrives there, at the player's position at that moment. The camera switches once the spider is
     * viewable for the player, see the class documentation.
     *
     * @param player the player whose eyes open
     * @param game   the instance the player is moved into, which is where the spider lives
     */
    public void open(Player player, Instance game) {
        open(List.of(player), game);
    }

    private void start(Kind kind, Iterable<Player> players, Instance target) {
        Objects.requireNonNull(target, "target");
        Batch batch = new Batch(kind);
        for (Player player : players) {
            start(batch, player, target);
        }
    }

    private void start(Batch batch, Player player, Instance target) {
        Kind kind = batch.kind;
        cancel(player, kind, "replaced by a new transition");

        TransitionKey key = new TransitionKey(player.getUuid(), kind);
        Transition transition = new Transition(batch, player, target);
        transitions.put(key, transition);
        if (transition.isAtTarget()) {
            spawnSpider(transition);
        } else {
            LOGGER.info("wake-up transition [{}]: waiting for {} to arrive in {}",
                    kind.label(), player.getUsername(), kind.place());
        }
        transition.task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> tick(key))
                .repeat(TaskSchedule.tick(1))
                .schedule();
    }

    /**
     * Spawns the spider in the target instance at the player's current eye position and view. Runs once per
     * transition, when the player is in the target instance for the first time.
     */
    private static void spawnSpider(Transition transition) {
        Player player = transition.player;
        String label = transition.kind().label();
        LOGGER.info("wake-up transition [{}]: {} arrived, spawning spider", label, player.getUsername());

        WakeUpSpider spider = new WakeUpSpider();
        Pos eye = player.getPosition();
        double spiderY = eye.y() + player.getEyeHeight() - spider.getEyeHeight();
        Pos spawn = eye.withY(spiderY);
        CompletableFuture<Void> spawned = Objects.requireNonNullElseGet(
                spider.setInstance(transition.instance, spawn), () -> CompletableFuture.completedFuture(null));
        spider.setView(spawn.yaw(), spawn.pitch(), spawn.yaw());
        spider.updateViewableRule(viewer -> viewer.getUuid().equals(player.getUuid()));
        LOGGER.info("wake-up transition [{}]: spider spawned for {}", label, player.getUsername());

        transition.spider = spider;
        transition.spawned = spawned;
        transition.arrived = true;
    }

    /**
     * Ends the transitions of one player: the spiders are removed and the scheduled checks are cancelled. If a camera
     * was on a spider, it returns to the player. Does nothing if no transition is running for the player.
     *
     * @param player the player whose transitions should end
     */
    public void cancel(Player player) {
        cancelPlayer(player, "cancelled");
    }

    /**
     * Ends every running transition, see {@link #cancel(Player)}.
     */
    public void cancelAll() {
        cancelAll("cancelled");
    }

    /**
     * Checks whether any transition is running for the given player, pending or with the camera on the spider.
     *
     * @param player the player to check
     * @return {@code true} if a transition is running for this player
     */
    public boolean isRunning(Player player) {
        for (Kind kind : Kind.values()) {
            if (isRunning(player, kind)) return true;
        }
        return false;
    }

    /**
     * Checks whether a transition of the given kind is running for the player.
     *
     * @param player the player to check
     * @param kind   the kind of transition
     * @return {@code true} if a transition of this kind is running for this player
     */
    public boolean isRunning(Player player, Kind kind) {
        return transitions.containsKey(new TransitionKey(player.getUuid(), kind));
    }

    /**
     * Returns the world age a batch of the given kind moves an instance to, given its current world age.
     * <ul>
     *     <li>{@link Kind#CLOSE}: the next multiple of {@link #WORLD_AGE_PERIOD} that is not below the current age.
     *     Its close band is {@code [0, 6000)} of the period, so the eyes are shut from the first second on.</li>
     *     <li>{@link Kind#OPEN}: the next age that is not below the current age and is {@link #OPEN_BAND_START}
     *     modulo {@link #WORLD_AGE_PERIOD}, the start of the open band.</li>
     * </ul>
     * The world age only moves forward, so the instance time never goes backwards.
     *
     * @param kind    the kind of the batch
     * @param current the current world age of the instance
     * @return the world age to set
     */
    public static long worldAgeFor(Kind kind, long current) {
        return switch (kind) {
            case CLOSE -> Math.ceilDiv(current, WORLD_AGE_PERIOD) * WORLD_AGE_PERIOD;
            case OPEN -> Math.ceilDiv(current - OPEN_BAND_START, WORLD_AGE_PERIOD) * WORLD_AGE_PERIOD + OPEN_BAND_START;
        };
    }

    private void cancelAll(String reason) {
        for (Transition transition : new ArrayList<>(transitions.values())) {
            cancel(transition, reason);
        }
    }

    private void cancelPlayer(Player player, String reason) {
        for (Kind kind : Kind.values()) {
            cancel(player, kind, reason);
        }
    }

    private void cancel(Player player, Kind kind, String reason) {
        Transition transition = transitions.get(new TransitionKey(player.getUuid(), kind));
        if (transition != null) {
            cancel(transition, reason);
        }
    }

    private void cancel(Transition transition, String reason) {
        if (!transitions.remove(transition.key(), transition)) return;
        transition.task.cancel();
        String label = transition.kind().label();
        String name = transition.player.getUsername();
        if (transition.onSpider) {
            if (transition.player.isOnline()) {
                transition.player.stopSpectating();
            }
            LOGGER.info("wake-up transition [{}]: camera returned for {} ({})", label, name, reason);
        } else {
            LOGGER.info("wake-up transition [{}]: abandoned for {} ({})", label, name, reason);
        }
        if (transition.spider != null) {
            transition.spider.remove();
        }
    }

    /**
     * Runs once per server tick for one transition: waits for the spider to be viewable, switches the camera after
     * the delay, or counts the {@link #DURATION_TICKS} of an open while the camera is on the spider.
     */
    private void tick(TransitionKey key) {
        Transition transition = transitions.get(key);
        if (transition == null) return;

        if (transition.onSpider) {
            // A close has no duration: it ends when the player leaves the lobby
            if (transition.kind() == Kind.OPEN) {
                transition.ticksOnSpider++;
                if (transition.ticksOnSpider >= DURATION_TICKS) {
                    cancel(transition, DURATION_TICKS + " ticks elapsed");
                }
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
                        : "player did not arrive in " + transition.kind().place() + " after " + PENDING_TIMEOUT_TICKS + " ticks";
                cancel(transition, reason);
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
        ensureWorldAge(transition);
        transition.player.spectate(transition.spider);
        transition.onSpider = true;
        transition.ticksOnSpider = 0;
        LOGGER.info("wake-up transition [{}]: camera switched to spider for {}",
                transition.kind().label(), transition.player.getUsername());
    }

    /**
     * Moves the world age of the transition's instance into the band of its kind, once per batch and instance, right
     * before the first camera switch of the batch in that instance.
     */
    private static void ensureWorldAge(Transition transition) {
        Batch batch = transition.batch;
        Instance instance = transition.instance;
        if (!batch.agedInstances.add(instance)) return;

        long target = worldAgeFor(batch.kind, instance.getWorldAge());
        // Minestom sends the new world age to the instance's players at once
        instance.setWorldAge(target);
        LOGGER.info("wake-up transition [{}]: world age of {} set to {}", batch.kind.label(), instance.getUuid(), target);
    }

    /**
     * The players started by one call of {@link #close} or {@link #open}, which share the world age of an instance.
     */
    private static final class Batch {

        private final Kind kind;
        private final Set<Instance> agedInstances = new HashSet<>();

        private Batch(Kind kind) {
            this.kind = kind;
        }
    }

    private record TransitionKey(UUID playerId, Kind kind) {
    }

    private static final class Transition {

        private final Batch batch;
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

        private Transition(Batch batch, Player player, Instance instance) {
            this.batch = batch;
            this.player = player;
            this.instance = instance;
        }

        private Kind kind() {
            return batch.kind;
        }

        private TransitionKey key() {
            return new TransitionKey(player.getUuid(), batch.kind);
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
