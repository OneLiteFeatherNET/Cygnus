package net.onelitefeather.cygnus.possession;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessionGlowTest extends CygnusPlayerTestBase {

    private static List<Boolean> glowOf(List<EntityMetaDataPacket> packets, Player survivor) {
        return packets.stream()
                .filter(packet -> packet.entityId() == survivor.getEntityId())
                .map(packet -> packet.entries().get(0).value() instanceof Byte flags && (flags & 0x40) != 0)
                .toList();
    }

    @Test
    @DisplayName("Survivors glow while they are in range and stop once they leave it")
    void followsTheRange(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance, new Pos(0, 40, 0));
        Player first = env.createPlayer(instance, new Pos(5, 40, 0));
        Player second = env.createPlayer(instance, new Pos(10, 40, 0));
        PossessionGlow glow = new PossessionGlow();
        Collector<EntityMetaDataPacket> packets = connection.trackIncoming(EntityMetaDataPacket.class);

        glow.update(slender, Set.of(first, second));
        glow.update(slender, Set.of(first));

        assertEquals(Set.of(first), glow.glowing());
        List<EntityMetaDataPacket> sent = packets.collect();
        assertEquals(List.of(true, false), glowOf(sent, second));
        assertEquals(List.of(true, true), glowOf(sent, first), "the glow is sent again, metadata updates would drop it");
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Clearing turns every glow off")
    void clearTurnsAllOff(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance, new Pos(0, 40, 0));
        Player survivor = env.createPlayer(instance, new Pos(5, 40, 0));
        PossessionGlow glow = new PossessionGlow();
        Collector<EntityMetaDataPacket> packets = connection.trackIncoming(EntityMetaDataPacket.class);

        glow.update(slender, Set.of(survivor));
        glow.clear(slender);

        assertTrue(glow.glowing().isEmpty());
        assertEquals(List.of(true, false), glowOf(packets.collect(), survivor));
        env.destroyInstance(instance, true);
    }
}
