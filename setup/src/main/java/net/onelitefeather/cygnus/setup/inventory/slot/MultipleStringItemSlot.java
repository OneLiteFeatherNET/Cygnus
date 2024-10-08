package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.inventory.slot.Slot;
import de.icevizion.aves.inventory.util.InventoryConstants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("java:S3252")
public final class MultipleStringItemSlot extends Slot {

    private final ItemStack stack;

    public MultipleStringItemSlot(@NotNull Component displayName, @Nullable String... data) {
        this(displayName, data == null ? List.of() : List.of(data));
    }

    public MultipleStringItemSlot(@NotNull Component displayName, @NotNull List<String> data) {
        if (data.isEmpty()) {
            this.stack = ItemStack.builder(Material.DARK_OAK_SIGN)
                    .customName(displayName)
                    .lore(Component.empty(), Component.text("No data available", NamedTextColor.RED), Component.empty())
                    .build();
            setClick(InventoryConstants.CANCEL_CLICK);
            return;
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        Component prefixPart = Component.text("-", NamedTextColor.GRAY).append(Component.space());

        for (int i = 0; i < data.size(); i++) {
            String rawData = data.get(i);
            lore.add(prefixPart.append(Component.text(rawData, NamedTextColor.GOLD)));
        }

        lore.add(Component.empty());
        this.stack = ItemStack.builder(Material.DARK_OAK_SIGN)
                .customName(displayName)
                .lore(lore)
                .build();

        setClick(InventoryConstants.CANCEL_CLICK);
    }

    @Override
    public @NotNull ItemStack getItem() {
        return this.stack;
    }
}
