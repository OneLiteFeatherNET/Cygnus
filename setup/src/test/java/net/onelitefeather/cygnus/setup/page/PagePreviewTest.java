package net.onelitefeather.cygnus.setup.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
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
    @DisplayName("A crowded group glows red, a lone spot grey, any other group in its own colour")
    void colourFollowsTheGroup() {
        PageGroups.Group crowded = new PageGroups.Group(List.of(NEAR, PAIRED), 0.5D);
        PageGroups.Group calm = new PageGroups.Group(List.of(NEAR, PAIRED), 0.1D);
        PageGroups.Group lone = new PageGroups.Group(List.of(FAR), 0.05D);

        assertEquals(PagePreview.CROWDED_COLOUR, PagePreview.colourOf(crowded, 0));
        assertEquals(PagePreview.LONE_COLOUR, PagePreview.colourOf(lone, 1));
        assertEquals(PagePreview.GROUP_COLOURS[0], PagePreview.colourOf(calm, 0));
        assertNotEquals(PagePreview.colourOf(calm, 0), PagePreview.colourOf(calm, 1),
                "groups next in line must not look alike");
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
