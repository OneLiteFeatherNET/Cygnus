package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.util.Components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.setup.util.PositionFormat.DECIMAL_FORMAT;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.DELETE_CLICK;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.TELEPORT_CLICK;

public class PageSlot extends AbstractDataSlot {

    private final PageResource pageResource;

    public PageSlot(PageResource pageResource) {
        super(MapDataCategory.PAGE);
        this.pageResource = pageResource;
    }

    @Override
    public ItemStack getItem() {
        ItemStack.Builder overviewItem = ItemStack.builder(type.getMaterial())
                .customName(Component.text("Page", type.getColor()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Face: ").append(Component.text(pageResource.face().name(), type.getColor())));
        lore.addAll(Components.pointToLore(MiniMessage.miniMessage(), pageResource.position(), DECIMAL_FORMAT));
        lore.add(Component.empty());
        lore.add(TELEPORT_CLICK);
        lore.add(DELETE_CLICK);
        lore.add(Component.empty());

        return overviewItem.lore(lore).build();
    }

    @Override
    protected void click(Player player, int slot, Click click, ItemStack stack, Consumer<ClickHolder> result) {
        result.accept(ClickHolder.cancelClick());

        switch (click) {
            case Click.Left _ -> {
                player.closeInventory();
                //TODO: Teleport one block in front
                player.teleport(this.pageResource.position().asPos());
            }
            case Click.Right _ ->
                    EventDispatcher.call(new PlayerRemoveDataEvent(player, type, new DialogContext.PageContent(this.pageResource)));
            default -> {
            }
        }
    }
}
