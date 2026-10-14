package net.onelitefeather.cygnus.setup.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MicrotusExtension.class)
class PagePreviewTest {

    private static final Pos BUILDER = new Pos(0, 40, 0);
    private static final PageResource NEAR = new PageResource(new Pos(10, 40, 0), Direction.NORTH);
    private static final PageResource FAR = new PageResource(new Pos(100, 40, 0), Direction.NORTH);

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

        spawns.assertSingle(packet -> assertEquals(EntityType.ITEM_DISPLAY, packet.type()));
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
        destroys.assertSingle(packet -> assertEquals(1, packet.entityIds().size()));

        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        this.builder.teleport(BUILDER).join();
        this.preview.update();
        spawns.assertSingle(packet -> assertEquals(EntityType.ITEM_DISPLAY, packet.type()));
    }

    @Test
    @DisplayName("Hiding the preview takes every display away in one go")
    void hideRemovesEverything() {
        this.spots.addAll(List.of(NEAR, new PageResource(new Pos(12, 40, 0), Direction.SOUTH)));
        this.preview.show();
        Collector<DestroyEntitiesPacket> destroys = this.connection.trackIncoming(DestroyEntitiesPacket.class);

        this.preview.hide();

        destroys.assertSingle(packet -> assertEquals(2, packet.entityIds().size()));
    }

    @Test
    @DisplayName("A new spot shows up as soon as the preview is refreshed")
    void refreshShowsANewSpot() {
        this.preview.show();
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);

        this.spots.add(NEAR);
        this.preview.refresh();

        spawns.assertSingle(packet -> assertEquals(EntityType.ITEM_DISPLAY, packet.type()));
    }

    @Test
    @DisplayName("A crowded spot glows red, any other green")
    void colourFollowsTheCrowd() {
        assertEquals(PagePreview.CROWDED_COLOUR, PagePreview.colourOf(0.5D));
        assertEquals(PagePreview.CALM_COLOUR, PagePreview.colourOf(1.0D));
    }
}
