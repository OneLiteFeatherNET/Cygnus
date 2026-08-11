package net.onelitefeather.cygnus.blood;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.timer.Task;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.ScreenOverlay;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;

/**
 * Throws a splatter of blood across the screen when a player is hit and fades it out again.
 * <p>
 * The textures are laid out as direction × variant × frame. The direction aims the splatter at the
 * side the hit came from, the variant keeps repeated hits from looking mechanical, and the frames
 * are the fade — Minecraft cannot animate a camera overlay, so the server steps through them.
 * </p>
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
public final class BloodSplatterService {

    /** How many drawings exist per direction. */
    static final int VARIANTS = 2;

    /** How many frames a splatter fades over. */
    static final int FRAMES = 12;

    /**
     * How long a single frame stays on screen. Twelve frames at this rate keep the splatter alive
     * for the same 1.2 seconds as six did at twice the interval, but it runs down the screen
     * smoothly rather than in visible steps.
     */
    static final int FRAME_MILLIS = 100;

    /** Where the splatter textures live, as {@code camera_overlay} resolves them. */
    static final String TEXTURE_PATH = "gui/blood/";

    private static final Key[] TEXTURES = buildTextures();

    private final ScreenOverlay overlay;
    private final IntUnaryOperator variantPicker;
    private final Map<UUID, Splatter> active = new ConcurrentHashMap<>();

    private @Nullable Task task;

    /**
     * Creates a new service.
     *
     * @param overlay       the overlay that owns the player's screen
     * @param variantPicker picks a variant below the given bound
     */
    public BloodSplatterService(ScreenOverlay overlay, IntUnaryOperator variantPicker) {
        this.overlay = overlay;
        this.variantPicker = variantPicker;
    }

    /**
     * Listens for hits and for players leaving.
     *
     * @param node the node to register on
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(PlayerDamagedEvent.class, event -> this.splatter(
                event.getPlayer(),
                BloodDirection.between(event.getPlayer().getPosition(), event.getSource())
        ));
        node.addListener(PlayerDisconnectEvent.class, event -> this.clear(event.getPlayer()));
    }

    /**
     * Throws a fresh splatter, replacing whatever is still fading.
     *
     * @param player    the player who was hit
     * @param direction the side the hit came from
     */
    public void splatter(Player player, BloodDirection direction) {
        Splatter splatter = new Splatter(player, direction, this.variantPicker.applyAsInt(VARIANTS));
        this.active.put(player.getUuid(), splatter);
        this.draw(splatter);
        this.ensureTask();
    }

    /**
     * Takes the splatter off a player's screen.
     *
     * @param player the player to clear
     */
    public void clear(Player player) {
        if (this.active.remove(player.getUuid()) == null) return;
        this.overlay.set(player, OverlayLayer.BLOOD, null);
    }

    /**
     * Advances every splatter by one frame and drops the ones that have faded out.
     */
    void tick() {
        Iterator<Splatter> splatters = this.active.values().iterator();
        while (splatters.hasNext()) {
            Splatter splatter = splatters.next();
            splatter.frame++;

            if (splatter.frame >= FRAMES) {
                splatters.remove();
                this.overlay.set(splatter.player, OverlayLayer.BLOOD, null);
                continue;
            }
            this.draw(splatter);
        }

        if (!this.active.isEmpty() || this.task == null) return;
        // Nothing is bleeding; the task would only spin over an empty map until the next hit.
        this.task.cancel();
        this.task = null;
    }

    /**
     * Puts a splatter's current frame on its player's screen.
     *
     * @param splatter the splatter to draw
     */
    private void draw(Splatter splatter) {
        int index = (splatter.direction.ordinal() * VARIANTS + splatter.variant) * FRAMES + splatter.frame;
        this.overlay.set(splatter.player, OverlayLayer.BLOOD, TEXTURES[index]);
    }

    /**
     * Starts the fade task unless it is already running.
     */
    private void ensureTask() {
        if (this.task != null) return;
        this.task = MinecraftServer.getSchedulerManager()
                .buildTask(this::tick)
                .repeat(FRAME_MILLIS, ChronoUnit.MILLIS)
                .schedule();
    }

    /**
     * Builds the texture key for every cell of the direction × variant × frame grid.
     *
     * @return the keys, indexed the same way
     */
    private static Key[] buildTextures() {
        Key[] textures = new Key[BloodDirection.values().length * VARIANTS * FRAMES];
        for (BloodDirection direction : BloodDirection.values()) {
            for (int variant = 0; variant < VARIANTS; variant++) {
                for (int frame = 0; frame < FRAMES; frame++) {
                    int index = (direction.ordinal() * VARIANTS + variant) * FRAMES + frame;
                    String name = "%s%s_%d_%d".formatted(
                            TEXTURE_PATH, direction.name().toLowerCase(Locale.ROOT), variant + 1, frame + 1);
                    textures[index] = Key.key("cygnus", name);
                }
            }
        }
        return textures;
    }

    /**
     * One player's running splatter.
     */
    private static final class Splatter {

        private final Player player;
        private final BloodDirection direction;
        private final int variant;
        private int frame;

        private Splatter(Player player, BloodDirection direction, int variant) {
            this.player = player;
            this.direction = direction;
            this.variant = variant;
        }
    }
}
