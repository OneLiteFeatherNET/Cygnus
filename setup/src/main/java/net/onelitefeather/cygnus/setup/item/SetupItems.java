package net.onelitefeather.cygnus.setup.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Tags;

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

    private static final HotBarLayout selectionLayout;
    private static final HotBarLayout mapLayout;
    private static final HotBarLayout pageLayout;

    static {
        selectionLayout = new HotBarLayout();
        selectionLayout.set(FOURTH_INDEX, ItemStack.builder(Material.CHEST)
                .customName(Component.text("Map selection", NamedTextColor.GREEN))
                .set(Tags.ITEM_TAG, ZERO_INDEX)
                .build()
        );
        ItemStack saveItem = ItemStack.builder(Material.BELL)
                .customName(Component.text("Save data", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, (byte) 0x01)
                .build();
        mapLayout = new HotBarLayout();
        mapLayout.set(2, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Data", NamedTextColor.AQUA))
                .set(Tags.ITEM_TAG, (byte) 0x02)
                .build()
        );
        mapLayout.set(6, saveItem);

        pageLayout = new HotBarLayout();
        pageLayout.set(2, ItemStack.builder(Material.BARRIER)
                .customName(Component.text("Cancel", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, (byte) 0x03)
                .build()
        );
        pageLayout.set(6, saveItem);
    }

    /**
     * Set's the {@link ItemStack} which represents the map selection into an inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setMapSelection(Player player) {
        selectionLayout.apply(player);
        player.setHeldItemSlot(FOURTH_INDEX);
    }

    /**
     * Set's the {@link ItemStack} which represents the save function into an inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setSaveData(Player player) {
        mapLayout.apply(player);
        player.setHeldItemSlot(ZERO_INDEX);
    }

    /**
     * Set's the {@link ItemStack} which are required for the page setup.
     *
     * @param player the player who should receive the item
     */
    public static void setPageItems(Player player) {
        pageLayout.apply(player);
        player.setHeldItemSlot(ZERO_INDEX);
    }

    private SetupItems() {
        // Nothing to do here
    }
}
