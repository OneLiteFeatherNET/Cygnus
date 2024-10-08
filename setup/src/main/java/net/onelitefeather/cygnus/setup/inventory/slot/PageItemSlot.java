package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S3252")
public final class PageItemSlot extends PositionBaseSlot {

    private static final Component PAGE_NAME = Component.text("Page", NamedTextColor.GOLD);
    private final ItemStack stack;

    public PageItemSlot(@NotNull PageResource pageResource) {
        super(pageResource.position());
        this.stack = ItemStack.builder(Material.GREEN_BED)
                .customName(PAGE_NAME)
                .lore(getPosLore(this.point))
                .build();
        setClick(this::handleClick);
    }

    @Override
    public @NotNull ItemStack getItem() {
        return stack;
    }
}
