package net.onelitefeather.cygnus.phase.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class LobbyWaitingTaskIntegrationTest {

    @Test
    void testWaitingDisplayActionBarUpdates(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ActionBarPacket> packetCollector = connection.trackIncoming(ActionBarPacket.class);
        LobbyWaitingTask display = new LobbyWaitingTask(2);

        display.update(1);

        List<ActionBarPacket> packets = packetCollector.collect();
        assertFalse(packets.isEmpty(), "An ActionBarPacket should have been sent to the player.");

        Component lastSentComponent = packets.getLast().components().stream().findFirst().get();
        String plainText = PlainTextComponentSerializer.plainText().serialize(lastSentComponent);
        assertEquals("Need 1 players to start", plainText, "Action bar text should request 1 more player.");

        // 2. Send manual display update to the player
        display.send(player);

        List<ActionBarPacket> newPackets = packetCollector.collect();
        assertEquals(1, newPackets.size(), "An additional packet should be sent immediately.");

        display.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testWaitingDisplayStopPreventsTicking(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        connection.connect(instance);
        Collector<ActionBarPacket> packetCollector = connection.trackIncoming(ActionBarPacket.class);
        LobbyWaitingTask display = new LobbyWaitingTask(2);
        display.update(1);
        int packetsBeforeStop = packetCollector.collect().size();
        assertTrue(packetsBeforeStop > 0);
        display.stop();
        for (int i = 0; i < 50; i++) {
            env.tick();
        }
        int packetsAfterStop = packetCollector.collect().size();
        assertEquals(packetsBeforeStop, packetsAfterStop, "No more packets should be sent after stop() is called.");
        env.destroyInstance(instance, true);
    }
}
