package net.onelitefeather.cygnus.setup.item;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

import java.util.Arrays;

/**
 * Represents a hotbar layout consisting of nine item slots (indices 0–8).
 * Each slot is initially filled with {@link ItemStack#AIR} and can be
 * overwritten individually via {@link #set(int, ItemStack)}.
 * <p>
 * Use {@link #apply(Player)} to write the layout into a player's inventory.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
public final class HotBarLayout {

    private static final int HOTBAR_SIZE = 9;

    private final ItemStack[] items = new ItemStack[HOTBAR_SIZE];

    /**
     * Creates a new layout with all slots set to {@link ItemStack#AIR}.
     */
    public HotBarLayout() {
        Arrays.fill(items, ItemStack.AIR);
    }

    /**
     * Places the given item at the specified hotbar slot.
     *
     * @param slot the slot index (0–8)
     * @param item the item to place
     * @return this layout, for chaining
     * @throws IndexOutOfBoundsException if the slot is not in range 0–8
     */
    public HotBarLayout set(int slot, ItemStack item) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            throw new IndexOutOfBoundsException("Slot must be between 0 and 8, got: " + slot);
        }
        items[slot] = item;
        return this;
    }

    /**
     * Returns the item at the given hotbar slot.
     *
     * @param slot the slot index (0–8)
     * @return the item at the slot
     * @throws IndexOutOfBoundsException if the slot is not in range 0–8
     */
    public ItemStack get(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            throw new IndexOutOfBoundsException("Slot must be between 0 and 8, got: " + slot);
        }
        return items[slot];
    }

    /**
     * Writes this layout into the player's inventory (slots 0–8).
     *
     * @param player the player whose hotbar is updated
     */
    public void apply(Player player) {
        for (int i = 0; i < items.length; i++) {
            player.getInventory().setItemStack(i, items[i]);
        }
    }
}
