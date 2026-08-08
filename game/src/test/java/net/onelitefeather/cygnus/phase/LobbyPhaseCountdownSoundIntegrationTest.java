package net.onelitefeather.cygnus.phase;

import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests verifying that {@link LobbyPhase} drives the countdown horror sound
 * at the expected tick marks.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
class LobbyPhaseCountdownSoundIntegrationTest extends CygnusPlayerTestBase {

    @Test
    void testLobbyPhasePlaysCountdownSoundAtTenSeconds(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        GameConfig config = GameConfig.builder()
                .lobbyTime(30)
                .minPlayers(1)
                .gameTime(600)
                .maxPlayers(10)
                .build();

        LobbyPhase lobbyPhase = new LobbyPhase(config, () -> instance);

        TestConnection connection = env.createConnection();
        connection.connect(instance);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        lobbyPhase.setCurrentTicks(10);
        lobbyPhase.onUpdate();

        sounds.assertSingle(packet -> assertEquals(SoundEvent.BLOCK_NOTE_BLOCK_PLING, packet.soundEvent()));

        env.destroyInstance(instance, true);
    }

    @Test
    void testLobbyPhasePlaysNoCountdownSoundOutsideMarks(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        GameConfig config = GameConfig.builder()
                .lobbyTime(30)
                .minPlayers(1)
                .gameTime(600)
                .maxPlayers(10)
                .build();

        LobbyPhase lobbyPhase = new LobbyPhase(config, () -> instance);

        TestConnection connection = env.createConnection();
        connection.connect(instance);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        lobbyPhase.setCurrentTicks(8);
        lobbyPhase.onUpdate();

        sounds.assertEmpty();

        env.destroyInstance(instance, true);
    }
}
