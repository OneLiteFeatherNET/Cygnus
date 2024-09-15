package net.onelitefeather.cygnus.utils;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

/**
 * The class contains each {@link ItemStack} reference which is required for the game.
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings({"java:S3252"})
public final class Items {

    private final ItemStack slenderEye;

    public Items() {
        this.slenderEye = ItemStack.builder(Material.ENDER_EYE)
                .customName(Messages.withMini("<!i><color:#ff00d4>SlenderEye</color>"))
                .set(Tags.ITEM_TAG, (byte) 0)
                .build();
    }

    public void setSlenderEye(@NotNull Player player) {
        player.getInventory().clear();
        player.getInventory().addItemStack(this.slenderEye);
        player.switchEntityType(EntityType.ENDERMAN);
    }
}
