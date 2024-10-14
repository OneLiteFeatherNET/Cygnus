package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.util.functional.PlayerConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.inventory.DataType;
import org.jetbrains.annotations.NotNull;

import static net.onelitefeather.cygnus.setup.util.LoreHelper.getPosLore;

@SuppressWarnings("java:S3252")
public final class PageItemSlot extends PositionBaseSlot {

    private static final Component PAGE_NAME = Component.text("Page", NamedTextColor.GOLD);
    private final ItemStack stack;

    public PageItemSlot(@NotNull PageResource pageResource, @NotNull PlayerConsumer rightClickAction) {
        super(pageResource.position(), rightClickAction);
        this.stack = ItemStack.builder(DataType.PAGE.getMaterial())
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
