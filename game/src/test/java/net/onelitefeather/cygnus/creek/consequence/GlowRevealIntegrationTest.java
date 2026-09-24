package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlowRevealIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The slender sees the survivor glow at once")
    void slenderSeesTheGlow(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        Collector<EntityMetaDataPacket> packets = slenderConnection.trackIncoming(EntityMetaDataPacket.class);
        GlowReveal reveal = new GlowReveal(6);

        reveal.reveal(survivor, slender);

        assertTrue(packets.collect().stream().anyMatch(packet -> glows(packet, survivor)));
        reveal.cleanUp();
    }

    @Test
    @DisplayName("Sprinting wipes the glow only until the next resend")
    void glowComesBackAfterSprinting(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        GlowReveal reveal = new GlowReveal(6);
        reveal.reveal(survivor, slender);
        Collector<EntityMetaDataPacket> packets = slenderConnection.trackIncoming(EntityMetaDataPacket.class);

        survivor.setSprinting(true);
        for (int tick = 0; tick < GlowReveal.RESEND_TICKS + 1; tick++) {
            env.tick();
        }

        List<EntityMetaDataPacket> sent = packets.collect().stream()
                .filter(packet -> packet.entityId() == survivor.getEntityId() && packet.entries().containsKey(0))
                .toList();
        assertFalse(sent.isEmpty());
        assertTrue(glows(sent.getLast(), survivor), "the resend has to bring the glow back");
        reveal.cleanUp();
    }

    @Test
    @DisplayName("Cleaning up takes the glow back")
    void cleanUpTakesTheGlowBack(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        GlowReveal reveal = new GlowReveal(6);
        reveal.reveal(survivor, slender);
        Collector<EntityMetaDataPacket> packets = slenderConnection.trackIncoming(EntityMetaDataPacket.class);

        reveal.cleanUp();

        List<EntityMetaDataPacket> sent = packets.collect();
        assertFalse(sent.isEmpty());
        assertNotNull(sent.getLast().entries().get(0), "the flags have to be sent explicitly");
        assertFalse(glows(sent.getLast(), survivor));
    }

    @Test
    @DisplayName("The survivor is warned")
    void survivorIsWarned(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));
        Collector<SystemChatPacket> messages = survivorConnection.trackIncoming(SystemChatPacket.class);
        GlowReveal reveal = new GlowReveal(6);

        reveal.reveal(survivor, slender);

        assertFalse(messages.collect().isEmpty());
        reveal.cleanUp();
    }

    private static boolean glows(EntityMetaDataPacket packet, Player survivor) {
        if (packet.entityId() != survivor.getEntityId()) return false;
        Metadata.Entry<?> flags = packet.entries().get(0);
        return flags != null && flags.value() instanceof Byte value && (value & GlowReveal.GLOWING) != 0;
    }
}
