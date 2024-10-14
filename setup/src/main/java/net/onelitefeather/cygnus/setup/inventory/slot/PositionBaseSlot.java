package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.inventory.slot.Slot;
import de.icevizion.aves.util.functional.PlayerConsumer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import org.jetbrains.annotations.NotNull;

public abstract class PositionBaseSlot extends Slot {

    protected final Point point;
    protected final PlayerConsumer rightClickAction;

    PositionBaseSlot(@NotNull Point point, @NotNull PlayerConsumer rightClickAction) {
        this.point = point;
        this.rightClickAction = rightClickAction;
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
            player.teleport(Pos.fromPoint(this.point));
            return;
        }

        if (type == ClickType.RIGHT_CLICK && this.rightClickAction != null) {
            this.rightClickAction.accept(player);
        }
    }
}
