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
 * @author theEvilReaper
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("java:S3252")
public final class SetupItems {

    public static final byte ZERO_INDEX = (byte) 0x00;
    public static final byte FOURTH_INDEX = (byte) 0x04;

    private final ItemStack mapSelection;
    private final ItemStack saveData;

    /**
     * Creates a new reference from the class and instantiates the {@link ItemStack} references.
     */
    public SetupItems() {
        this.mapSelection = ItemStack.builder(Material.CHEST)
                .customName(Component.text("Map selection", NamedTextColor.GREEN))
                .set(Tags.ITEM_TAG, ZERO_INDEX)
                .build();
        this.saveData = ItemStack.builder(Material.BELL)
                .customName(Component.text("Save data", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, (byte) 0x01)
                .build();
    }

    /**
     * Set's the {@link ItemStack} which represents the map selection into an inventory.
     * @param player the player who should receive the item
     */
    public void setMapSelection(@NotNull Player player) {
        player.getInventory().setItemStack(FOURTH_INDEX, this.mapSelection);
        player.setHeldItemSlot(FOURTH_INDEX);
    }

    /**
     * Set's the {@link ItemStack} which represents the save function into an inventory.
     * @param player the player who should receive the item
     */
    public void setSaveData(@NotNull Player player) {
        player.getInventory().setItemStack(FOURTH_INDEX, this.saveData);
        player.setHeldItemSlot(ZERO_INDEX);
    }
}
