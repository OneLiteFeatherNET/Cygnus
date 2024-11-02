package net.onelitefeather.spectator.item;

import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The SpectatorItem interface defines the fundamental properties of an {@link ItemStack} that can be used
 * by a {@link net.minestom.server.entity.Player} in spectator mode.
 * <p>
 * A SpectatorItem represents an item shown in the inventory of a spectator, complete with details such as
 * the item’s slot and internal indices.
 * <p>
 * The SpectatorItem interface is sealed and only permits extensions from the {@link SpectatorItemContainer} class.
 *
 * @version 1.0.0
 * @see SpectatorItemContainer
 * @since 1.0.0
 */
public sealed interface SpectatorItem permits SpectatorItemContainer {

    /**
     * Retrieves the {@link ItemStack} displayed in the spectator's inventory.
     *
     * @return a non-null {@link ItemStack} that represents the item displayed
     */
    @NotNull ItemStack item();

    /**
     * Returns the unique internal index of the {@link ItemStack}, used for identifying the item
     * across different gameplay scenarios, such as inventory management and event handling.
     *
     * @return an integer representing the item's internal index
     */
    int itemIndex();

    /**
     * Obtains the slot index where this {@link ItemStack} should be placed in the spectator's inventory.
     * This determines the item's position
     *
     * @return an integer representing the inventory slot index
     */
    int slotIndex();
}