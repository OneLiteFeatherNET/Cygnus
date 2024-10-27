package net.onelitefeather.cygnus.player;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("java:S3252")
public final class CygnusPlayer extends Player {

    private static final AttributeModifier SPEED_MODIFIER_SPRINTING =
            new AttributeModifier(NamespaceID.from("minecraft:sprinting"), 0.3, AttributeOperation.MULTIPLY_TOTAL);

    private static final AttributeModifier DISABLED_SPRINT_MODIFIER =
            new AttributeModifier(NamespaceID.from("minecraft:sprinting"), 0.0, AttributeOperation.MULTIPLY_TOTAL);

    private boolean blockedSprinting;

    public CygnusPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
        super(uuid, username, playerConnection);
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
            sendPacket(getPropertiesPacket());
            return;
        }

        if (sprinting) {
            this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(DISABLED_SPRINT_MODIFIER);
            this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SPEED_MODIFIER_SPRINTING);
        } else {
            this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_SPRINTING);
            this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(DISABLED_SPRINT_MODIFIER);
        }
        this.entityMeta.setSprinting(sprinting);
        sendPacket(getPropertiesPacket());
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
