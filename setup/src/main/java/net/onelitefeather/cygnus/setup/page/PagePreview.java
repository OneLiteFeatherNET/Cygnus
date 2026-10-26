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
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
 * they cost the server no ticking and no chunk loading. Every spot keeps its entity ids for as long as
 * it exists, and only what changed is sent: spots and labels that come into or go out of reach, that
 * were added or deleted, and a new look for those whose glow or text changed.
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

    private static final Comparator<PageResource> ANCHOR_ORDER = Comparator
            .comparingDouble((PageResource spot) -> spot.position().x())
            .thenComparingDouble(spot -> spot.position().y())
            .thenComparingDouble(spot -> spot.position().z())
            .thenComparing(PageResource::face);

    private final Player player;
    private final Supplier<? extends Collection<PageResource>> spots;
    private final Map<Integer, EntityMetaDataPacket> looks;
    private final EntityMetaDataPacket boxLook = boxLook();
    private Map<PageResource, Marker> markers = Map.of();
    private final Map<PageResource, Marker> shown = new HashMap<>();
    /** The label of every group, by the group's anchor, see {@link #anchorOf(PageGroups.Group)}. */
    private Map<PageResource, Label> labels = Map.of();
    private final Map<PageResource, Label> shownLabels;
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
     * Brings the displays up to date after a spot was added or removed. A change can turn the
     * neighbours of a spot into another group too, so the groups are found anew, but only the
     * displays that changed are sent, and a label whose group grew or shrank is moved, not spawned
     * again. Does nothing while hidden.
     */
    public void refresh() {
        if (this.task == null) return;
        this.build();
        this.update();
    }

    /**
     * Sends the builder what differs from what they see: displays that came into reach or were added,
     * the removal of those that went out of reach or were deleted, and the new look of those whose
     * glow or text changed.
     */
    void update() {
        if (!this.player.isOnline()) {
            this.hide();
            return;
        }
        Pos here = this.player.getPosition();
        double reach = REACH * REACH;
        List<Integer> gone = new ArrayList<>();
        Iterator<Map.Entry<PageResource, Marker>> shownMarkers = this.shown.entrySet().iterator();
        while (shownMarkers.hasNext()) {
            Map.Entry<PageResource, Marker> entry = shownMarkers.next();
            Marker current = this.markers.get(entry.getKey());
            if (current == null || entry.getKey().position().distanceSquared(here) > reach) {
                gone.add(entry.getValue().id());
                gone.add(entry.getValue().boxId());
                this.boxes.remove(entry.getValue().boxId());
                shownMarkers.remove();
            } else if (current.colour() != entry.getValue().colour()) {
                this.player.sendPacket(current.look());
                entry.setValue(current);
            }
        }
        Iterator<Map.Entry<PageResource, Label>> shownLabels = this.shownLabels.entrySet().iterator();
        while (shownLabels.hasNext()) {
            Map.Entry<PageResource, Label> entry = shownLabels.next();
            Label current = this.labels.get(entry.getKey());
            if (current == null || current.id() != entry.getValue().id()
                    || current.centre().distanceSquared(here) > reach) {
                gone.add(entry.getValue().id());
                shownLabels.remove();
            } else if (current != entry.getValue()) {
                Label shownLabel = entry.getValue();
                if (!current.centre().samePoint(shownLabel.centre())) {
                    this.player.sendPacket(new EntityTeleportPacket(current.id(), labelPosition(current.centre()),
                            Vec.ZERO, 0, false));
                }
                if (current.look() != shownLabel.look()) this.player.sendPacket(current.look());
                entry.setValue(current);
            }
        }
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
        for (Map.Entry<PageResource, Label> entry : this.labels.entrySet()) {
            if (this.shownLabels.containsKey(entry.getKey())) continue;
            Label label = entry.getValue();
            if (label.centre().distanceSquared(here) > reach) continue;
            this.player.sendPacket(label.spawn());
            this.player.sendPacket(label.look());
            this.shownLabels.put(entry.getKey(), label);
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
     * Picks the glow of every group. A crowded group glows red and a lone spot grey. Any other group
     * keeps the colour most of its spots had before, so adding or deleting a spot leaves the colours of
     * the other groups alone. A colour goes to one group only, the larger one first; groups without a
     * colour to keep take one no other group has, as long as there is one left.
     *
     * @param groups   the groups, largest first, see {@link PageGroups#find(Collection)}
     * @param previous the colour every spot glowed in before
     * @return the colour of each group as RGB, in the order of the groups
     */
    static int[] colours(List<PageGroups.Group> groups, Map<PageResource, Integer> previous) {
        int[] colours = new int[groups.size()];
        Set<Integer> taken = new HashSet<>();
        List<Integer> waiting = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            PageGroups.Group group = groups.get(index);
            if (group.crowded()) {
                colours[index] = CROWDED_COLOUR;
            } else if (group.spots().size() == 1) {
                colours[index] = LONE_COLOUR;
            } else {
                Integer kept = keptColour(group, previous, taken);
                if (kept == null) {
                    waiting.add(index);
                } else {
                    colours[index] = kept;
                    taken.add(kept);
                }
            }
        }
        for (int index : waiting) {
            int colour = GROUP_COLOURS[index % GROUP_COLOURS.length];
            for (int free : GROUP_COLOURS) {
                if (!taken.contains(free)) {
                    colour = free;
                    break;
                }
            }
            colours[index] = colour;
            taken.add(colour);
        }
        return colours;
    }

    /**
     * Finds the group colour most of the group's spots glowed in before and no other group took yet.
     */
    private static @Nullable Integer keptColour(PageGroups.Group group, Map<PageResource, Integer> previous,
                                                Set<Integer> taken) {
        Integer kept = null;
        int most = 0;
        for (int colour : GROUP_COLOURS) {
            if (taken.contains(colour)) continue;
            int count = 0;
            for (PageResource spot : group.spots()) {
                if (Integer.valueOf(colour).equals(previous.get(spot))) count++;
            }
            if (count > most) {
                most = count;
                kept = colour;
            }
        }
        return kept;
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

    /**
     * Finds the groups and builds what the builder should see. A spot that was there before keeps its
     * marker and only gets a new look if its glow changed; a label keeps its id as long as its group
     * keeps its anchor.
     */
    private void build() {
        List<PageGroups.Group> groups = PageGroups.find(List.copyOf(this.spots.get()));
        Map<PageResource, Integer> previous = new HashMap<>(this.markers.size());
        this.markers.forEach((spot, marker) -> previous.put(spot, marker.colour()));
        int[] colours = colours(groups, previous);
        Map<PageResource, Marker> built = new HashMap<>(previous.size() + 1);
        Map<PageResource, Label> builtLabels = new HashMap<>();
        for (int index = 0; index < groups.size(); index++) {
            PageGroups.Group group = groups.get(index);
            int colour = colours[index];
            EntityMetaDataPacket template = this.looks.computeIfAbsent(colour, PagePreview::look);
            for (PageResource spot : group.spots()) {
                Marker old = this.markers.get(spot);
                if (old == null) {
                    built.put(spot, this.marker(spot, colour, template));
                } else {
                    built.put(spot, old.colour() == colour ? old : old.withColour(colour, template));
                }
            }
            if (group.spots().size() > 1) {
                PageResource anchor = anchorOf(group);
                builtLabels.put(anchor, label(group, this.labels.get(anchor)));
            }
        }
        this.markers = built;
        this.labels = builtLabels;
    }

    /**
     * Picks the spot a group's label is filed under: the one with the smallest position, so the label
     * stays the same while the group gains or loses other spots.
     */
    private static PageResource anchorOf(PageGroups.Group group) {
        return group.spots().stream().min(ANCHOR_ORDER).orElseThrow();
    }

    private Marker marker(PageResource spot, int colour, EntityMetaDataPacket template) {
        int id = Entity.generateId();
        Pos where = Helper.updatePosition(spot.position().asPos(), spot.face());
        SpawnEntityPacket spawn = new SpawnEntityPacket(id, UUID.randomUUID(), EntityType.ITEM_DISPLAY,
                where, where.yaw(), 0, Vec.ZERO);
        int boxId = Entity.generateId();
        // An interaction box stands on its position, so it is lowered to sit around the page.
        SpawnEntityPacket boxSpawn = new SpawnEntityPacket(boxId, UUID.randomUUID(), EntityType.INTERACTION,
                where.sub(0, BOX_SIZE / 2.0D, 0), 0F, 0, Vec.ZERO);
        return new Marker(id, spawn, colour, new EntityMetaDataPacket(id, template.entries()),
                boxId, boxSpawn, new EntityMetaDataPacket(boxId, this.boxLook.entries()));
    }

    /**
     * Builds the label floating above a group, red if the group holds too many spots. The old label of
     * the group keeps its id, so the builder's client only has to move it or change its text; it is
     * kept as it is if nothing about it changed.
     */
    private static Label label(PageGroups.Group group, @Nullable Label old) {
        Pos centre = group.centre().asPos();
        String text = labelText(group);
        boolean crowded = group.crowded();
        if (old != null) {
            boolean samePlace = old.centre().samePoint(centre);
            boolean sameLook = old.text().equals(text) && old.crowded() == crowded;
            if (samePlace && sameLook) return old;
            SpawnEntityPacket spawn = samePlace ? old.spawn() : labelSpawn(old.id(), centre);
            EntityMetaDataPacket look = sameLook ? old.look() : labelLook(old.id(), text, crowded);
            return new Label(centre, old.id(), spawn, text, crowded, look);
        }
        int id = Entity.generateId();
        return new Label(centre, id, labelSpawn(id, centre), text, crowded, labelLook(id, text, crowded));
    }

    private static SpawnEntityPacket labelSpawn(int id, Pos centre) {
        return new SpawnEntityPacket(id, UUID.randomUUID(), EntityType.TEXT_DISPLAY,
                labelPosition(centre), 0F, 0, Vec.ZERO);
    }

    private static Pos labelPosition(Pos centre) {
        return centre.add(0, LABEL_HEIGHT, 0);
    }

    private static EntityMetaDataPacket labelLook(int id, String text, boolean crowded) {
        Entity template = new Entity(EntityType.TEXT_DISPLAY);
        TextDisplayMeta meta = (TextDisplayMeta) template.getEntityMeta();
        meta.setText(Component.text(text, crowded ? NamedTextColor.RED : NamedTextColor.WHITE));
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setSeeThrough(true);
        return new EntityMetaDataPacket(id, template.getMetadataPacket().entries());
    }

    private void removeShown() {
        if (this.shown.isEmpty() && this.shownLabels.isEmpty()) return;
        List<Integer> ids = new ArrayList<>(this.shown.size() * 2 + this.shownLabels.size());
        for (Marker marker : this.shown.values()) {
            ids.add(marker.id());
            ids.add(marker.boxId());
        }
        for (Label label : this.shownLabels.values()) {
            ids.add(label.id());
        }
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
     * @param colour   the glow of the page as RGB
     * @param look     makes it a page glowing in that colour
     * @param boxId    the id of the box that takes the builder's hit
     * @param boxSpawn puts the box in front of the page
     * @param boxLook  gives the box its size
     */
    private record Marker(int id, SpawnEntityPacket spawn, int colour, EntityMetaDataPacket look,
                          int boxId, SpawnEntityPacket boxSpawn, EntityMetaDataPacket boxLook) {

        /**
         * Returns the same marker glowing in another colour.
         */
        Marker withColour(int colour, EntityMetaDataPacket template) {
            return new Marker(this.id, this.spawn, colour, new EntityMetaDataPacket(this.id, template.entries()),
                    this.boxId, this.boxSpawn, this.boxLook);
        }
    }

    /**
     * The packets of one group's label, built once.
     *
     * @param centre  the middle of the group, which decides whether the label is in reach
     * @param id      the label's entity id
     * @param spawn   puts the label above the group
     * @param text    the text it shows
     * @param crowded whether the text is red
     * @param look    gives it its text
     */
    private record Label(Pos centre, int id, SpawnEntityPacket spawn, String text, boolean crowded,
                         EntityMetaDataPacket look) {
    }
}
