package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.GlobalInventoryBuilder;
import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("java:S3252")
public class MapSetupInventory extends GlobalInventoryBuilder {

    private static final List<Component> LORE_COMPONENTS = List.of(
            Component.text("Setup as"),
            Component.newline(),
            Component.text("Lobby (LeftClick)"),
            Component.newline(),
            Component.text("Game (RightClick)")
    );
    private static final int[] MAP_SLOTS = LayoutCalculator.repeat(InventoryType.CHEST_1_ROW.getSize(), InventoryType.CHEST_3_ROW.getSize());

    public MapSetupInventory(@NotNull List<MapEntry> maps) {
        super(Component.text("Select map"), InventoryType.CHEST_4_ROW);

        var layout = InventoryLayout.fromType(getType());
        var decoration = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.text("")).build();
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), decoration);
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_4_ROW), decoration);

        this.setLayout(layout);

        if (maps.isEmpty()) {
            return;
        }
        setDataLayoutFunction(dataLayoutFunction -> {
            var dataLayout = dataLayoutFunction == null ? InventoryLayout.fromType(getType()) : dataLayoutFunction;

            dataLayout.blank(MAP_SLOTS);

            for (int i = 0; i < maps.size(); i++) {
                var currentMap = maps.get(i);
                dataLayout.setItem(MAP_SLOTS[i], getMapItem(currentMap.path()), (player, slot, clickType, inventoryConditionResult) -> {
                    inventoryConditionResult.setCancel(true);
                    if (clickType != ClickType.LEFT_CLICK && clickType != ClickType.RIGHT_CLICK) return;
                    var mode = clickType == ClickType.LEFT_CLICK ? SetupMode.LOBBY : SetupMode.GAME;
                    EventDispatcher.callCancellable(new MapSetupSelectEvent(player, currentMap, mode), player::closeInventory);
                });
            }
            return dataLayout;
        });
        this.invalidateDataLayout();
        this.register();
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull ItemStack getMapItem(@NotNull Path path) {
        return ItemStack.builder(Material.PAPER)
                .lore(LORE_COMPONENTS)
                .customName(Component.text(path.getFileName().toString()))
                .build();
    }

    public void open(@NotNull Player player) {
        player.openInventory(this.getInventory());
    }
}
