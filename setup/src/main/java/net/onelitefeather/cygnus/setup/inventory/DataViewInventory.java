package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.pageable.PageableInventory;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static de.icevizion.aves.inventory.util.InventoryConstants.CANCEL_CLICK;

public class DataViewInventory {

    private final int[] BLOCKED_SLOTS = LayoutCalculator.fillRow(InventoryType.CHEST_4_ROW);
    private final int[] DATA_SLOTS = LayoutCalculator.quad(0, InventoryType.CHEST_3_ROW.getSize() - 1);
    private final Map<GameDataType, List<Object>> dataCache;
    private final GameMap gameMap;
    private final PageableInventory pageableInventory;

    public DataViewInventory(@NotNull Player player, @NotNull GameMap gameMap) {
        this.gameMap = gameMap;
        this.dataCache = new EnumMap<>(GameDataType.class);
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_6_ROW);
        layout.setItems(BLOCKED_SLOTS, SetupItems.DECORATION, CANCEL_CLICK);
        this.pageableInventory = PageableInventory
                .builder()
                .player(player)
                .title(Component.text("Data view"))
                .type(InventoryType.CHEST_6_ROW)
                .layout(layout)
                .slotRange(DATA_SLOTS)
                .values(List.of())
                .build();

    }
}

