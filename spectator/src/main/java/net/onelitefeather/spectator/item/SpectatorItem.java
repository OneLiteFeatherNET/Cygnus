package net.onelitefeather.spectator.item;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * The SpectatorItem interface defines the fundamental properties of an {@link ItemStack} that can be used
 * by a {@link net.minestom.server.entity.Player} in spectator mode.
 * <p>
 * A SpectatorItem represents an item shown in the inventory of a spectator, complete with details such as
 * the item’s slot and internal indices.
 * <p>
 * The SpectatorItem interface is sealed and only permits extensions from the {@link SpectatorHotBarItem} class.
 *
 * @version 1.0.0
 * @see SpectatorHotBarItem
 * @since 1.0.0
 */
public sealed interface SpectatorItem permits SpectatorHotBarItem {

    Tag<Integer> SPEC_ITEM_TAG = Tag.Integer("spectator_item");

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

    /**
     * Retrieves the logic to be executed when the item is clicked by the spectator.
     *
     * @return a non-null {@link Consumer} that represents the logic to be executed
     */
    @NotNull Consumer<Player> logic();
}
