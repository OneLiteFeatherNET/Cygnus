package net.onelitefeather.cygnus.damage;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.utils.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongSupplier;

/**
 * Lets a player hear that they were hit.
 *
 * <p>Cygnus takes health off a survivor with {@link Player#setHealth(float)}, which never runs
 * Minestom's damage pipeline and therefore never plays the sound a client would otherwise hear - a
 * hit lands in silence. This service fills that gap: it listens for {@link PlayerDamagedEvent} and
 * plays the configured sound on the victim's own entity, so it reaches nobody else and carries no
 * direction. The blood splatter already tells the player where the hit came from; the sound only
 * has to tell them that it happened.</p>
 *
 * <p>The slender never hears it. He is the only source of damage in the game and is skipped by
 * {@code SlenderBarHelper#applyDamage}, so no event ever names him as the victim - a sound on his
 * side would only be a second way to locate the survivor he is already standing next to.</p>
 *
 * <p>The cooldown is what keeps the feedback readable. A draining slender damages everyone around
 * him twice a second, so without it a survivor next to him would hear the sound at that rate until
 * one of them died.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DamageSoundService service = new DamageSoundService(config, System::currentTimeMillis);
 * service.registerListener(eventNode);
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.13.0
 */
public final class DamageSoundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamageSoundService.class);

    /** How long a server tick lasts, to read the configured cooldown against a wall clock. */
    private static final long MILLIS_PER_TICK = 50L;

    /**
     * Played at full volume and unshifted pitch: the sound sits on the listener's own entity, so
     * there is no distance for a volume to model and no second hit for a pitch to tell apart.
     */
    private static final float VOLUME = 1.0F;

    /** @see #VOLUME */
    private static final float PITCH = 1.0F;

    private final GameConfig config;
    private final LongSupplier clock;
    private final Sound sound;
    private final long cooldownMillis;

    /** When each player may hear the sound again, as a value of {@link #clock}. */
    private final PlayerState<Long> cooldowns = new PlayerState<>();

    /**
     * Creates a new instance of the {@link DamageSoundService}.
     *
     * @param config the configuration holding the sound and the cooldown
     * @param clock  supplies the current time in milliseconds
     */
    public DamageSoundService(GameConfig config, LongSupplier clock) {
        this.config = config;
        this.clock = clock;
        this.cooldownMillis = config.damageSoundCooldown() * MILLIS_PER_TICK;
        this.sound = Sound.sound(
                resolveSound(config.damageSound()),
                Sound.Source.PLAYER,
                VOLUME,
                PITCH
        );
    }

    /**
     * Listens for hits and for players leaving.
     *
     * @param node the node to register on
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(PlayerDamagedEvent.class, event -> this.play(event.getPlayer()));
        node.addListener(PlayerDisconnectEvent.class, event -> this.clear(event.getPlayer()));
    }

    /**
     * Plays the sound to the given player, unless the feature is off or they are still inside their
     * cooldown.
     *
     * @param player the player who was hit
     */
    void play(Player player) {
        if (!this.config.damageSoundEnabled()) {
            return;
        }

        long now = this.clock.getAsLong();
        Long readyAt = this.cooldowns.get(player);
        if (readyAt != null && now < readyAt) {
            return;
        }

        this.cooldowns.put(player, now + this.cooldownMillis);
        // Emitter.self() binds the sound to the victim's own entity: it reaches only their client
        // and plays without direction or falloff, which a position - even their own - would add.
        player.playSound(this.sound, Sound.Emitter.self());
    }

    /**
     * Forgets a player's cooldown. Someone who rejoins is a fresh player, and the map must not keep
     * growing over a round either.
     *
     * @param player the player to clear
     */
    public void clear(Player player) {
        this.cooldowns.remove(player);
    }

    /**
     * Resolves the configured key against the sound registry. A key that names no known sound would
     * leave a hit silent again, which is the bug this service exists for, so it falls back to
     * {@link GameConfig#DEFAULT_DAMAGE_SOUND}.
     *
     * @param key the configured sound key
     * @return the resolved sound
     */
    private static SoundEvent resolveSound(Key key) {
        SoundEvent soundEvent = SoundEvent.fromKey(key);
        if (soundEvent != null) {
            return soundEvent;
        }
        LOGGER.warn("'{}' names no known sound, falling back to {}", key, GameConfig.DEFAULT_DAMAGE_SOUND);
        return SoundEvent.fromKey(GameConfig.DEFAULT_DAMAGE_SOUND);
    }
}
