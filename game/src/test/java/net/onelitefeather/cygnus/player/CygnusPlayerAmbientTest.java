package net.onelitefeather.cygnus.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the ambient sounds of a {@link CygnusPlayer} get more frequent the more scared the
 * player is.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
class CygnusPlayerAmbientTest extends CygnusPlayerTestBase {

    @Test
    void testCalmPlayerHearsNothingBeforeTheShortestCalmInterval(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, Pos.ZERO);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        // 300 ticks at full calm, at most 20 percent shorter.
        for (int i = 0; i < 239; i++) {
            player.tickAmbient(0.0D);
        }

        sounds.assertEmpty();
        env.destroyInstance(instance, true);
    }

    @Test
    void testTerrifiedPlayerHearsASoundWithinTheLongestTerrifiedInterval(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, Pos.ZERO);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        // 100 ticks at full fear, at most 20 percent longer.
        for (int i = 0; i < 120; i++) {
            player.tickAmbient(1.0D);
        }

        assertFalse(sounds.collect().isEmpty(), "a terrified player should have heard an ambient sound");
        env.destroyInstance(instance, true);
    }

    @Test
    void testCalmPlayerOnlyHearsTheCave(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, Pos.ZERO);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        for (int i = 0; i < 6000; i++) {
            player.tickAmbient(0.3D);
        }

        List<SoundEffectPacket> packets = sounds.collect();
        assertFalse(packets.isEmpty(), "the player should have heard something over five minutes");
        assertTrue(packets.stream().allMatch(packet -> packet.soundEvent() == SoundEvent.AMBIENT_CAVE),
                "below the dread threshold only the cave should be heard");
        env.destroyInstance(instance, true);
    }

    @Test
    void testTerrifiedPlayerAlsoHearsEerierSounds(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, Pos.ZERO);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        // Roughly 100 sounds, so missing every eerie one by chance is practically impossible.
        for (int i = 0; i < 10000; i++) {
            player.tickAmbient(1.0D);
        }

        assertTrue(sounds.collect().stream().anyMatch(packet -> packet.soundEvent() != SoundEvent.AMBIENT_CAVE),
                "a terrified player should also hear sounds other than the cave");
        env.destroyInstance(instance, true);
    }
}
