package net.onelitefeather.cygnus.noise;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the static the slender hears while the survivors take his pages away.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.14.0
 */
class SlenderStaticServiceTest extends CygnusPlayerTestBase {

    /** How many pages a round needs in every test that does not say otherwise. */
    private static final int PAGES = 4;

    /** Seconds between two bursts while no page has been found yet. */
    private static final int QUIET_INTERVAL = 8;

    /** Seconds between two bursts once every page is gone. */
    private static final int FRANTIC_INTERVAL = 2;

    private static final float MIN_VOLUME = 0.2F;
    private static final float MAX_VOLUME = 0.8F;

    @Test
    @DisplayName("Before a page is found the static comes in the long interval")
    void beforeAPageIsFoundTheStaticComesSlowly(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance);
        SlenderStaticService service = service(config(true), () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        Collector<EntitySoundEffectPacket> beforeTheGapIsUp = connection.trackIncoming(EntitySoundEffectPacket.class);

        tick(service, QUIET_INTERVAL - 1);
        // A collector stops tracking the moment it is read, so the second half of the test needs
        // one of its own.
        beforeTheGapIsUp.assertEmpty();
        Collector<EntitySoundEffectPacket> onTheLastSecond = connection.trackIncoming(EntitySoundEffectPacket.class);
        tick(service, 1);

        onTheLastSecond.assertSingle(packet ->
                assertEquals(slender.getEntityId(), packet.entityId(), "the static sits in the slender's own head"));

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Every found page shortens the gap between two bursts")
    void everyFoundPageShortensTheGap(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance);
        SlenderStaticService service = service(config(true), () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        tick(service, FRANTIC_INTERVAL);

        sounds.assertSingle(packet ->
                assertEquals(MAX_VOLUME, packet.volume(), "the last page has to be as loud as it gets"));

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A found page hits the slender with a burst right away")
    void aFoundPageBurstsImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance);
        SlenderStaticService service = service(config(true), () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));

        sounds.assertSingle(packet -> assertTrue(packet.volume() > MIN_VOLUME,
                "the burst has to stand out against the carpet it interrupts"));

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A burst restarts the gap, so it never lands on top of the carpet")
    void aBurstRestartsTheGap(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance);
        SlenderStaticService service = service(config(true), () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        tick(service, QUIET_INTERVAL - 1);
        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        tick(service, 1);

        sounds.assertEmpty();

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Nobody but the slender hears the static")
    void nobodyElseHearsTheStatic(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance);
        TestConnection survivorConnection = env.createConnection();
        survivorConnection.connect(instance);
        SlenderStaticService service = service(config(true), () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        Collector<EntitySoundEffectPacket> survivorSounds =
                survivorConnection.trackIncoming(EntitySoundEffectPacket.class);

        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));
        tick(service, QUIET_INTERVAL);

        survivorSounds.assertEmpty();

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A turned off feature stays silent")
    void aTurnedOffFeatureStaysSilent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance);
        SlenderStaticService service = service(config(false), () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));
        tick(service, QUIET_INTERVAL);

        sounds.assertEmpty();
        assertFalse(service.isRunning(), "a turned off feature must not even schedule a task");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The end of the round stops the static and forgets the progress")
    void theEndOfTheRoundStopsTheStatic(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance);
        SlenderStaticService service = service(config(true), () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));
        assertFalse(service.isRunning(), "the task has to be gone once the round is over");

        EventDispatcher.call(new GameStartEvent());
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);
        tick(service, FRANTIC_INTERVAL);

        sounds.assertEmpty();

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Without a slender there is nobody to play to")
    void withoutASlenderNothingIsPlayed(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        connection.connect(instance);
        SlenderStaticService service = service(config(true), () -> null);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        tick(service, QUIET_INTERVAL);

        sounds.assertEmpty();

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A resource pack sound is sent as named instead of being dropped")
    void aResourcePackSoundIsSentAsNamed(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance);
        Key packSound = Key.key("cygnus", "vhs_static");
        GameConfig config = GameConfig.builder()
                .slenderStaticEnabled(true)
                .slenderStaticSound(packSound)
                .slenderStaticQuietInterval(QUIET_INTERVAL)
                .slenderStaticFranticInterval(FRANTIC_INTERVAL)
                .slenderStaticMinVolume(MIN_VOLUME)
                .slenderStaticMaxVolume(MAX_VOLUME)
                .build();
        SlenderStaticService service = service(config, () -> slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));

        sounds.assertSingle(packet -> assertEquals(packSound, packet.soundEvent().key(),
                "a sound that only exists in the resource pack must reach the client under its own name"));

        env.destroyInstance(instance, true);
    }

    /**
     * Runs the service's second by hand, so a test does not have to wait for the scheduler.
     *
     * @param service the service under test
     * @param seconds how many seconds to run
     */
    private static void tick(SlenderStaticService service, int seconds) {
        for (int i = 0; i < seconds; i++) {
            service.tick();
        }
    }

    /**
     * Builds a service for the given slender.
     *
     * @param config  the configuration to run with
     * @param slender supplies the current slender
     * @return the service under test
     */
    private static SlenderStaticService service(GameConfig config, Supplier<Player> slender) {
        return new SlenderStaticService(config, slender);
    }

    /**
     * Builds a configuration that only says something about the static.
     *
     * @param enabled whether the static is on
     * @return the configuration
     */
    private static GameConfig config(boolean enabled) {
        return GameConfig.builder()
                .slenderStaticEnabled(enabled)
                .slenderStaticSound(Key.key("weather.rain"))
                .slenderStaticQuietInterval(QUIET_INTERVAL)
                .slenderStaticFranticInterval(FRANTIC_INTERVAL)
                .slenderStaticMinVolume(MIN_VOLUME)
                .slenderStaticMaxVolume(MAX_VOLUME)
                .build();
    }
}
