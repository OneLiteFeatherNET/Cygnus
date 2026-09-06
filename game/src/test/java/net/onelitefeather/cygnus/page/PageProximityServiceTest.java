package net.onelitefeather.cygnus.page;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PageProximityService}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.12.0
 */
@ExtendWith(MicrotusExtension.class)
class PageProximityServiceTest {

    private static final Pos PLAYER_POS = new Pos(0, 64, 0);

    @Test
    void testANearbyPageIsAnnouncedAtItsOwnPosition(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        Pos page = new Pos(0, 64, 10);
        service(config(20), player, page).tick();

        sounds.assertSingle(packet -> {
            assertEquals(SoundEvent.BLOCK_AMETHYST_BLOCK_CHIME, packet.soundEvent());
            assertEquals(page.x(), packet.origin().x(), 0.001,
                    "the sound has to come from the page so the client can place it in 3D");
            assertEquals(page.z(), packet.origin().z(), 0.001);
        });

        env.destroyInstance(instance, true);
    }

    @Test
    void testAPageBeyondTheRangeStaysSilent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        service(config(20), player, new Pos(0, 64, 21)).tick();

        sounds.assertEmpty();
        env.destroyInstance(instance, true);
    }

    @ParameterizedTest
    @CsvSource({"8, 1.0", "16, 1.0", "32, 2.0", "48, 3.0"})
    void testVolumeCarriesTheSoundAcrossTheWholeRange(int range, float expectedVolume, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        service(config(range), player, new Pos(0, 64, 1)).tick();

        sounds.assertSingle(packet -> assertEquals(expectedVolume, packet.volume(), 0.001F,
                "a client only hears a sound 16 * volume blocks away, so the volume has to cover the range"));

        env.destroyInstance(instance, true);
    }

    @Test
    void testADisabledServiceNeverStartsItsTask(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        GameConfig config = GameConfig.builder()
                .pageProximityEnabled(false)
                .pageProximityRange(20)
                .pageProximityInterval(20)
                .pageProximitySound(GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND)
                .build();
        PageProximityService service = service(config, player, new Pos(0, 64, 1));

        service.startTask();

        assertFalse(service.isRunning(), "a service turned off in the config must not schedule anything");
        service.tick();
        sounds.assertEmpty();

        service.stopTask();
        env.destroyInstance(instance, true);
    }

    @Test
    void testAnUnknownSoundKeyFallsBackToTheDefaultChime(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        GameConfig config = GameConfig.builder()
                .pageProximityEnabled(true)
                .pageProximityRange(20)
                .pageProximityInterval(20)
                .pageProximitySound(Key.key("cygnus", "no_such_sound"))
                .build();

        service(config, player, new Pos(0, 64, 1)).tick();

        sounds.assertSingle(packet -> assertEquals(SoundEvent.BLOCK_AMETHYST_BLOCK_CHIME, packet.soundEvent(),
                "a key naming no known sound must not leave the hint silent"));

        env.destroyInstance(instance, true);
    }

    @Test
    void testTheTaskRunsWhileTheServiceIsEnabled(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);

        PageProximityService service = service(config(20), player, new Pos(0, 64, 1));
        service.startTask();
        assertTrue(service.isRunning());

        service.stopTask();
        assertFalse(service.isRunning());

        env.destroyInstance(instance, true);
    }

    private static GameConfig config(int range) {
        return GameConfig.builder()
                .pageProximityEnabled(true)
                .pageProximityRange(range)
                .pageProximityInterval(20)
                .pageProximitySound(GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND)
                .build();
    }

    private static PageProximityService service(GameConfig config, Player player, Pos... pages) {
        Collection<Player> listeners = List.of(player);
        List<Pos> positions = List.of(pages);
        return new PageProximityService(config, () -> listeners, () -> positions);
    }
}
