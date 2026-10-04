package net.onelitefeather.cygnus.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathStrikeTest extends CygnusPlayerTestBase {

    private static final Pos WHERE = new Pos(0, 40, 0);

    static List<Entity> bolts(Instance instance) {
        return instance.getEntities().stream()
                .filter(entity -> entity.getEntityType() == EntityType.LIGHTNING_BOLT)
                .toList();
    }

    @Test
    @DisplayName("Lightning strikes where they died and is gone after a second")
    void boltComesAndGoes(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(5, 40, 0));

        new DeathStrike().strike(instance, WHERE);

        List<Entity> bolts = bolts(instance);
        assertEquals(1, bolts.size());
        assertEquals(WHERE, bolts.getFirst().getPosition());

        for (int tick = 0; tick <= DeathStrike.BOLT_TICKS; tick++) {
            env.tick();
        }
        assertTrue(bolts(instance).isEmpty());
    }

    @Test
    @DisplayName("The thunder reaches even someone far away")
    void thunderReachesEveryone(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection farConnection = env.createConnection();
        farConnection.connect(instance, new Pos(300, 40, 0));
        Collector<SoundEffectPacket> sounds = farConnection.trackIncoming(SoundEffectPacket.class);

        new DeathStrike().strike(instance, WHERE);

        List<SoundEffectPacket> sent = sounds.collect();
        assertEquals(1, sent.size());
        assertEquals(DeathStrike.THUNDER, sent.getFirst().soundEvent().key());
        assertTrue(sent.getFirst().volume() >= DeathStrike.THUNDER_VOLUME);
    }
}
