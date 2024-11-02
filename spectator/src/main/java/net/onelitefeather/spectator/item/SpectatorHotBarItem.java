package net.onelitefeather.spectator.item;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.math.IntRange;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * The {@link SpectatorHotBarItem} is the implementation of the {@link SpectatorItem} interface.
 *
 * @param item      the item to be displayed
 * @param itemIndex the index of the item
 * @param slotIndex the index of the slot
 * @param logic     the logic to be executed when the item is clicked
 */
@SuppressWarnings("java:S3252")
public record SpectatorHotBarItem(
        @NotNull ItemStack item,
        int itemIndex,
        int slotIndex,
        @NotNull Consumer<Player> logic
) implements SpectatorItem {

    private static final IntRange SLOT_RANGE = new IntRange(0, 8);

    public SpectatorHotBarItem {
        Check.argCondition(item == ItemStack.AIR, "The ItemStack can not be referenced to ItemStack.AIR");
        Check.argCondition(item.material() == Material.AIR, "Item cannot be air");
        Check.argCondition(itemIndex < 0, "Item index cannot be negative");
        Check.argCondition(!SLOT_RANGE.isInRange(slotIndex), "Item index must be between 0 and 8");
        Check.argCondition(!item.hasTag(SPEC_ITEM_TAG), "Item must have the SpectatorItem tag");
    }

    /**
     * Creates a new instance of the {@link SpectatorHotBarItem}.
     * @param builder the builder to create the item
     * @param itemIndex the index of the item
     * @param slotIndex the index of the slot
     * @param logic the logic to be executed when the item is clicked
     */
    public SpectatorHotBarItem(@NotNull ItemStack.Builder builder, int itemIndex, int slotIndex, @NotNull Consumer<Player> logic) {
        this(builder.set(SpectatorItem.SPEC_ITEM_TAG, itemIndex).build(), itemIndex, slotIndex, logic);
    }
}
