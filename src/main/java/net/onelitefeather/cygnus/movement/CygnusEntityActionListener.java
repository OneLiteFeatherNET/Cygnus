package net.onelitefeather.cygnus.movement;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerStopSneakingEvent;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;
import org.jetbrains.annotations.NotNull;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public class CygnusEntityActionListener {

    public static void listener(ClientEntityActionPacket packet, Player player) {
        switch (packet.action()) {
            case START_SNEAKING -> setSneaking(player, true);
            case STOP_SNEAKING -> setSneaking(player, false);
            case START_SPRINTING -> {
                var sprintEvent = new PlayerStartSprintingEvent(player);
                EventDispatcher.callCancellable(sprintEvent, () -> setSprinting(player, true));
            }
            case STOP_SPRINTING -> setSprinting(player, false);
            case START_FLYING_ELYTRA -> startFlyingElytra(player);

            // TODO do remaining actions
        }
    }

    private static void setSprinting(@NotNull Player player, boolean sprinting) {
        player.setSprinting(sprinting);
    }

    private static void setSneaking(Player player, boolean sneaking) {
        boolean oldState = player.isSneaking();

        player.setSneaking(sneaking);

        if (oldState != sneaking) {
            if (sneaking) {
                EventDispatcher.call(new PlayerStartSneakingEvent(player));
            } else {
                EventDispatcher.call(new PlayerStopSneakingEvent(player));
            }
        }
    }

    private static void startFlyingElytra(Player player) {
        player.setFlyingWithElytra(true);
        EventDispatcher.call(new PlayerStartFlyingWithElytraEvent(player));
    }
}
