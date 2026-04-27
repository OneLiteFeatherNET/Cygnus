package net.onelitefeather.cygnus.setup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Tags;
import org.jetbrains.annotations.NotNull;

/**
 * The class holds the {@link ItemStack} references which have some functionality during a setup process from an map.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("java:S3252")
public final class SetupItems {

    public static final byte ZERO_INDEX = (byte) 0x00;
    public static final byte FOURTH_INDEX = (byte) 0x04;

    public static final ItemStack DECORATION;

    private static final ItemStack mapSelection;
    private static final ItemStack dataItem;
    private static final ItemStack saveData;

    static  {
        mapSelection = ItemStack.builder(Material.CHEST)
                .customName(Component.text("Map selection", NamedTextColor.GREEN))
                .set(Tags.ITEM_TAG, ZERO_INDEX)
                .build();
        saveData = ItemStack.builder(Material.BELL)
                .customName(Component.text("Save data", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, (byte) 0x01)
                .build();
        dataItem = ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Data", NamedTextColor.AQUA))
                .set(Tags.ITEM_TAG, (byte) 0x02)
                .build();
        DECORATION = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();
    }

    /**
     * Set's the {@link ItemStack} which represents the map selection into an inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setMapSelection(@NotNull Player player) {
        player.getInventory().setItemStack(FOURTH_INDEX, mapSelection);
        player.setHeldItemSlot(FOURTH_INDEX);
    }

    /**
     * Set's the {@link ItemStack} which represents the save function into an inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setSaveData(@NotNull Player player) {
        player.getInventory().setItemStack(2, dataItem);
        player.getInventory().setItemStack(6, saveData);
        player.setHeldItemSlot(ZERO_INDEX);
    }

    private SetupItems() {
        // Nothing to do here
    }
}
