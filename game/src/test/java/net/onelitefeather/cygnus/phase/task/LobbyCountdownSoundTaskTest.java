package net.onelitefeather.cygnus.phase.task;

import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LobbyCountdownSoundTask}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@ExtendWith(MicrotusExtension.class)
class LobbyCountdownSoundTaskTest {

    @ParameterizedTest
    @CsvSource({
            "10, 1.000, 0.5",
            "5,  0.944, 0.6",
            "3,  0.667, 0.7",
            "2,  0.630, 0.75",
            "1,  0.561, 0.85",
            "0,  0.500, 0.9"
    })
    void testOnTickPlaysExpectedNoteAtMark(int currentTicks, float expectedPitch, float expectedVolume, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        connection.connect(instance);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        new LobbyCountdownSoundTask().onTick(currentTicks);

        sounds.assertSingle(packet -> {
            assertEquals(SoundEvent.BLOCK_NOTE_BLOCK_PLING, packet.soundEvent());
            assertEquals(expectedPitch, packet.pitch(), 0.001F);
            assertEquals(expectedVolume, packet.volume(), 0.001F);
        });

        env.destroyInstance(instance, true);
    }

    @ParameterizedTest
    @CsvSource({"9", "8", "7", "6", "4", "-1", "11"})
    void testOnTickDoesNothingOutsideMarks(int currentTicks, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        connection.connect(instance);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        new LobbyCountdownSoundTask().onTick(currentTicks);

        sounds.assertEmpty();
        env.destroyInstance(instance, true);
    }

    @Test
    void testResetDoesNotThrow() {
        new LobbyCountdownSoundTask().reset();
    }
}
