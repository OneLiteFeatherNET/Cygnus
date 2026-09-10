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
import net.onelitefeather.cygnus.common.page.PageProximityTarget;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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

        // Factor 1 on purpose: this case is about the derivation from the range alone.
        service(config(range, 1.0F), player, new Pos(0, 64, 1)).tick();

        sounds.assertSingle(packet -> assertEquals(expectedVolume, packet.volume(), 0.001F,
                "a client only hears a sound 16 * volume blocks away, so the volume has to cover the range"));

        env.destroyInstance(instance, true);
    }

    @ParameterizedTest
    @CsvSource({"20, 1.0, 1.25", "20, 2.0, 2.5", "20, 4.0, 5.0", "8, 2.0, 2.0"})
    void testTheVolumeFactorStretchesTheFalloff(int range, float factor, float expectedVolume, @NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        service(config(range, factor), player, new Pos(0, 64, 1)).tick();

        sounds.assertSingle(packet -> assertEquals(expectedVolume, packet.volume(), 0.001F,
                "the factor has to reach the played sound, not just be stored"));

        env.destroyInstance(instance, true);
    }

    /**
     * The shipped default has to stretch the falloff, not merely cover the range. A factor of 1
     * would put the chime's own silence at the range's edge, which is the bug this guards.
     */
    @Test
    void testTheDefaultFactorPlaysLouderThanTheRangeAloneWouldNeed(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        service(config(20), player, new Pos(0, 64, 1)).tick();

        float rangeAlone = 20 / 16.0F;
        sounds.assertSingle(packet -> assertTrue(packet.volume() > rangeAlone,
                "the default has to reach past the range, got " + packet.volume()));

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

    /** A configuration on the shipped volume factor. */
    private static GameConfig config(int range) {
        return GameConfig.builder()
                .pageProximityEnabled(true)
                .pageProximityRange(range)
                .pageProximityInterval(20)
                .pageProximitySound(GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND)
                .build();
    }

    private static GameConfig config(int range, float volumeFactor) {
        return GameConfig.builder()
                .pageProximityEnabled(true)
                .pageProximityRange(range)
                .pageProximityInterval(20)
                .pageProximitySound(GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND)
                .pageProximityVolumeFactor(volumeFactor)
                .build();
    }

    @Test
    void testPhase1SilentAboveFiftyPercentTtl(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        PageProximityTarget target = PageProximityTarget.of(UUID.randomUUID(), new Pos(0, 64, 5), 0.75);
        PageProximityService service = new PageProximityService(config(20), () -> List.of(player), () -> List.of(target));
        service.tick();

        sounds.assertEmpty();
        env.destroyInstance(instance, true);
    }

    @Test
    void testPhase2WarningChimeBetweenTwentyAndFiftyPercent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        PageProximityTarget target = PageProximityTarget.of(UUID.randomUUID(), new Pos(0, 64, 5), 0.35);
        PageProximityService service = new PageProximityService(config(20), () -> List.of(player), () -> List.of(target));
        service.tick();

        sounds.assertSingle(packet -> {
            assertEquals(PageProximityService.WARNING_PITCH, packet.pitch(), 0.001F);
            assertEquals(SoundEvent.BLOCK_AMETHYST_BLOCK_CHIME, packet.soundEvent());
        });
        env.destroyInstance(instance, true);
    }

    @Test
    void testPhase3CriticalChimeBelowTwentyPercent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        PageProximityTarget target = PageProximityTarget.of(UUID.randomUUID(), new Pos(0, 64, 5), 0.15);
        PageProximityService service = new PageProximityService(config(20), () -> List.of(player), () -> List.of(target));
        service.tick();

        sounds.assertSingle(packet -> {
            assertEquals(PageProximityService.CRITICAL_PITCH, packet.pitch(), 0.001F);
            float baseVolume = (20 / 16.0F) * GameConfig.DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR;
            assertEquals(baseVolume * PageProximityService.CRITICAL_VOLUME_MULTIPLIER, packet.volume(), 0.001F);
        });
        env.destroyInstance(instance, true);
    }

    @Test
    void testChimeIntervalThrottling(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, PLAYER_POS);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        PageProximityTarget target = PageProximityTarget.of(UUID.randomUUID(), new Pos(0, 64, 5), 0.35);
        PageProximityService service = new PageProximityService(config(20), () -> List.of(player), () -> List.of(target));

        service.tick(); // First tick: triggers chime
        service.tick(); // Immediate next tick (5 ticks later): throttled by 60-tick warning interval!

        assertEquals(1, sounds.collect().size(), "second tick within interval must be throttled");
        env.destroyInstance(instance, true);
    }

    private static PageProximityService service(GameConfig config, Player player, Pos... pages) {
        Collection<Player> listeners = List.of(player);
        List<PageProximityTarget> targets = Arrays.stream(pages)
                .map(pos -> PageProximityTarget.of(UUID.nameUUIDFromBytes(pos.toString().getBytes()), pos, 0.35))
                .toList();
        return new PageProximityService(config, () -> listeners, () -> targets);
    }
}
