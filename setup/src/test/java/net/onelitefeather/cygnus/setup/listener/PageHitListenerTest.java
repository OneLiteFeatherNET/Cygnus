package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.client.play.ClientAttackPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.guira.SetupDataService;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageHitListenerTest {

    private Instance instance;
    private TestConnection connection;
    private Player builder;
    private GameData data;
    private PageHitListener listener;
    private final List<PlayerRemoveDataEvent> asked = new ArrayList<>();
    private final EventNode<Event> node = EventNode.all("page-hit-test");

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
        this.connection = env.createConnection();
        this.builder = this.connection.connect(this.instance, new Pos(0, 40, 0));
        this.data = new GameData(this.builder, MapEntry.of(Paths.get("")));
        SetupDataService service = SetupDataService.create();
        service.add(this.builder.getUuid(), this.data);
        this.listener = new PageHitListener(service);
        this.node.addListener(PlayerRemoveDataEvent.class, this.asked::add);
        env.process().eventHandler().addChild(this.node);
    }

    @AfterEach
    void tearDown(Env env) {
        env.process().eventHandler().removeChild(this.node);
        this.data.reset();
        env.destroyInstance(this.instance, true);
    }

    @Test
    @DisplayName("Hitting a page of the preview asks to delete it and keeps the hit from going anywhere else")
    void hittingAPageAsksToDeleteIt() {
        Collector<SpawnEntityPacket> spawns = this.connection.trackIncoming(SpawnEntityPacket.class);
        this.data.swapPageMode();
        this.data.selectPagePool("default");
        this.data.addPage(new Vec(10, 40, 0), Direction.NORTH);
        int box = spawns.collect().stream().filter(packet -> packet.type() == EntityType.INTERACTION)
                .findFirst().orElseThrow().entityId();
        PlayerPacketEvent event = new PlayerPacketEvent(this.builder, new ClientAttackPacket(box));

        this.listener.accept(event);

        assertEquals(1, this.asked.size());
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("Hitting anything else is left alone")
    void hittingSomethingElseIsLeftAlone() {
        this.data.swapPageMode();
        PlayerPacketEvent event = new PlayerPacketEvent(this.builder, new ClientAttackPacket(-1));

        this.listener.accept(event);

        assertTrue(this.asked.isEmpty());
        assertFalse(event.isCancelled());
    }
}
