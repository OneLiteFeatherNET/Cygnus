package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LureFigureIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("Only the lured survivor gets the figure, standing where it was put")
    void onlyTheSurvivorGetsTheFigure(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection survivorConn = env.createConnection();
        Player survivor = survivorConn.connect(instance, new Pos(0.5, 40, 0.5));
        TestConnection otherConn = env.createConnection();
        Player dead = otherConn.connect(instance, new Pos(2, 40, 2));
        DeadPlayerMannequin corpse = DeadPlayerMannequin.sleeping(dead);
        Collector<SpawnEntityPacket> survivorSpawns = survivorConn.trackIncoming(SpawnEntityPacket.class);
        Collector<SpawnEntityPacket> otherSpawns = otherConn.trackIncoming(SpawnEntityPacket.class);

        LureFigure figure = LureFigure.show(survivor, corpse, new Pos(20.5, 40, 0.5));

        List<SpawnEntityPacket> packets = survivorSpawns.collect();
        assertEquals(1, packets.size());
        assertEquals(EntityType.MANNEQUIN, packets.getFirst().type());
        assertTrue(otherSpawns.collect().isEmpty(), "nobody else may get the figure");
        assertEquals(dead.getUuid(), figure.identity());
        assertEquals(new Pos(20.5, 40, 0.5), figure.position());
    }

    @Test
    @DisplayName("Removing the figure twice destroys it once")
    void removeIsIdempotent(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection survivorConn = env.createConnection();
        Player survivor = survivorConn.connect(instance, new Pos(0.5, 40, 0.5));
        Player dead = env.createConnection().connect(instance, new Pos(2, 40, 2));
        LureFigure figure = LureFigure.show(survivor, DeadPlayerMannequin.sleeping(dead), new Pos(20.5, 40, 0.5));
        Collector<DestroyEntitiesPacket> destroys = survivorConn.trackIncoming(DestroyEntitiesPacket.class);

        figure.remove();
        figure.remove();

        assertEquals(1, destroys.collect().size());
    }
}
