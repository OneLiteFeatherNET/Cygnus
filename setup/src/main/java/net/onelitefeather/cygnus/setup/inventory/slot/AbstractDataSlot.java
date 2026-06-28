package net.onelitefeather.cygnus.setup.inventory.slot;

import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.slot.Slot;

import java.util.function.Consumer;

public abstract class AbstractDataSlot extends Slot {

    protected final MapDataCategory type;

    protected AbstractDataSlot(MapDataCategory type) {
        this.type = type;
        this.setClick(this::click);
    }

    protected abstract void click(
            Player player,
            int slot,
            Click clickType,
            ItemStack stack,
            Consumer<ClickHolder> result
    );

    /**
     * Converts the given ItemStack to a builder, allowing for modifications
     * without altering the original stack.
     *
     * @param stack the ItemStack to convert
     * @return a new ItemStack.Builder with the same material and custom name
     */
    protected ItemStack.Builder asBuilder(ItemStack stack) {
        ItemStack.Builder builder = ItemStack.builder(stack.material());
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            builder.customName(stack.get(DataComponents.CUSTOM_NAME));
        }
        return builder;
    }
}
