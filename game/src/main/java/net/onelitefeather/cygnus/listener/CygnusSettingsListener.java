package net.onelitefeather.cygnus.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.avatar.PlayerMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerSettingsChangeEvent;
import net.minestom.server.network.packet.client.common.ClientSettingsPacket;

/**
 * Replaces Minestom's built-in {@code SettingsListener}. A disguised {@link Player} (e.g. the
 * Slender player, see {@link net.onelitefeather.cygnus.utils.Items#setSlenderEye}) has its entity
 * meta swapped to a non-avatar type such as {@code EndermanMeta}. Minestom's own listener always
 * casts the meta to {@link PlayerMeta} and crashes with a {@link ClassCastException} whenever the
 * client resends its settings while disguised, so this guards the cast instead.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.7.1
 */
public final class CygnusSettingsListener {

    private CygnusSettingsListener() {
    }

    public static void listener(ClientSettingsPacket packet, Player player) {
        if (player.getEntityMeta() instanceof PlayerMeta) {
            player.refreshSettings(packet.settings());
        }
        EventDispatcher.call(new PlayerSettingsChangeEvent(player));
    }
}
