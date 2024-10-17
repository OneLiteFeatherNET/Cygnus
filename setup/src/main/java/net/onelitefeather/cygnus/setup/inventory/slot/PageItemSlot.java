package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.util.functional.PlayerConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.inventory.DataType;
import org.jetbrains.annotations.NotNull;

import static net.onelitefeather.cygnus.setup.util.LoreHelper.getPosLore;

@SuppressWarnings("java:S3252")
public final class PageItemSlot extends PositionBaseSlot {

    private static final Component PAGE_NAME = Component.text("Page", NamedTextColor.GOLD);
    private final ItemStack stack;
    private final Point opposite;

    public PageItemSlot(@NotNull PageResource pageResource, @NotNull PlayerConsumer rightClickAction) {
        super(pageResource.position(), rightClickAction);
        this.stack = ItemStack.builder(DataType.PAGE.getMaterial())
                .customName(PAGE_NAME)
                .lore(getPosLore(this.point))
                .build();
        this.opposite = pageResource.opposite();
        setClick(this::handleClick);
    }

    @Override
    protected void handleClick(@NotNull Player player, int ignore, @NotNull ClickType type, @NotNull InventoryConditionResult result) {
        result.setCancel(true);
        if (!(type == ClickType.LEFT_CLICK || type == ClickType.RIGHT_CLICK)) return;

        if (type == ClickType.LEFT_CLICK) {
            player.closeInventory();
            player.teleport(Pos.fromPoint(this.opposite));
            return;
        }

        if (type == ClickType.RIGHT_CLICK && this.rightClickAction != null) {
            this.rightClickAction.accept(player);
        }
    }

    @Override
    public @NotNull ItemStack getItem() {
        return stack;
    }
}
