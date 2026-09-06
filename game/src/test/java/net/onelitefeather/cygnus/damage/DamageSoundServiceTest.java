package net.onelitefeather.cygnus.damage;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the sound a player hears when the slender hits them.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.13.0
 */
class DamageSoundServiceTest extends CygnusPlayerTestBase {

    /** Where the hit came from. The service must not care - only the victim does. */
    private static final Pos SOURCE = new Pos(0, 40, 6);

    /** Ticks between two sounds in every test that does not test the cooldown itself. */
    private static final int COOLDOWN_TICKS = 20;

    /** The same cooldown expressed in the milliseconds the fake clock counts. */
    private static final long COOLDOWN_MILLIS = COOLDOWN_TICKS * 50L;

    @Test
    @DisplayName("A hit plays the configured sound to the player who was hit")
    void aHitPlaysTheSoundToTheVictim(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);
        DamageSoundService service = service(config(true, COOLDOWN_TICKS, Key.key("entity.player.hurt")), new AtomicLong());
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new PlayerDamagedEvent(player, SOURCE, 1.0F));

        sounds.assertSingle(packet -> {
            assertEquals(SoundEvent.ENTITY_PLAYER_HURT, packet.soundEvent());
            assertEquals(player.getEntityId(), packet.entityId(), "the sound has to sit on the victim, not on a location");
        });

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Nobody but the player who was hit hears the sound")
    void nobodyElseHearsTheSound(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection victimConnection = env.createConnection();
        Player victim = victimConnection.connect(instance);
        TestConnection bystanderConnection = env.createConnection();
        bystanderConnection.connect(instance);
        Collector<EntitySoundEffectPacket> bystanderSounds =
                bystanderConnection.trackIncoming(EntitySoundEffectPacket.class);
        DamageSoundService service = service(config(true, COOLDOWN_TICKS, GameConfig.DEFAULT_DAMAGE_SOUND), new AtomicLong());
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new PlayerDamagedEvent(victim, SOURCE, 1.0F));

        bystanderSounds.assertEmpty();

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A second hit inside the cooldown stays silent")
    void aSecondHitInsideTheCooldownStaysSilent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        AtomicLong clock = new AtomicLong();
        DamageSoundService service = service(config(true, COOLDOWN_TICKS, GameConfig.DEFAULT_DAMAGE_SOUND), clock);
        service.play(player);
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        clock.set(COOLDOWN_MILLIS - 1);
        service.play(player);

        sounds.assertEmpty();

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The sound comes back once the cooldown has passed")
    void theSoundComesBackAfterTheCooldown(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        AtomicLong clock = new AtomicLong();
        DamageSoundService service = service(config(true, COOLDOWN_TICKS, GameConfig.DEFAULT_DAMAGE_SOUND), clock);
        service.play(player);
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        clock.set(COOLDOWN_MILLIS);
        service.play(player);

        sounds.assertSingle(packet -> assertEquals(SoundEvent.ENTITY_PLAYER_HURT, packet.soundEvent()));

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The cooldown is kept per player")
    void theCooldownIsKeptPerPlayer(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection firstConnection = env.createConnection();
        Player first = firstConnection.connect(instance);
        TestConnection secondConnection = env.createConnection();
        Player second = secondConnection.connect(instance);
        DamageSoundService service = service(config(true, COOLDOWN_TICKS, GameConfig.DEFAULT_DAMAGE_SOUND), new AtomicLong());
        service.play(first);
        Collector<EntitySoundEffectPacket> secondSounds = secondConnection.trackIncoming(EntitySoundEffectPacket.class);

        service.play(second);

        secondSounds.assertSingle(packet -> assertEquals(SoundEvent.ENTITY_PLAYER_HURT, packet.soundEvent()));

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A turned off feature never plays anything")
    void aTurnedOffFeatureStaysSilent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);
        DamageSoundService service = service(config(false, COOLDOWN_TICKS, GameConfig.DEFAULT_DAMAGE_SOUND), new AtomicLong());
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new PlayerDamagedEvent(player, SOURCE, 1.0F));

        sounds.assertEmpty();

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("An unknown sound key falls back to the default instead of going silent")
    void anUnknownSoundKeyFallsBackToTheDefault(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);
        DamageSoundService service = service(
                config(true, COOLDOWN_TICKS, Key.key("cygnus", "no_such_sound")), new AtomicLong());

        service.play(player);

        sounds.assertSingle(packet -> assertEquals(SoundEvent.ENTITY_PLAYER_HURT, packet.soundEvent()));

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Clearing a player drops their cooldown")
    void clearingAPlayerDropsTheCooldown(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        DamageSoundService service = service(config(true, COOLDOWN_TICKS, GameConfig.DEFAULT_DAMAGE_SOUND), new AtomicLong());
        service.play(player);
        Collector<EntitySoundEffectPacket> sounds = connection.trackIncoming(EntitySoundEffectPacket.class);

        service.clear(player);
        service.play(player);

        sounds.assertSingle(packet -> assertEquals(SoundEvent.ENTITY_PLAYER_HURT, packet.soundEvent()));

        env.destroyInstance(instance, true);
    }

    /**
     * Builds a service whose clock the test moves by hand, so a cooldown can be crossed without
     * waiting for it.
     *
     * @param config the configuration to run with
     * @param clock  the milliseconds the service reads the current time from
     * @return the service under test
     */
    private static DamageSoundService service(GameConfig config, AtomicLong clock) {
        return new DamageSoundService(config, clock::get);
    }

    /**
     * Builds a configuration that only says something about the damage sound.
     *
     * @param enabled  whether the feedback is on
     * @param cooldown the cooldown in ticks
     * @param sound    the sound key to play
     * @return the configuration
     */
    private static GameConfig config(boolean enabled, int cooldown, Key sound) {
        return GameConfig.builder()
                .damageSoundEnabled(enabled)
                .damageSoundCooldown(cooldown)
                .damageSound(sound)
                .build();
    }
}
