package net.onelitefeather.cygnus.tunnelvision;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.utils.PlayerState;
import net.onelitefeather.cygnus.utils.RepeatingTask;

import java.time.temporal.ChronoUnit;
import java.util.Set;
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
 * <p>
 * Unlike {@code AmbientProvider}, this service is not merely started and stopped by name from
 * {@code GameStartListener} and {@code Cygnus.finishGame()} — it still exposes {@link #startTask()}
 * and {@link #stopTask()} for exactly that purpose, but it also has to react the moment a single
 * survivor dies or disconnects, or the vignette they last saw keeps showing on a screen nobody is
 * playing through any more. Neither of those listeners knows about individual players today, and
 * teaching them to would spread a tunnel-vision concern into files that otherwise have nothing to do
 * with it. Registering here, scoped to this service's own node, keeps that mapping local to the one
 * class that needs it — {@code BloodSplatterService} and {@code SlenderGazeService} register
 * themselves for the same reason.
 * </p>
 *
 * @author TheMeinerLP
 * @version 2.1.0
 * @since 2.7.0
 */
public final class TunnelVisionService {

    private final TunnelVisionRenderer renderer;
    private final ToDoubleFunction<Player> stamina;
    private final PlayerState<Tracked> survivors = new PlayerState<>();
    private final RepeatingTask task = new RepeatingTask(this::tick);

    /**
     * Creates a new service.
     *
     * @param renderer the renderer that puts a stage on the screen
     * @param stamina  supplies a survivor's remaining stamina as a share of a full bar
     */
    public TunnelVisionService(TunnelVisionRenderer renderer, ToDoubleFunction<Player> stamina) {
        this.renderer = renderer;
        this.stamina = stamina;
    }

    /**
     * Starts the update task. Does nothing if it is already running.
     */
    public void startTask() {
        this.task.start(TunnelVisionStage.TICK_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Stops the update task. Does nothing if it is not running.
     */
    public void stopTask() {
        this.task.stop();
    }

    /**
     * Starts drawing for a survivor, with a fresh stage.
     * <p>
     * This is bookkeeping only: it does not touch the update task, so {@link #registerListener} can
     * compose it with {@link #startTask()} instead of the two always happening together.
     * </p>
     *
     * @param survivor the survivor to draw for
     */
    public void track(Player survivor) {
        this.survivors.put(survivor, new Tracked(survivor, new TunnelVisionStage()));
    }

    /**
     * Hooks the service into the round's lifecycle.
     * <p>
     * See the class documentation for why this service registers itself rather than being called by
     * name the way {@code AmbientProvider} is.
     * </p>
     *
     * @param node      the node to register on
     * @param survivors supplies the survivors of the starting round
     */
    public void registerListener(EventNode<Event> node, Supplier<Set<Player>> survivors) {
        node.addListener(GameStartEvent.class, event -> {
            this.startTask();
            for (Player survivor : survivors.get()) {
                this.track(survivor);
            }
        });
        node.addListener(PlayerDeathEvent.class, event -> this.remove(event.getPlayer()));
        node.addListener(PlayerDisconnectEvent.class, event -> this.remove(event.getPlayer()));
        node.addListener(GameFinishEvent.class, event -> {
            this.cleanUp();
            this.stopTask();
        });
    }

    /**
     * Stops drawing for a survivor and clears whatever is still on their screen — on death, on
     * the way into the spectator team, or on quit.
     *
     * @param player the survivor to drop
     */
    public void remove(Player player) {
        if (this.survivors.remove(player) == null) return;
        this.renderer.clear(player);
    }

    /**
     * Clears every survivor's screen and stops tracking all of them, without touching the update
     * task — pair with {@link #stopTask()} to end a round the way {@link #registerListener} does.
     */
    public void cleanUp() {
        for (Tracked tracked : this.survivors.values()) {
            this.renderer.clear(tracked.player());
        }
        this.survivors.clear();
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
