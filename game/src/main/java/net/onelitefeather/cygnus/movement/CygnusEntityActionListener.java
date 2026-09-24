package net.onelitefeather.cygnus.movement;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class CygnusEntityActionListener {

    private CygnusEntityActionListener() {
    }

    public static void listener(ClientEntityActionPacket packet, Player player) {
        switch (packet.action()) {
            case START_SPRINTING -> {
                var sprintEvent = new PlayerStartSprintingEvent(player);
                EventDispatcher.call(sprintEvent);
                player.setSprinting(!sprintEvent.isCancelled());
            }
            case STOP_SPRINTING -> {
                EventDispatcher.call(new PlayerStopSprintingEvent(player));
                player.setSprinting(false);
            }
        }
    }
}
