package net.onelitefeather.cygnus.setup.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shows the map builder every page spot around them as a page hanging where it will appear in the
 * game. Spots close together form a group (see {@link PageGroups}): every group glows in its own
 * colour and carries a label with its size and share of all spots. A group holding too many spots
 * glows red, a lone spot grey. Hitting a page asks to delete its spot, through the same confirmation
 * as the page list.
 * <p>
 * The displays exist for the builder alone and only as packets: nothing is put into the world, so
 * they cost the server no ticking and no chunk loading. Every spot gets its packets built once, and
 * only spots that come into or go out of reach are sent.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 2.18.0
 */
public final class PagePreview {

    /** How far around the builder the spots are shown, in blocks. */
    static final double REACH = 64.0D;

    /** How often the preview checks which spots came into or went out of reach, in ticks. */
    static final int INTERVAL_TICKS = 20;

    /** The glow of a group that holds too many spots. */
    static final int CROWDED_COLOUR = 0xFF5555;

    /** The glow of a spot without neighbours. */
    static final int LONE_COLOUR = 0xAAAAAA;

    /** The glows the other groups take in turn, none of them red or grey. */
    static final int[] GROUP_COLOURS = {0x55FF55, 0x55FFFF, 0xFFAA00, 0xFF55FF, 0x5555FF, 0xFFFF55};

    /** How far above the middle of a group its label floats, in blocks. */
    static final double LABEL_HEIGHT = 1.0D;

    /** The size of the invisible box in front of a page that takes the builder's hit, in blocks. */
    static final float BOX_SIZE = 0.6F;

    private static final ItemStack PAGE = ItemStack.of(Material.PAPER);

    private final Player player;
    private final Supplier<? extends Collection<PageResource>> spots;
    private final Map<Integer, EntityMetaDataPacket> looks;
    private final EntityMetaDataPacket boxLook = boxLook();
    private Map<PageResource, Marker> markers = Map.of();
    private final Map<PageResource, Marker> shown = new HashMap<>();
    private List<Label> labels = List.of();
    private final Map<Integer, Label> shownLabels;
    /** The spots behind the boxes the builder can hit right now. Read from the network thread. */
    private final Map<Integer, PageResource> boxes = new ConcurrentHashMap<>();
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
        this.looks = new HashMap<>();
        this.shownLabels = new HashMap<>();
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
            if (away) {
                gone.add(entry.getValue().id());
                gone.add(entry.getValue().boxId());
                this.boxes.remove(entry.getValue().boxId());
            }
            return away;
        });
        this.shownLabels.values().removeIf(label -> {
            boolean away = label.centre().distanceSquared(here) > reach;
            if (away) gone.add(label.id());
            return away;
        });
        if (!gone.isEmpty()) this.player.sendPacket(new DestroyEntitiesPacket(gone));
        for (Map.Entry<PageResource, Marker> entry : this.markers.entrySet()) {
            if (this.shown.containsKey(entry.getKey())) continue;
            if (entry.getKey().position().distanceSquared(here) > reach) continue;
            Marker marker = entry.getValue();
            this.player.sendPacket(marker.spawn());
            this.player.sendPacket(marker.look());
            this.player.sendPacket(marker.boxSpawn());
            this.player.sendPacket(marker.boxLook());
            this.shown.put(entry.getKey(), marker);
            this.boxes.put(marker.boxId(), entry.getKey());
        }
        for (Label label : this.labels) {
            if (this.shownLabels.containsKey(label.id())) continue;
            if (label.centre().distanceSquared(here) > reach) continue;
            this.player.sendPacket(label.spawn());
            this.player.sendPacket(label.look());
            this.shownLabels.put(label.id(), label);
        }
    }

    /**
     * Handles the builder hitting an entity. If it is the box in front of a shown page, the builder is
     * asked whether to delete that spot.
     *
     * @param entityId the id of the entity the builder hit
     * @return {@code true} if it was a page of this preview
     */
    public boolean hit(int entityId) {
        PageResource spot = this.boxes.get(entityId);
        if (spot == null) return false;
        EventDispatcher.call(new PlayerRemoveDataEvent(this.player, MapDataCategory.PAGE, new DialogContext.PageContent(spot, false)));
        return true;
    }

    /**
     * Picks the glow of a group.
     *
     * @param group the group
     * @param index the place of the group among all groups, largest first
     * @return the colour as RGB
     */
    static int colourOf(PageGroups.Group group, int index) {
        if (group.crowded()) return CROWDED_COLOUR;
        if (group.spots().size() == 1) return LONE_COLOUR;
        return GROUP_COLOURS[index % GROUP_COLOURS.length];
    }

    /**
     * Writes the label of a group.
     *
     * @param group the group
     * @return its size and its share of all spots
     */
    static String labelText(PageGroups.Group group) {
        return String.format(Locale.ROOT, "%d pages · %d%%", group.spots().size(), Math.round(group.share() * 100.0D));
    }

    private void build() {
        List<PageGroups.Group> groups = PageGroups.find(List.copyOf(this.spots.get()));
        Map<PageResource, Marker> built = new HashMap<>();
        List<Label> builtLabels = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            PageGroups.Group group = groups.get(index);
            EntityMetaDataPacket template = this.looks.computeIfAbsent(colourOf(group, index), PagePreview::look);
            for (PageResource spot : group.spots()) {
                built.put(spot, this.marker(spot, template));
            }
            if (group.spots().size() > 1) builtLabels.add(label(group));
        }
        this.markers = built;
        this.labels = builtLabels;
    }

    private Marker marker(PageResource spot, EntityMetaDataPacket template) {
        int id = Entity.generateId();
        Pos where = Helper.updatePosition(spot.position().asPos(), spot.face());
        SpawnEntityPacket spawn = new SpawnEntityPacket(id, UUID.randomUUID(), EntityType.ITEM_DISPLAY,
                where, where.yaw(), 0, Vec.ZERO);
        int boxId = Entity.generateId();
        // An interaction box stands on its position, so it is lowered to sit around the page.
        SpawnEntityPacket boxSpawn = new SpawnEntityPacket(boxId, UUID.randomUUID(), EntityType.INTERACTION,
                where.sub(0, BOX_SIZE / 2.0D, 0), 0F, 0, Vec.ZERO);
        return new Marker(id, spawn, new EntityMetaDataPacket(id, template.entries()),
                boxId, boxSpawn, new EntityMetaDataPacket(boxId, this.boxLook.entries()));
    }

    /**
     * Builds the label floating above a group, red if the group holds too many spots.
     */
    private static Label label(PageGroups.Group group) {
        Entity template = new Entity(EntityType.TEXT_DISPLAY);
        TextDisplayMeta meta = (TextDisplayMeta) template.getEntityMeta();
        meta.setText(Component.text(labelText(group), group.crowded() ? NamedTextColor.RED : NamedTextColor.WHITE));
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setSeeThrough(true);
        int id = Entity.generateId();
        Pos centre = group.centre().asPos();
        SpawnEntityPacket spawn = new SpawnEntityPacket(id, UUID.randomUUID(), EntityType.TEXT_DISPLAY,
                centre.add(0, LABEL_HEIGHT, 0), 0F, 0, Vec.ZERO);
        return new Label(centre, id, spawn, new EntityMetaDataPacket(id, template.getMetadataPacket().entries()));
    }

    private void removeShown() {
        if (this.shown.isEmpty() && this.shownLabels.isEmpty()) return;
        List<Integer> ids = new ArrayList<>(this.shown.size() * 2 + this.shownLabels.size());
        for (Marker marker : this.shown.values()) {
            ids.add(marker.id());
            ids.add(marker.boxId());
        }
        ids.addAll(this.shownLabels.keySet());
        this.shown.clear();
        this.boxes.clear();
        this.shownLabels.clear();
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
     * Builds the look of the invisible box in front of every page once.
     */
    private static EntityMetaDataPacket boxLook() {
        Entity template = new Entity(EntityType.INTERACTION);
        InteractionMeta meta = (InteractionMeta) template.getEntityMeta();
        meta.setWidth(BOX_SIZE);
        meta.setHeight(BOX_SIZE);
        meta.setResponse(true);
        return template.getMetadataPacket();
    }

    /**
     * The packets of one spot, built once.
     *
     * @param id       the display's entity id
     * @param spawn    puts the display at the spot
     * @param look     makes it a glowing page
     * @param boxId    the id of the box that takes the builder's hit
     * @param boxSpawn puts the box in front of the page
     * @param boxLook  gives the box its size
     */
    private record Marker(int id, SpawnEntityPacket spawn, EntityMetaDataPacket look,
                          int boxId, SpawnEntityPacket boxSpawn, EntityMetaDataPacket boxLook) {
    }

    /**
     * The packets of one group's label, built once.
     *
     * @param centre the middle of the group, which decides whether the label is in reach
     * @param id     the label's entity id
     * @param spawn  puts the label above the group
     * @param look   gives it its text
     */
    private record Label(Pos centre, int id, SpawnEntityPacket spawn, EntityMetaDataPacket look) {
    }
}
