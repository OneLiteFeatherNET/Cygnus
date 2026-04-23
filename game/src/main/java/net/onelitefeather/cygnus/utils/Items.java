package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import org.jetbrains.annotations.NotNull;

/**
 * The class contains each {@link ItemStack} reference which is required for the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings({"java:S3252"})
public final class Items {

    private static final ItemStack slenderEye = ItemStack.builder(Material.ENDER_EYE)
            .customName(Component.text("SlenderEye").color(TextColor.fromHexString("#ff00d4")))
            .set(Tags.ITEM_TAG, (byte) 0)
            .build();

    /**
     * Sets the {@link ItemStack} for the SlenderEye to the player inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setSlenderEye(Player player) {
        player.getInventory().clear();
        player.getInventory().addItemStack(slenderEye);
        player.switchEntityType(EntityType.ENDERMAN);
    }

    private Items() {}
}
