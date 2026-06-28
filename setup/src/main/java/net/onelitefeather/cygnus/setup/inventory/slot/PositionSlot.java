package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.util.Components;
import org.jetbrains.annotations.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.DELETE_CLICK;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.TELEPORT_CLICK;

public class PositionSlot extends AbstractDataSlot {

    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("#.##");
        DECIMAL_FORMAT.setRoundingMode(RoundingMode.CEILING);
        DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    private final @Nullable Pos position;

    public PositionSlot(MapDataCategory category, @Nullable Pos position) {
        super(category);
        this.position = position;
    }

    @Override
    public ItemStack getItem() {
        ItemStack overviewItem = MapDataCategory.getDefaultItem(type);

        if (position == null) {
            return overviewItem;
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.addAll(Components.pointToLore(MiniMessage.miniMessage(), position, DECIMAL_FORMAT));
        lore.add(Component.empty());
        lore.add(TELEPORT_CLICK);
        lore.add(DELETE_CLICK);
        lore.add(Component.empty());

        return asBuilder(overviewItem).lore(lore).build();
    }

    @Override
    protected void click(Player player, int slot, Click click, ItemStack stack, Consumer<ClickHolder> result) {
        result.accept(ClickHolder.cancelClick());
        if ((!(click instanceof Click.Left || click instanceof Click.Right)) || position == null) return;
        if (click instanceof Click.Left) {
            player.closeInventory();
            player.teleport(position);
            return;
        }
        EventDispatcher.call(new DialogRequestEvent(player, DialogTarget.DELETE_SPAWN, new DialogContext.PositionContent(position)));
    }
}
