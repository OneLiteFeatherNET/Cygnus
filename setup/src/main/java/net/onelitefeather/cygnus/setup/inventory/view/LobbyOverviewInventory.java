package net.onelitefeather.cygnus.setup.inventory.view;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.util.exception.ThrowingException;
import net.theevilreaper.aves.util.functional.ThrowingFunction;
import org.jetbrains.annotations.NotNull;

public class LobbyOverviewInventory extends PersonalInventoryBuilder {

    private static final int[] DATA_SLOT = LayoutCalculator.from(10, 12, 14);

    public LobbyOverviewInventory(Player player, BaseMapBuilder builder) {
        super(Component.text("Lobby data"), InventoryType.CHEST_3_ROW, player);

        InventoryLayout backGround = InventoryLayout.fromType(getType());

        backGround.setItems(LayoutCalculator.quad(0, getType().getSize() - 1), SetupItems.DECORATION_PANE);

        setLayout(backGround);

        this.setDataLayoutFunction(dataLayoutFunction -> {
            InventoryLayout dataLayout = dataLayoutFunction == null ? InventoryLayout.fromType(getType()) : dataLayoutFunction;
            dataLayout.blank(DATA_SLOT);

            MapDataCategory[] categories = MapDataCategory.getValues();

            for (int i = 0; i < categories.length && i < DATA_SLOT.length; i++) {
                MapDataCategory currentType = categories[i];
                dataLayout.setItem(DATA_SLOT[i], getOverViewItem(currentType));
            }
            return dataLayout;
        });

        register();
    }
}
