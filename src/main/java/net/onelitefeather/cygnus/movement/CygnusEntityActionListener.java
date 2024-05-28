package net.onelitefeather.cygnus.movement;

import net.minestom.server.attribute.AttributeInstance;
import net.minestom.server.attribute.AttributeModifier;
import net.minestom.server.attribute.AttributeOperation;
import net.minestom.server.attribute.VanillaAttribute;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerStopSneakingEvent;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;
import net.minestom.server.network.packet.server.play.EntityPropertiesPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public final class CygnusEntityActionListener {

    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "Sprinting speed boost", 0.30000001192092896D, AttributeOperation.MULTIPLY_TOTAL);

    private CygnusEntityActionListener() { }

    public static void listener(ClientEntityActionPacket packet, Player player) {
        switch (packet.action()) {
            case START_SNEAKING -> setSneaking(player, true);
            case STOP_SNEAKING -> setSneaking(player, false);
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
            case START_FLYING_ELYTRA -> startFlyingElytra(player);

            // TODO do remaining actions
        }
    }

    private static void setSprinting(@NotNull Player player, boolean sprinting) {
        player.setSprinting(sprinting);
        AttributeInstance attribute = player.getAttribute(VanillaAttribute.GENERIC_MOVEMENT_SPEED);
        attribute.removeModifier(SPEED_MODIFIER_SPRINTING);
        if (sprinting) {
            attribute.addModifier(SPEED_MODIFIER_SPRINTING);
            return;
        }
        var propertiesPacket = new EntityPropertiesPacket(player.getEntityId(), new ArrayList<>(Collections.singletonList(attribute)));
        player.sendPacketToViewersAndSelf(propertiesPacket);
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
