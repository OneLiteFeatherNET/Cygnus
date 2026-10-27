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
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.vitality.Vitality;

import java.util.Collection;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Shared behaviour of the slender's bar: his swing at the survivors in range, his blindness and the
 * sounds survivors hear when he appears or vanishes.
 *
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 */
@SuppressWarnings({"java:S3252"})
public interface SlenderBarHelper {

    byte VISIBLE = 0;
    byte HIDDEN = 1;

    Sound TELEPORT = Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1F, 0.2F);
    Sound SPAWN = Sound.sound(SoundEvent.ENTITY_WITHER_SPAWN, Sound.Source.MASTER, 0.4F, 0F);

    /**
     * How far away a survivor hears the slender appear and vanish, in blocks. Minecraft fades a quiet
     * sound out over this distance anyway, so further away nobody would hear it.
     */
    int SOUND_RANGE = 16;

    TimedPotion BLINDNESS = new TimedPotion(new Potion(PotionEffect.BLINDNESS, (byte) 1, -1), 0);

    BiPredicate<UUID, UUID> UUID_COMPARATOR = UUID::equals;

    /**
     * What one swing did to the survivors in range. Both can be true at once, since every survivor
     * rolls on their own.
     *
     * @param hit    whether at least one survivor was hit
     * @param missed whether at least one survivor was missed
     */
    record SwingOutcome(boolean hit, boolean missed) {
    }

    /**
     * Takes the blindness back off the given player.
     *
     * <p>The slender used to receive night vision here, so that hiding also meant seeing better.
     * He no longer does: hiding now only lifts the blindness, and he sees the map as everyone
     * else does.</p>
     *
     * @param player the player to clear the effect from
     */
    default void clearBlindness(Player player) {
        player.removeEffect(BLINDNESS.potion().effect());
    }

    /**
     * Applies the blindness effect to the given player.
     *
     * @param player the player to apply the effect
     */
    default void applyBlindness(Player player) {
        player.addEffect(BLINDNESS.potion());
    }

    /**
     * Swings once at every survivor in the given range.
     * <p>
     * Every survivor rolls on their own with their own fear, so the same swing can hit one and miss
     * the other. A hit takes the rolled hearts as points through {@link Vitality}, a miss takes nothing.
     * </p>
     *
     * @param instance the instance where the entities are located
     * @param uuid     the uuid of the player who swings
     * @param center   the center position of the swing, also where a hit comes from
     * @param range    the range of the swing
     * @param swing    rolls what the swing does to each survivor
     * @param dread    supplies the fear of each survivor
     * @return whether the swing hit and whether it missed at least one survivor in range
     */
    default SwingOutcome swing(Instance instance, UUID uuid, Pos center, int range, SlenderSwing swing, DreadSource dread) {
        boolean hit = false;
        boolean missed = false;
        long now = System.currentTimeMillis();
        for (Entity nearbyEntity : instance.getNearbyEntities(center, range)) {
            if (!(nearbyEntity instanceof CygnusPlayer target)) continue;
            if (UUID_COMPARATOR.test(uuid, target.getUuid())) continue;
            if (!isDamageableSurvivor(target)) continue;
            double hearts = swing.roll(dread.dreadOf(target.getUuid(), now));
            if (hearts <= 0.0D) {
                missed = true;
                continue;
            }
            target.vitality().damage(hearts * SlenderSwing.POINTS_PER_HEART, center);
            hit = true;
        }
        return new SwingOutcome(hit, missed);
    }

    /**
     * Checks whether the given player may take damage from the slender.
     * <p>
     * Only players of the survivor team are valid targets. The slender itself and every spectator
     * must stay untouched, otherwise a spectator would slowly bleed out and trigger the whole death
     * pipeline a second time. Because {@link Vitality} sets health directly and bypasses the damage
     * event chain, the game mode is checked as a second, independent guard: it stays correct even if the
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
     * Plays a sound to every survivor within {@link #SOUND_RANGE}. It comes from where the slender
     * stands, so a survivor hears from which side he appears, and quieter the further away he is.
     *
     * @param instance the instance where the entities are located
     * @param center   the center position where the sound should be played
     * @param uuid     the uuid of the player who caused the sound
     * @param spawn    if the sound should be a spawn sound
     */
    private void playToSound(Instance instance, Pos center, UUID uuid, boolean spawn) {
        Collection<Entity> nearbyEntities = instance.getNearbyEntities(center, SOUND_RANGE);
        if (nearbyEntities.isEmpty()) return;

        for (Entity nearbyEntity : nearbyEntities) {
            if (!(nearbyEntity instanceof Player target)) continue;
            if (UUID_COMPARATOR.test(uuid, target.getUuid())) continue;
            // The teleport sound fires exactly when the slender turns invisible, so it would tell a
            // spectator both the position and the state of the slender. Only survivors may hear it.
            if (!TeamHelper.isSurvivorTeam(target)) continue;
            target.playSound(spawn ? SPAWN : TELEPORT, center);
        }
    }
}
