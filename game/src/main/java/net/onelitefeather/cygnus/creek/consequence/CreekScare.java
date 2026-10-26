package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;

/**
 * The creek's scare sound, shared by a catch and the patrol.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class CreekScare {

    /** The sound of the scare. */
    public static final Key SCARE_SOUND = Key.key("entity.creaking.activate");

    private CreekScare() {
    }

    /**
     * Plays the creek's scare sound to a player.
     *
     * @param player the player
     */
    public static void play(Player player) {
        player.playSound(Sound.sound(SCARE_SOUND, Sound.Source.HOSTILE, 1.0F, 0.6F));
    }
}
