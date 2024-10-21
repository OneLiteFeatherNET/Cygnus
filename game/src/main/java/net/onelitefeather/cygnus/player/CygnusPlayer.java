package net.onelitefeather.cygnus.player;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
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

    public void setBlockedSprinting(boolean blockedSprinting) {
        this.blockedSprinting = blockedSprinting;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        if (blockedSprinting) {
            this.entityMeta.setSprinting(false);
            sendPacket(getMetadataPacket());
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
        sendPacket(getMetadataPacket());
    }

    public boolean hasBlockedSprinting() {
        return blockedSprinting;
    }
}
