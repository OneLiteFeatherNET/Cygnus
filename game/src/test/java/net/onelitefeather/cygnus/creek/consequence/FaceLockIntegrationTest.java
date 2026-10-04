package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.FacePlayerPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceLockIntegrationTest extends CygnusPlayerTestBase {

    private static final Pos TARGET = new Pos(0, 42.5, 5);

    @Test
    @DisplayName("The head is held on the target every tick, and let go once the time is up")
    void holdsTheHeadForItsTime(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<FacePlayerPacket> faces = connection.trackIncoming(FacePlayerPacket.class);
        FaceLock lock = new FaceLock();

        lock.lock(player, () -> TARGET, 10);
        for (int tick = 0; tick < 20; tick++) {
            env.tick();
        }

        assertEquals(10, faces.collect().size());
    }

    @Test
    @DisplayName("Cleaning up lets go of the head at once")
    void cleanUpLetsGo(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        FaceLock lock = new FaceLock();
        lock.lock(player, () -> TARGET, 40);
        env.tick();
        Collector<FacePlayerPacket> faces = connection.trackIncoming(FacePlayerPacket.class);

        lock.cleanUp();
        for (int tick = 0; tick < 5; tick++) {
            env.tick();
        }

        assertTrue(faces.collect().isEmpty());
    }
}
