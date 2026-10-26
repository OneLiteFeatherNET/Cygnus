package net.onelitefeather.cygnus.setup.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PagePreviewTest {

    private static final Pos BUILDER = new Pos(0, 40, 0);
    private static final PageResource NEAR = new PageResource(new Pos(10, 40, 0), Direction.NORTH);
    private static final PageResource FAR = new PageResource(new Pos(100, 40, 0), Direction.NORTH);
    private static final PageResource PAIRED = new PageResource(new Pos(12, 40, 0), Direction.SOUTH);
    private static final PageResource OTHER = new PageResource(new Pos(-30, 40, 0), Direction.NORTH);
    private static final PageResource OTHER_PAIRED = new PageResource(new Pos(-32, 40, 0), Direction.NORTH);
    private static final PageResource LONE = new PageResource(new Pos(0, 40, 30), Direction.EAST);

    private Instance instance;
    private TestConnection connection;
    private Player builder;
    private final List<PageResource> spots = new ArrayList<>();
    private PagePreview preview;

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
        this.connection = env.createConnection();
        this.builder = this.connection.connect(this.instance, BUILDER);
        this.preview = new PagePreview(this.builder, () -> this.spots);
    }

    @AfterEach
    void tearDown(Env env) {
        this.preview.hide();
        env.destroyInstance(this.instance, true);
    }

    @Test
    @DisplayName("A spot within reach gets a page display, one far away does not")
    void showsOnlySpotsWithinReach() {
        this.spots.addAll(List.of(NEAR, FAR));
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);

        this.preview.show();

        assertEquals(List.of(EntityType.ITEM_DISPLAY, EntityType.INTERACTION), types(spawns),
                "the near spot gets its page and the box to click it, the far one nothing");
    }

    @Test
    @DisplayName("Another player in the setup sees none of the displays")
    void otherPlayersSeeNothing(Env env) {
        TestConnection otherConnection = env.createConnection();
        otherConnection.connect(this.instance, BUILDER);
        this.spots.add(NEAR);
        Collector<SpawnEntityPacket> spawns = otherConnection.trackIncoming(SpawnEntityPacket.class);

        this.preview.show();

        spawns.assertEmpty();
    }

    @Test
    @DisplayName("A spot already shown is not sent again")
    void aShownSpotIsNotSentAgain() {
        this.spots.add(NEAR);
        this.preview.show();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);

        this.preview.update();

        spawns.assertEmpty();
    }

    @Test
    @DisplayName("Walking away takes the display away, coming back brings it back")
    void followsTheBuilder() {
        this.spots.add(NEAR);
        this.preview.show();
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);

        this.builder.teleport(new Pos(-100, 40, 0)).join();
        this.preview.update();
        destroys.assertSingle(packet -> assertEquals(2, packet.entityIds().size()));

        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        this.builder.teleport(BUILDER).join();
        this.preview.update();
        assertEquals(List.of(EntityType.ITEM_DISPLAY, EntityType.INTERACTION), types(spawns));
    }

    @Test
    @DisplayName("Hiding the preview takes every display away in one go")
    void hideRemovesEverything() {
        this.spots.addAll(List.of(NEAR, PAIRED));
        this.preview.show();
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);

        this.preview.hide();

        destroys.assertSingle(packet -> assertEquals(5, packet.entityIds().size(), "two pages, two boxes and the label"));
    }

    @Test
    @DisplayName("A new spot shows up as soon as the preview is refreshed")
    void refreshShowsANewSpot() {
        this.preview.show();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);

        this.spots.add(NEAR);
        this.preview.refresh();

        assertEquals(List.of(EntityType.ITEM_DISPLAY, EntityType.INTERACTION), types(spawns));
    }

    @Test
    @DisplayName("Hitting a page asks to delete its spot")
    void hittingAPageAsksToDeleteIt(Env env) {
        this.spots.add(NEAR);
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        this.preview.show();
        int box = spawns.collect().stream().filter(packet -> packet.type() == EntityType.INTERACTION)
                .findFirst().orElseThrow().entityId();
        List<PlayerRemoveDataEvent> asked = new ArrayList<>();
        EventNode<Event> node = EventNode.all("page-preview-test");
        node.addListener(PlayerRemoveDataEvent.class, asked::add);
        env.process().eventHandler().addChild(node);

        assertTrue(this.preview.hit(box));

        env.process().eventHandler().removeChild(node);
        assertEquals(1, asked.size());
        assertEquals(MapDataCategory.PAGE, asked.getFirst().getMapDataCategory());
        assertEquals(new DialogContext.PageContent(NEAR, false), asked.getFirst().getContext(),
                "a hit in the world must not send the builder back to the page list");
    }

    @Test
    @DisplayName("Hitting anything that is not a shown page does nothing")
    void hittingSomethingElseDoesNothing() {
        this.spots.add(NEAR);
        this.preview.show();

        assertFalse(this.preview.hit(-1));
    }

    @Test
    @DisplayName("A hidden preview has nothing to hit")
    void aHiddenPreviewHasNothingToHit() {
        this.spots.add(NEAR);
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        this.preview.show();
        int box = spawns.collect().stream().filter(packet -> packet.type() == EntityType.INTERACTION)
                .findFirst().orElseThrow().entityId();

        this.preview.hide();

        assertFalse(this.preview.hit(box));
    }

    private static List<EntityType> types(Collector<SpawnEntityPacket> spawns) {
        return spawns.collect().stream().map(SpawnEntityPacket::type).toList();
    }

    @Test
    @DisplayName("A crowded group glows red, a lone spot grey, other groups in colours of their own")
    void colourFollowsTheGroup() {
        PageGroups.Group calm = new PageGroups.Group(List.of(NEAR, PAIRED), 0.1D);
        PageGroups.Group otherCalm = new PageGroups.Group(List.of(OTHER, OTHER_PAIRED), 0.1D);
        PageGroups.Group lone = new PageGroups.Group(List.of(FAR), 0.05D);

        int[] colours = PagePreview.colours(List.of(calm, otherCalm, lone), Map.of());

        assertNotEquals(colours[0], colours[1], "two groups must not look alike");
        assertTrue(palette().containsAll(List.of(colours[0], colours[1])));
        assertEquals(PagePreview.LONE_COLOUR, colours[2]);
        assertEquals(PagePreview.CROWDED_COLOUR, PagePreview.colours(
                List.of(new PageGroups.Group(List.of(NEAR, PAIRED), 0.5D)), Map.of())[0]);
    }

    @Test
    @DisplayName("A group keeps the colour its spots had before")
    void aGroupKeepsItsColour() {
        int kept = PagePreview.GROUP_COLOURS[3];
        PageGroups.Group calm = new PageGroups.Group(List.of(NEAR, PAIRED), 0.1D);

        assertEquals(kept, PagePreview.colours(List.of(calm), Map.of(NEAR, kept, PAIRED, kept))[0]);
    }

    @Test
    @DisplayName("Two groups that had the same colour do not keep it both")
    void aKeptColourGoesToOneGroupOnly() {
        int kept = PagePreview.GROUP_COLOURS[3];
        PageGroups.Group calm = new PageGroups.Group(List.of(NEAR, PAIRED), 0.1D);
        PageGroups.Group otherCalm = new PageGroups.Group(List.of(OTHER, OTHER_PAIRED), 0.1D);

        int[] colours = PagePreview.colours(List.of(calm, otherCalm),
                Map.of(NEAR, kept, PAIRED, kept, OTHER, kept, OTHER_PAIRED, kept));

        assertEquals(kept, colours[0], "the larger group, which comes first, keeps it");
        assertNotEquals(kept, colours[1]);
    }

    @Test
    @DisplayName("A new spot is sent on its own, nothing shown is taken away")
    void refreshSendsOnlyTheNewSpot() {
        this.spots.addAll(List.of(NEAR, OTHER));
        this.preview.show();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);

        this.spots.add(LONE);
        this.preview.refresh();

        assertEquals(List.of(EntityType.ITEM_DISPLAY, EntityType.INTERACTION), types(spawns));
        destroys.assertEmpty();
    }

    @Test
    @DisplayName("A spot that joins a group only recolours its neighbour, it is not spawned again")
    void joiningAGroupOnlyRecolours() {
        this.spots.add(NEAR);
        Collector<SpawnEntityPacket> first = this.connection.trackIncoming(SpawnEntityPacket.class);
        this.preview.show();
        int nearId = first.collect().getFirst().entityId();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);
        Collector<EntityMetaDataPacket> looks = this.connection.trackIncoming(EntityMetaDataPacket.class);

        this.spots.add(PAIRED);
        this.preview.refresh();

        destroys.assertEmpty();
        assertEquals(List.of(EntityType.ITEM_DISPLAY, EntityType.INTERACTION, EntityType.TEXT_DISPLAY), types(spawns));
        assertTrue(looks.collect().stream().anyMatch(packet -> packet.entityId() == nearId),
                "the lone spot turned into a group and needs its new glow");
    }

    @Test
    @DisplayName("A deleted spot takes only its own displays away")
    void refreshRemovesOnlyTheDeletedSpot() {
        this.spots.addAll(List.of(NEAR, PAIRED, OTHER));
        this.preview.show();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);

        this.spots.remove(OTHER);
        this.preview.refresh();

        spawns.assertEmpty();
        destroys.assertSingle(packet -> assertEquals(2, packet.entityIds().size(), "the page and its box"));
    }

    @Test
    @DisplayName("A label whose group grows moves along instead of being spawned again")
    void aGrowingGroupMovesItsLabel() {
        this.spots.addAll(List.of(NEAR, PAIRED));
        Collector<SpawnEntityPacket> first = this.connection.trackIncoming(SpawnEntityPacket.class);
        this.preview.show();
        int labelId = first.collect().stream().filter(packet -> packet.type() == EntityType.TEXT_DISPLAY)
                .findFirst().orElseThrow().entityId();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);
        Collector<EntityTeleportPacket> moves = this.connection.trackIncoming(EntityTeleportPacket.class);

        this.spots.add(new PageResource(new Pos(14, 40, 0), Direction.NORTH));
        this.preview.refresh();

        destroys.assertEmpty();
        assertEquals(List.of(EntityType.ITEM_DISPLAY, EntityType.INTERACTION), types(spawns),
                "only the new page and its box, the label is not spawned again");
        moves.assertSingle(packet -> {
            assertEquals(labelId, packet.entityId());
            assertEquals(new Pos(12, 40 + PagePreview.LABEL_HEIGHT, 0), packet.position());
        });
    }

    @Test
    @DisplayName("Nothing is sent when a refresh changes nothing")
    void refreshWithoutChangeSendsNothing() {
        this.spots.addAll(List.of(NEAR, PAIRED, OTHER));
        this.preview.show();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);
        Collector<EntityMetaDataPacket> looks = this.connection.trackIncoming(EntityMetaDataPacket.class);

        this.preview.refresh();

        spawns.assertEmpty();
        destroys.assertEmpty();
        looks.assertEmpty();
    }

    private static List<Integer> palette() {
        return java.util.Arrays.stream(PagePreview.GROUP_COLOURS).boxed().toList();
    }

    @Test
    @DisplayName("The label of a group tells its size and share")
    void labelTellsSizeAndShare() {
        assertEquals("2 pages · 38%", PagePreview.labelText(new PageGroups.Group(List.of(NEAR, PAIRED), 0.375D)));
    }

    @Test
    @DisplayName("A group of spots gets a label above it, a lone spot none")
    void groupGetsALabel() {
        this.spots.addAll(List.of(NEAR, PAIRED));
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);

        this.preview.show();

        List<EntityType> types = types(spawns);
        assertEquals(1, types.stream().filter(type -> type == EntityType.TEXT_DISPLAY).count());
        assertEquals(5, types.size(), "two pages, two boxes and one label");
    }

    @Test
    @DisplayName("Walking away from a group takes its label away too")
    void labelFollowsTheBuilder() {
        this.spots.addAll(List.of(NEAR, PAIRED));
        this.preview.show();
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);

        this.builder.teleport(new Pos(-100, 40, 0)).join();
        this.preview.update();

        assertEquals(5, destroys.collect().stream().mapToInt(packet -> packet.entityIds().size()).sum());
    }
}
