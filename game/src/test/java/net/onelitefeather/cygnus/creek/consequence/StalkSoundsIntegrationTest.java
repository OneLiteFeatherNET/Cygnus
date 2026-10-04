package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StalkSoundsIntegrationTest extends CygnusPlayerTestBase {

    private static final double DELTA = 0.2D;

    @Test
    @DisplayName("From far away the sound comes from his direction, but no further than eight blocks")
    void farAwaySoundsFromHisDirection(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        new StalkSounds(new Random(1)).play(target, new Pos(0, 40, 30), 0.0D);

        List<SoundEffectPacket> sent = sounds.collect();
        assertEquals(1, sent.size());
        Point origin = sent.getFirst().origin();
        assertEquals(0.0D, origin.x(), DELTA);
        assertEquals(StalkSounds.MAX_DISTANCE, origin.z(), DELTA);
    }

    @Test
    @DisplayName("Close by the sound comes from where he stands")
    void closeBySoundsFromHim(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        new StalkSounds(new Random(1)).play(target, new Pos(3, 40, 4), 1.0D);

        Point origin = sounds.collect().getFirst().origin();
        assertEquals(3.0D, origin.x(), DELTA);
        assertEquals(4.0D, origin.z(), DELTA);
    }

    @Test
    @DisplayName("Late in the stalk he is louder, and nobody but the target hears him")
    void louderLateAndOnlyForTheTarget(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, new Pos(0, 40, 0));
        TestConnection otherConnection = env.createConnection();
        otherConnection.connect(instance, new Pos(2, 40, 0));
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);
        Collector<SoundEffectPacket> others = otherConnection.trackIncoming(SoundEffectPacket.class);
        StalkSounds stalk = new StalkSounds(new Random(1));

        stalk.play(target, new Pos(0, 40, 30), 0.0D);
        stalk.play(target, new Pos(0, 40, 30), 1.0D);

        List<SoundEffectPacket> sent = sounds.collect();
        assertTrue(sent.get(1).volume() > sent.get(0).volume());
        assertTrue(others.collect().isEmpty());
    }
}
