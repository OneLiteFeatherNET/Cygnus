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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lets the resource pack play its "wake-up eyes" post effect when a round starts.
 * <p>
 * Vanilla clients only apply the {@code minecraft:post_effect/spider.json} effect while their camera spectates a
 * spider. For {@link #DURATION_TICKS} ticks after {@link #start(Player)}, the player's camera is pointed at an
 * invisible spider that exists only for that player and sits at the player's eye position with the player's view.
 * The game mode is never changed: spectating only switches the camera.
 * </p>
 * <p>
 * The transition ends early, with the spider removed and no error, when the player disconnects or leaves the
 * instance the spider lives in. The end of the waiting phase does not end it: the camera stays on the spider for
 * the full {@link #DURATION_TICKS} even though the game phase has already started. When the round finishes
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
 * @version 1.0.0
 * @since 2.16.0
 */
public final class WakeUpTransition {

    /**
     * How long the camera stays on the spider, in server ticks.
     */
    public static final int DURATION_TICKS = 100;

    private final Map<UUID, Transition> transitions = new HashMap<>();

    /**
     * Registers the listeners which end a transition early, and the cleanup when the round finishes.
     *
     * @param node the node to listen on; the global event handler in production
     */
    public void register(EventNode<Event> node) {
        node.addListener(GameFinishEvent.class, event -> cancelAll());
        node.addListener(PlayerDisconnectEvent.class, event -> cancel(event.getPlayer()));
        node.addListener(RemoveEntityFromInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;
            Transition transition = transitions.get(player.getUuid());
            if (transition != null && transition.instance() == event.getInstance()) {
                cancel(player);
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
     * Does nothing if the player is not in an instance.
     *
     * @param player the player to point the camera from
     */
    public void start(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) return;
        cancel(player);

        WakeUpSpider spider = new WakeUpSpider();
        Pos eye = player.getPosition();
        double spiderY = eye.y() + player.getEyeHeight() - spider.getEyeHeight();
        Pos spawn = eye.withY(spiderY);
        spider.setInstance(instance, spawn);
        spider.setView(spawn.yaw(), spawn.pitch(), spawn.yaw());
        spider.updateViewableRule(viewer -> viewer.getUuid().equals(player.getUuid()));

        player.spectate(spider);
        Task task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> end(player.getUuid()))
                .delay(TaskSchedule.tick(DURATION_TICKS))
                .schedule();
        transitions.put(player.getUuid(), new Transition(player, instance, spider, task));
    }

    /**
     * Ends the transition for one player: the camera returns to the player, the spider is removed and the
     * scheduled end is cancelled. Does nothing if no transition is running for the player.
     *
     * @param player the player whose transition should end
     */
    public void cancel(Player player) {
        Transition transition = transitions.remove(player.getUuid());
        if (transition == null) return;
        transition.task().cancel();
        if (transition.player().isOnline()) {
            transition.player().stopSpectating();
        }
        transition.spider().remove();
    }

    /**
     * Ends every running transition, see {@link #cancel(Player)}.
     */
    public void cancelAll() {
        for (Transition transition : new ArrayList<>(transitions.values())) {
            cancel(transition.player());
        }
    }

    /**
     * Checks whether a transition is running for the given player.
     *
     * @param player the player to check
     * @return {@code true} if the camera is currently on the spider for this player
     */
    public boolean isRunning(Player player) {
        return transitions.containsKey(player.getUuid());
    }

    private void end(UUID playerId) {
        Transition transition = transitions.get(playerId);
        if (transition != null) {
            cancel(transition.player());
        }
    }

    private record Transition(Player player, Instance instance, Entity spider, Task task) {
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
