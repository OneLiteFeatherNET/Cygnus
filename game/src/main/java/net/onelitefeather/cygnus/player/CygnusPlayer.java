package net.onelitefeather.cygnus.player;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S3252")
public final class CygnusPlayer extends Player {

    private static final AttributeModifier SPEED_MODIFIER_SPRINTING =
            new AttributeModifier(Key.key("minecraft:sprinting"), 0.25, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    private static final AttributeModifier DISABLED_SPRINT_MODIFIER =
            new AttributeModifier(Key.key("minecraft:sprinting"), 0.0, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    private boolean blockedSprinting;

    public CygnusPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
        this.blockedSprinting = false;
    }

    /**
     * Sets if the player is blocked from sprinting.
     *
     * @param blockedSprinting {@code true} if the player is blocked from sprinting, otherwise {@code false}.
     */
    public void setBlockedSprinting(boolean blockedSprinting) {
        this.blockedSprinting = blockedSprinting;
    }

    /**
     * Sets if the entity is sprinting.
     *
     * @param sprinting true to make the entity sprint otherwise false for no sprinting
     */
    @Override
    public void setSprinting(boolean sprinting) {
        if (blockedSprinting) {
            this.entityMeta.setSprinting(false);
            this.sendSpringPackets();
            return;
        }

        if (sprinting) {
            this.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(DISABLED_SPRINT_MODIFIER);
            this.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(SPEED_MODIFIER_SPRINTING);
        } else {
            this.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_SPRINTING);
            this.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(DISABLED_SPRINT_MODIFIER);
        }
        this.entityMeta.setSprinting(sprinting);
        this.sendSpringPackets();
    }

    /**
     * Sends the packets to the player to update the sprinting state.
     */
    public void sendSpringPackets() {
        sendPacket(getPropertiesPacket());
        sendPacket(getMetadataPacket());
    }

    /**
     * Checks if the player has blocked sprinting.
     *
     * @return {@code true} if the player has blocked sprinting, otherwise {@code false}.
     */
    public boolean hasBlockedSprinting() {
        return blockedSprinting;
    }

    @Override
    public @NotNull EntityAttributesPacket getPropertiesPacket() {
        return super.getPropertiesPacket();
    }
}
