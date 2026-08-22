package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.Collection;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * The StaminaDamage interface is designed to provide a method to apply damage to entities in a given range.
 * The method will iterate over all entities in the given range and apply the damage to the entity if it is a player.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
@SuppressWarnings({"java:S3252"})
public interface SlenderBarHelper {

    byte VISIBLE = 0;
    byte HIDDEN = 1;

    Sound TELEPORT = Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1F, 0.2F);
    Sound SPAWN = Sound.sound(SoundEvent.ENTITY_WITHER_SPAWN, Sound.Source.MASTER, 0.4F, 0F);

    TimedPotion NIGHT_VISION = new TimedPotion(new Potion(PotionEffect.NIGHT_VISION, (byte) 1, -1), 0);
    TimedPotion BLINDNESS = new TimedPotion(new Potion(PotionEffect.BLINDNESS, (byte) 1, -1), 0);

    BiPredicate<UUID, UUID> UUID_COMPARATOR = UUID::equals;

    /**
     * Applies the night vision effect to the given player.
     *
     * @param player the player to apply the effect
     */
    default void applyNightVision(Player player) {
        updateEffect(player, true);
    }

    /**
     * Applies the blindness effect to the given player.
     *
     * @param player the player to apply the effect
     */
    default void applyBlindness(Player player) {
        updateEffect(player, false);
    }

    /**
     * Updates the effect for the given player.
     *
     * @param player      the player to update the effect
     * @param nightVision if the night vision should be applied
     */
    private void updateEffect(Player player, boolean nightVision) {
        if (nightVision) {
            player.removeEffect(BLINDNESS.potion().effect());
            player.addEffect(NIGHT_VISION.potion());
            return;
        }
        player.removeEffect(NIGHT_VISION.potion().effect());
        player.addEffect(BLINDNESS.potion());
    }

    /**
     * Applies the damage to all entities in the given range.
     *
     * @param instance the instance where the entities are located
     * @param uuid     the uuid of the player who caused the damage
     * @param center   the center position where the damage should be applied
     * @param range    the range where the damage should be applied
     * @param damage   the damage that should be applied
     */
    default void applyDamage(Instance instance, UUID uuid, Pos center, int range, float damage) {
        Collection<Entity> nearbyEntities = instance.getNearbyEntities(center, range);
        if (nearbyEntities.isEmpty()) return;
        for (Entity nearbyEntity : nearbyEntities) {
            if (!(nearbyEntity instanceof Player target)) continue;
            if (UUID_COMPARATOR.test(uuid, target.getUuid())) continue;
            if (!isDamageableSurvivor(target)) continue;
            target.setHealth(target.getHealth() - damage);
        }
    }

    /**
     * Checks whether the given player may take damage from the slender.
     * <p>
     * Only players of the survivor team are valid targets. The slender itself and every spectator
     * must stay untouched, otherwise a spectator would slowly bleed out and trigger the whole death
     * pipeline a second time. Because {@link Player#setHealth(float)} bypasses the damage event
     * chain, the game mode is checked as a second, independent guard: it stays correct even if the
     * team tag and the game mode ever drift apart.
     *
     * @param target the player to check
     * @return {@code true} if the player is a living survivor that may take damage
     */
    private static boolean isDamageableSurvivor(Player target) {
        return TeamHelper.isSurvivorTeam(target)
                && !target.getGameMode().invulnerable()
                && !target.isDead()
                && target.getHealth() > 0;
    }

    /**
     * Plays the spawn sound to all players in the given range.
     *
     * @param instance the instance where the entities are located
     * @param center   the center position where the sound should be played
     * @param uuid     the uuid of the player who caused the sound
     */
    default void playSpawnSound(Instance instance, Pos center, UUID uuid) {
        playToSound(instance, center, uuid, true);
    }

    /**
     * Plays the teleport sound to all players in the given range.
     *
     * @param instance the instance where the entities are located
     * @param center   the center position where the sound should be played
     * @param uuid     the uuid of the player who caused the sound
     */
    default void playTeleportSound(Instance instance, Pos center, UUID uuid) {
        playToSound(instance, center, uuid, false);
    }

    /**
     * Plays a sound to all players in the given range.
     *
     * @param instance the instance where the entities are located
     * @param center   the center position where the sound should be played
     * @param uuid     the uuid of the player who caused the sound
     * @param spawn    if the sound should be a spawn sound
     */
    private void playToSound(Instance instance, Pos center, UUID uuid, boolean spawn) {
        Collection<Entity> nearbyEntities = instance.getNearbyEntities(center, 2);
        if (nearbyEntities.isEmpty()) return;

        for (Entity nearbyEntity : nearbyEntities) {
            if (!(nearbyEntity instanceof Player target)) continue;
            if (UUID_COMPARATOR.test(uuid, target.getUuid())) continue;
            // The teleport sound fires exactly when the slender turns invisible, so it would tell a
            // spectator both the position and the state of the slender. Only survivors may hear it.
            if (!TeamHelper.isSurvivorTeam(target)) continue;
            target.playSound(spawn ? SPAWN : TELEPORT, target.getPosition());
        }
    }
}
