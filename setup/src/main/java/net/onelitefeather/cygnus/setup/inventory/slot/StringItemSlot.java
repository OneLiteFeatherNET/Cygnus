package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.inventory.slot.Slot;
import de.icevizion.aves.inventory.util.InventoryConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S3252")
public final class StringItemSlot extends Slot {

    private final ItemStack stack;

    public StringItemSlot(@NotNull Component displayName, @NotNull String data) {
        Component mainLoreComponent = Component.text("-", NamedTextColor.GRAY).append(Component.space()).append(Component.text(data, NamedTextColor.GOLD));
        this.stack = ItemStack.builder(Material.OAK_SIGN)
                .customName(displayName)
                .lore(Component.empty(), mainLoreComponent, Component.empty())
                .build();
        setClick(InventoryConstants.CANCEL_CLICK);
    }

    @Override
    public @NotNull ItemStack getItem() {
        return this.stack;
    }
}
