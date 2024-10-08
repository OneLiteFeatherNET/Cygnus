package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.inventory.slot.Slot;
import de.icevizion.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.onelitefeather.cygnus.setup.util.FormatHelper.DECIMAL_FORMAT;

public abstract class PositionBaseSlot extends Slot {

    protected final Point point;

    PositionBaseSlot(@NotNull Point point) {
        this.point = point;
    }

    protected void handleClick(
            @NotNull Player player,
            int ignore,
            @NotNull ClickType type,
            @NotNull InventoryConditionResult result
    ) {
        result.setCancel(true);
        if (!(type == ClickType.LEFT_CLICK || type == ClickType.RIGHT_CLICK)) return;

        if (type == ClickType.LEFT_CLICK) {
            player.closeInventory();
            player.teleport(((Pos) this.point));
            return;
        }

        //TODO: Open confirm dialog:
    }

    protected @NotNull List<Component> getPosLore(@NotNull Point point) {
        List<Component> components = Components.pointToLore(MiniMessage.miniMessage(), point, DECIMAL_FORMAT);
        List<Component> loreList = new ArrayList<>();
        loreList.add(Component.empty());
        loreList.addAll(components);
        loreList.add(Component.empty());
        loreList.addAll(SetupMessages.ACTION_LORE);
        return loreList;
    }
}
