package net.onelitefeather.cygnus.gaze;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.utils.PlayerState;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tears a survivor's picture apart while the slender stands in their view.
 * <p>
 * This replaces what the tunnel vision used to do when he came near, and it asks a different
 * question: not how close he is, but whether they can see him. Standing behind a survivor does
 * nothing at all.
 * </p>
 * <p>
 * A real colour-space shift would need a post-processing shader, and on Minecraft 26.2 those
 * cannot be switched on for a single player, so this is a camera overlay like the others — the
 * colour is laid over the world rather than the world being recalculated.
 * </p>
 *
 * @author TheMeinerLP
 * @version 3.0.0
 * @since 2.7.0
 */
public final class SlenderGazeService {


    /** How many frames the tearing runs through. */
    /** Interval the service updates at. */
    static final int TICK_MILLIS = 100;

    private final GazeSink sink;
    private final Supplier<@Nullable Player> slender;
    private final PlayerState<Tracked> survivors = new PlayerState<>();
    private final RepeatingTask task = new RepeatingTask(this::tick);

    /**
     * Creates a new service.
     *
     * @param sink    where a survivor's level is signalled to, {@link GazeSink#NONE} to work the
     *                levels out without sending them anywhere
     * @param slender supplies the current slender, or {@code null} while there is none
     */
    public SlenderGazeService(GazeSink sink, Supplier<@Nullable Player> slender) {
        this.sink = sink;
        this.slender = slender;
    }

    /**
     * Hooks the service into the round's lifecycle.
     * <p>
     * Mirrors {@code TunnelVisionService}: the service listens for itself rather than being called
     * from {@code GameStartListener} and friends, because — unlike {@code AmbientProvider}, which
     * has no per-player state to speak of — it has to drop an individual survivor's tracking the
     * moment they die or disconnect, not only when the whole round ends. Folding that into the
     * round's start and finish hooks would mean widening their signatures for every service that
     * needs it; listening for itself keeps this self-contained instead.
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
     * Starts the update task. Does nothing if it is already running.
     */
    public void startTask() {
        this.task.start(TICK_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Stops the update task. Does nothing if it is not running. Leaves whatever is on a tracked
     * survivor's screen where it is — pair with {@link #cleanUp()} where every screen needs wiping
     * too.
     */
    public void stopTask() {
        this.task.stop();
    }

    /**
     * Starts drawing for a survivor.
     *
     * @param survivor the survivor to draw for
     */
    public void track(Player survivor) {
        this.survivors.put(survivor, new Tracked(survivor, SlenderGaze.NONE));
        this.sink.attach(survivor);
    }

    /**
     * Stops tracking a survivor. Their level reads as {@link SlenderGaze#NONE} afterwards.
     *
     * @param player the survivor to drop
     */
    public void remove(Player player) {
        if (this.survivors.remove(player) == null) return;
        this.sink.detach(player);
    }

    /**
     * Forgets every tracked survivor.
     */
    public void cleanUp() {
        for (Tracked tracked : List.copyOf(this.survivors.values())) {
            this.sink.detach(tracked.player());
        }
        this.survivors.clear();
    }

    /**
     * Returns how badly the sight of the slender is tearing this survivor's view right now.
     *
     * @param player the survivor to ask about
     * @return a level between {@code 0} and {@link SlenderGaze#LEVELS} minus one, or
     *         {@link SlenderGaze#NONE} when he is out of sight or the player is not tracked
     */
    public int levelOf(Player player) {
        Tracked tracked = this.survivors.get(player);
        return tracked == null ? SlenderGaze.NONE : tracked.level();
    }

    /**
     * Recomputes the level of every tracked survivor.
     */
    void tick() {
        if (this.survivors.isEmpty()) return;

        Player currentSlender = this.slender.get();

        for (Tracked tracked : List.copyOf(this.survivors.values())) {
            Player survivor = tracked.player();
            int level = this.levelFor(survivor, currentSlender);
            if (level == tracked.level()) continue;

            // Only on a change: every channel a sink can use costs the client something, so
            // repeating an unchanged level is waste at best and a stutter at worst.
            this.survivors.put(survivor, new Tracked(survivor, level));
            this.sink.level(survivor, level);
        }
    }

    /**
     * Works out the tearing one survivor gets.
     *
     * @param survivor the survivor to look at
     * @param slender  the current slender, may be {@code null}
     * @return the level, or {@link SlenderGaze#NONE}
     */
    private int levelFor(Player survivor, @Nullable Player slender) {
        if (slender == null) return SlenderGaze.NONE;

        Instance instance = slender.getInstance();
        if (instance == null || !instance.equals(survivor.getInstance())) return SlenderGaze.NONE;

        return SlenderGaze.levelOf(survivor.getPosition(), slender.getPosition());
    }

    /**
     * One survivor and the level their view is currently torn at.
     *
     * @param player the survivor
     * @param level  their level, or {@link SlenderGaze#NONE}
     */
    private record Tracked(Player player, int level) {
    }
}
