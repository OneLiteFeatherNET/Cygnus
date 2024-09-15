package net.onelitefeather.cygnus.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

/**
 * The class holds the {@link ItemStack} references which have some functionality during a setup process from an map.
 * @author theEvilReaper
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("java:S3252")
public final class SetupItems {

    private final ItemStack mapSelection;
    private final ItemStack saveData;

    /**
     * Creates a new reference from the class and instantiates the {@link ItemStack} references.
     */
    public SetupItems() {
        this.mapSelection = ItemStack.builder(Material.CHEST)
                .customName(Component.text("Map selection", NamedTextColor.GREEN))
                .set(Tags.ITEM_TAG, (byte) 0)
                .build();
        this.saveData = ItemStack.builder(Material.BELL)
                .customName(Component.text("Save data", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, (byte) 1)
                .build();
    }

    /**
     * Set's the {@link ItemStack} which represents the map selection into an inventory.
     * @param player the player who should receive the item
     */
    public void setMapSelection(@NotNull Player player) {
        player.getInventory().setItemStack(4, this.mapSelection);
        player.setHeldItemSlot((byte)4);
    }

    /**
     * Set's the {@link ItemStack} which represents the save function into an inventory.
     * @param player the player who should receive the item
     */
    public void setSaveData(@NotNull Player player) {
        player.getInventory().setItemStack(4, this.saveData);
        player.setHeldItemSlot((byte)0);
    }
}
