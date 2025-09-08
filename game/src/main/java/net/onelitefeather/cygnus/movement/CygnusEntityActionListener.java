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

public final class CygnusEntityActionListener {

    private CygnusEntityActionListener() {
    }

    public static void listener(ClientEntityActionPacket packet, Player player) {
        switch (packet.action()) {
            case START_SPRINTING -> {
                var sprintEvent = new PlayerStartSprintingEvent(player);
                EventDispatcher.call(sprintEvent);
                if (sprintEvent.isCancelled()) {
                    setSprinting(player, false);
                    return;
                }
                setSprinting(player, true);
            }
            case STOP_SPRINTING -> {
                EventDispatcher.call(new PlayerStopSprintingEvent(player));
                setSprinting(player, false);
            }

            // TODO do remaining actions
        }
    }

    private static void setSprinting(@NotNull Player player, boolean sprinting) {
        player.setSprinting(sprinting);
       /* AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        attribute.removeModifier(SPEED_MODIFIER_SPRINTING);
        if (sprinting) {
            attribute.addModifier(SPEED_MODIFIER_SPRINTING);
            return;
        }*/
        //TODO: Fix this
        // var propertiesPacket = new EntityAttributesPacket(player.getEntityId(), new ArrayList<>(Collections.singletonList(attribute)));
//        player.sendPacketToViewersAndSelf(propertiesPacket);
    }

    private static void startFlyingElytra(Player player) {
        player.setFlyingWithElytra(true);
        EventDispatcher.call(new PlayerStartFlyingWithElytraEvent(player));
    }
}
