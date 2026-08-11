package net.onelitefeather.cygnus.tunnelvision;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.timer.Task;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Drives the tunnel vision of every survivor from a single repeating task.
 * <p>
 * One task rather than one per player, as {@code StaminaBar} does it, so there is a single place to
 * clean up.
 * </p>
 * <p>
 * The stamina arrives as a function rather than as a service: it only needs a number, so the
 * dependency does not have to be a live object the service keeps in sync.
 * </p>
 * <p>
 * The slender used to feed into this as well. He now speaks through {@code SlenderGazeService}
 * instead, which asks whether a survivor can see him rather than how near he is.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class TunnelVisionService {

    private final TunnelVisionRenderer renderer;
    private final ToDoubleFunction<Player> stamina;
    private final Map<UUID, Tracked> survivors;

    private @Nullable Task task;

    /**
     * Creates a new service.
     *
     * @param renderer the renderer that puts a stage on the screen
     * @param stamina  supplies a survivor's remaining stamina as a share of a full bar
     */
    public TunnelVisionService(TunnelVisionRenderer renderer, ToDoubleFunction<Player> stamina) {
        this.renderer = renderer;
        this.stamina = stamina;
        this.survivors = new LinkedHashMap<>();
    }

    /**
     * Registers the given survivors and starts the update task if it is not already running.
     *
     * @param survivors the survivors to draw for
     */
    public void start(Set<Player> survivors) {
        for (Player survivor : survivors) {
            this.survivors.put(survivor.getUuid(), new Tracked(survivor, new TunnelVisionStage()));
        }

        if (this.task != null) return;
        this.task = MinecraftServer.getSchedulerManager()
                .buildTask(this::tick)
                .repeat(TunnelVisionStage.TICK_MILLIS, ChronoUnit.MILLIS)
                .schedule();
    }

    /**
     * Hooks the service into the round's lifecycle.
     * <p>
     * The service listens for itself rather than being called from {@code GameStartListener} and
     * friends: it needs nothing from them beyond the moment, and keeping the wiring here leaves
     * their signatures alone.
     * </p>
     *
     * @param node      the node to register on
     * @param survivors supplies the survivors of the starting round
     */
    public void registerListener(EventNode<Event> node, Supplier<Set<Player>> survivors) {
        node.addListener(GameStartEvent.class, event -> this.start(survivors.get()));
        node.addListener(PlayerDeathEvent.class, event -> this.remove(event.getPlayer()));
        node.addListener(PlayerDisconnectEvent.class, event -> this.remove(event.getPlayer()));
        node.addListener(GameFinishEvent.class, event -> this.cleanUp());
    }

    /**
     * Stops drawing for a survivor and clears whatever is still on their screen — on death, on
     * the way into the spectator team, or on quit.
     *
     * @param player the survivor to drop
     */
    public void remove(Player player) {
        if (this.survivors.remove(player.getUuid()) == null) return;
        this.renderer.clear(player);
    }

    /**
     * Clears every survivor's screen and stops the update task.
     */
    public void cleanUp() {
        for (Tracked tracked : this.survivors.values()) {
            this.renderer.clear(tracked.player());
        }
        this.survivors.clear();

        if (this.task == null) return;
        this.task.cancel();
        this.task = null;
    }

    /**
     * Updates every tracked survivor once.
     */
    void tick() {
        if (this.survivors.isEmpty()) return;

        for (Tracked tracked : this.survivors.values()) {
            Player survivor = tracked.player();
            double intensity = TunnelVisionIntensity.fromStamina(this.stamina.applyAsDouble(survivor));
            this.renderer.render(survivor, tracked.stage().update(intensity));
        }
    }

    /**
     * Pairs a survivor with the overlay state that belongs to them.
     *
     * @param player the survivor
     * @param stage  their stage state, carrying hysteresis and heartbeat
     */
    private record Tracked(Player player, TunnelVisionStage stage) {
    }
}
