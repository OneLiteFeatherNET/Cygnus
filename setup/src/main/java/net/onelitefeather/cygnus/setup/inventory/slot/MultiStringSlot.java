package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.click.ClickHolder;

import java.util.List;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.DELETE_CLICK;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.NO_SPACE_SEPARATOR;

public class MultiStringSlot extends AbstractDataSlot {

    private final List<String> data;

    public MultiStringSlot(MapDataCategory category, List<String> data) {
        super(category);
        this.data = data;
    }

    public MultiStringSlot(MapDataCategory category) {
        super(category);
        this.data = List.of();
    }

    @Override
    public ItemStack getItem() {
        ItemStack overviewItem = MapDataCategory.getDefaultItem(type);

        if (data == null || data.isEmpty()) {
            return overviewItem;
        }
        return asBuilder(overviewItem).lore(
                        Component.empty(),
                        NO_SPACE_SEPARATOR.append(Component.space()).append(Component.text(String.join(", ", data), type.getColor())),
                        Component.empty(),
                        DELETE_CLICK,
                        Component.empty()
                )
                .build();
    }

    @Override
    protected void click(Player player, int slot, Click click, ItemStack stack, Consumer<ClickHolder> result) {
        result.accept(ClickHolder.cancelClick());

        if (data.isEmpty()) {
            EventDispatcher.call(new DialogRequestEvent(player, DialogTarget.CREATE_AUTHORS));
            return;
        }

        if (click instanceof Click.Right) {
            EventDispatcher.call(new PlayerRemoveDataEvent(player, type));
        }
    }
}
