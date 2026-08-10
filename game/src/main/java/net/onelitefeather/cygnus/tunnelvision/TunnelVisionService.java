package net.onelitefeather.cygnus.tunnelvision;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
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
 * One task rather than one per player, as {@code StaminaBar} does it: the slender's position is
 * read once per tick instead of once per survivor, and there is a single place to clean up.
 * </p>
 * <p>
 * Both inputs arrive as functions rather than as services. The stamina share only needs a number,
 * and the slender may be absent at any moment, so neither dependency has to be a live object the
 * service keeps in sync.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class TunnelVisionService {

    private final TunnelVisionRenderer renderer;
    private final ToDoubleFunction<Player> stamina;
    private final Supplier<@Nullable Player> slender;
    private final Map<UUID, Tracked> survivors;

    private @Nullable Task task;

    /**
     * Creates a new service.
     *
     * @param renderer the renderer that puts a stage on the screen
     * @param stamina  supplies a survivor's remaining stamina as a share of a full bar
     * @param slender  supplies the current slender, or {@code null} while there is none
     */
    public TunnelVisionService(
            TunnelVisionRenderer renderer,
            ToDoubleFunction<Player> stamina,
            Supplier<@Nullable Player> slender
    ) {
        this.renderer = renderer;
        this.stamina = stamina;
        this.slender = slender;
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

        Player currentSlender = this.slender.get();
        for (Tracked tracked : this.survivors.values()) {
            Player survivor = tracked.player();
            double staminaShare = TunnelVisionIntensity.fromStamina(this.stamina.applyAsDouble(survivor));
            double slenderShare = this.slenderShare(survivor, currentSlender);
            double combined = TunnelVisionIntensity.combine(staminaShare, slenderShare);
            this.renderer.render(survivor, tracked.stage().update(combined));
        }
    }

    /**
     * Calculates the slender's share for one survivor.
     * <p>
     * A slender who is absent or somewhere else entirely weighs nothing — distance across
     * instances is meaningless.
     * </p>
     *
     * @param survivor the survivor to calculate for
     * @param slender  the current slender, may be {@code null}
     * @return the share in {@code [0, 1]}
     */
    private double slenderShare(Player survivor, @Nullable Player slender) {
        if (slender == null) return 0.0D;

        Instance instance = slender.getInstance();
        if (instance == null || !instance.equals(survivor.getInstance())) return 0.0D;

        return TunnelVisionIntensity.fromSlender(survivor.getPosition(), slender.getPosition());
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
