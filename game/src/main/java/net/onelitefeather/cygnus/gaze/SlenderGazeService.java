package net.onelitefeather.cygnus.gaze;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.OverlayTextureKeys;
import net.onelitefeather.cygnus.overlay.ScreenOverlay;
import net.onelitefeather.cygnus.utils.PlayerState;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
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

    /** Where the glitch textures live, as {@code camera_overlay} resolves them. */
    static final String TEXTURE_PATH = "gui/glitch/level_";

    /** How many frames the tearing runs through. */
    static final int FRAMES = 4;

    /** How long a frame stays on screen. */
    static final int TICK_MILLIS = 100;

    private static final Key[][] TEXTURES = OverlayTextureKeys.table(
            TEXTURE_PATH, SlenderGaze.LEVELS, FRAMES, OverlayTextureKeys.ONE_BASED, OverlayTextureKeys.ONE_BASED);

    private final ScreenOverlay overlay;
    private final Supplier<@Nullable Player> slender;
    private final PlayerState<Player> survivors = new PlayerState<>();
    private final RepeatingTask task = new RepeatingTask(this::tick);

    private int frame;

    /**
     * Creates a new service.
     *
     * @param overlay the overlay that owns the players' screens
     * @param slender supplies the current slender, or {@code null} while there is none
     */
    public SlenderGazeService(ScreenOverlay overlay, Supplier<@Nullable Player> slender) {
        this.overlay = overlay;
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
        this.survivors.put(survivor, survivor);
    }

    /**
     * Stops drawing for a survivor and clears what is left on their screen.
     *
     * @param player the survivor to drop
     */
    public void remove(Player player) {
        if (this.survivors.remove(player) == null) return;
        this.overlay.set(player, OverlayLayer.GLITCH, null);
    }

    /**
     * Clears every tracked survivor's screen and forgets all of them.
     */
    public void cleanUp() {
        for (Player survivor : this.survivors.values()) {
            this.overlay.set(survivor, OverlayLayer.GLITCH, null);
        }
        this.survivors.clear();
    }

    /**
     * Puts one level on a player's screen and leaves it there, for judging the drawings without a
     * slender to walk in front of.
     * <p>
     * This sits on the service rather than a separate type because it draws from the very texture
     * table {@link #tick()} already builds; splitting it out would mean either rebuilding that table
     * a second time or exposing it, trading one seam for a worse one over two lines of
     * {@code GlitchCommand} preview code.
     * </p>
     * <p>
     * Showing and clearing are one method rather than two because {@link SlenderGaze#NONE} already
     * says "nothing to draw" everywhere else in this class — {@link #tick()} reads it off
     * {@link SlenderGaze#levelOf(net.minestom.server.coordinate.Pos, net.minestom.server.coordinate.Pos)}
     * on every pass — so a separate {@code hide} would be a second spelling of a level the type
     * already has.
     * </p>
     *
     * @param player the player to draw for
     * @param level  the level between {@code 0} and {@code SlenderGaze.LEVELS - 1}, or
     *               {@link SlenderGaze#NONE} to take the tearing off their screen
     */
    public void preview(Player player, int level) {
        if (level == SlenderGaze.NONE) {
            this.overlay.set(player, OverlayLayer.GLITCH, null);
            return;
        }
        int clamped = Math.clamp(level, 0, SlenderGaze.LEVELS - 1);
        this.overlay.set(player, OverlayLayer.GLITCH, TEXTURES[clamped][this.frame % FRAMES]);
    }

    /**
     * Advances the tearing by one frame and redraws every survivor.
     */
    void tick() {
        if (this.survivors.isEmpty()) return;

        Player currentSlender = this.slender.get();
        this.frame++;

        for (Player survivor : this.survivors.values()) {
            int level = this.levelFor(survivor, currentSlender);
            if (level == SlenderGaze.NONE) {
                this.overlay.set(survivor, OverlayLayer.GLITCH, null);
                continue;
            }
            this.overlay.set(survivor, OverlayLayer.GLITCH, TEXTURES[level][this.frame % FRAMES]);
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
}
