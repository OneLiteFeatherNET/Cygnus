package net.onelitefeather.cygnus.setup.page;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.common.page.PageCrowding;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.util.Helper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Shows the map builder every page spot around them as a page hanging where it will appear in the
 * game, glowing red where the map is crowded with spots and green elsewhere.
 * <p>
 * The displays exist for the builder alone and only as packets: nothing is put into the world, so
 * they cost the server no ticking and no chunk loading. Every spot gets its packets built once, and
 * only spots that come into or go out of reach are sent.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.18.0
 */
public final class PagePreview {

    /** How far around the builder the spots are shown, in blocks. */
    static final double REACH = 64.0D;

    /** How often the preview checks which spots came into or went out of reach, in ticks. */
    static final int INTERVAL_TICKS = 20;

    /** The glow of a spot in a crowded part of the map. */
    static final int CROWDED_COLOUR = 0xFF5555;

    /** The glow of any other spot. */
    static final int CALM_COLOUR = 0x55FF55;

    private static final ItemStack PAGE = ItemStack.of(Material.PAPER);

    private final Player player;
    private final Supplier<? extends Collection<PageResource>> spots;
    private final EntityMetaDataPacket crowdedLook = look(CROWDED_COLOUR);
    private final EntityMetaDataPacket calmLook = look(CALM_COLOUR);
    private Map<PageResource, Marker> markers = Map.of();
    private final Map<PageResource, Marker> shown = new HashMap<>();
    private @Nullable Task task;

    /**
     * Creates the preview.
     *
     * @param player the map builder who sees it
     * @param spots  supplies the current page spots of the map
     */
    public PagePreview(Player player, Supplier<? extends Collection<PageResource>> spots) {
        this.player = player;
        this.spots = spots;
    }

    /**
     * Starts showing the spots. Does nothing if it is already running.
     */
    public void show() {
        if (this.task != null) return;
        this.build();
        this.update();
        this.task = MinecraftServer.getSchedulerManager()
                .buildTask(this::update)
                .repeat(TaskSchedule.tick(INTERVAL_TICKS))
                .schedule();
    }

    /**
     * Stops showing the spots and takes every display away.
     */
    public void hide() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.removeShown();
    }

    /**
     * Rebuilds the displays after a spot was added or removed. A change can turn the neighbours of a
     * spot crowded or calm too, so every display is built anew. Does nothing while hidden.
     */
    public void refresh() {
        if (this.task == null) return;
        this.removeShown();
        this.build();
        this.update();
    }

    /**
     * Sends the spots that came into reach and takes away the ones that went out of it.
     */
    void update() {
        if (!this.player.isOnline()) {
            this.hide();
            return;
        }
        Pos here = this.player.getPosition();
        double reach = REACH * REACH;
        List<Integer> gone = new ArrayList<>();
        this.shown.entrySet().removeIf(entry -> {
            boolean away = entry.getKey().position().distanceSquared(here) > reach;
            if (away) gone.add(entry.getValue().id());
            return away;
        });
        if (!gone.isEmpty()) this.player.sendPacket(new DestroyEntitiesPacket(gone));
        for (Map.Entry<PageResource, Marker> entry : this.markers.entrySet()) {
            if (this.shown.containsKey(entry.getKey())) continue;
            if (entry.getKey().position().distanceSquared(here) > reach) continue;
            Marker marker = entry.getValue();
            this.player.sendPacket(marker.spawn());
            this.player.sendPacket(marker.look());
            this.shown.put(entry.getKey(), marker);
        }
    }

    /**
     * Picks the glow of a spot.
     *
     * @param weight the spot's weight, see {@link PageCrowding#weights(Collection)}
     * @return the colour as RGB
     */
    static int colourOf(double weight) {
        return PageCrowding.isCrowded(weight) ? CROWDED_COLOUR : CALM_COLOUR;
    }

    private void build() {
        Map<PageResource, Double> weights = PageCrowding.weights(List.copyOf(this.spots.get()));
        Map<PageResource, Marker> built = new HashMap<>(weights.size());
        weights.forEach((spot, weight) -> {
            int id = Entity.generateId();
            Pos where = Helper.updatePosition(spot.position().asPos(), spot.face());
            SpawnEntityPacket spawn = new SpawnEntityPacket(id, UUID.randomUUID(), EntityType.ITEM_DISPLAY,
                    where, where.yaw(), 0, Vec.ZERO);
            EntityMetaDataPacket template = colourOf(weight) == CROWDED_COLOUR ? this.crowdedLook : this.calmLook;
            built.put(spot, new Marker(id, spawn, new EntityMetaDataPacket(id, template.entries())));
        });
        this.markers = built;
    }

    private void removeShown() {
        if (this.shown.isEmpty()) return;
        List<Integer> ids = this.shown.values().stream().map(Marker::id).toList();
        this.shown.clear();
        if (this.player.isOnline()) this.player.sendPacket(new DestroyEntitiesPacket(ids));
    }

    /**
     * Builds the look of a page display once, so every spot only needs its own id on it.
     */
    private static EntityMetaDataPacket look(int colour) {
        Entity template = new Entity(EntityType.ITEM_DISPLAY);
        ItemDisplayMeta meta = (ItemDisplayMeta) template.getEntityMeta();
        meta.setItemStack(PAGE);
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
        meta.setHasGlowingEffect(true);
        meta.setGlowColorOverride(colour);
        return template.getMetadataPacket();
    }

    /**
     * The packets of one spot, built once.
     *
     * @param id    the display's entity id
     * @param spawn puts the display at the spot
     * @param look  makes it a glowing page
     */
    private record Marker(int id, SpawnEntityPacket spawn, EntityMetaDataPacket look) {
    }
}
