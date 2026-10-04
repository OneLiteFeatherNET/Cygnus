package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hands out potion effects and takes back exactly those again.
 * <p>
 * Taking back only removes an effect that is still the one handed out here. If it has run out, or
 * something else has put the same effect on the player since, it is left alone.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class TrackedEffects {

    private final Map<Key, TimedPotion> given = new ConcurrentHashMap<>();

    /**
     * Puts an effect on a player and remembers it.
     *
     * @param player the player
     * @param potion the effect to put on them
     */
    void add(Player player, Potion potion) {
        player.addEffect(potion);
        TimedPotion active = player.getEffect(potion.effect());
        if (active != null) this.given.put(new Key(player, potion.effect()), active);
    }

    /**
     * Takes back one effect from one player, if it is still the one handed out here.
     *
     * @param player the player
     * @param effect the effect to take back
     */
    void remove(Player player, PotionEffect effect) {
        TimedPotion given = this.given.remove(new Key(player, effect));
        if (given != null && Objects.equals(player.getEffect(effect), given)) {
            player.removeEffect(effect);
        }
    }

    /**
     * Takes back every effect handed out that is still running, and forgets them all.
     */
    void removeAll() {
        for (Key key : List.copyOf(this.given.keySet())) {
            this.remove(key.player(), key.effect());
        }
    }

    /**
     * One effect on one player. A new effect of the same kind replaces the one remembered before.
     */
    private record Key(Player player, PotionEffect effect) {
    }
}
