package net.onelitefeather.cygnus.setup.inventory.view;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;

public class SurvivorViewInventory extends PersonalInventoryBuilder {

    /**
     * Creates a new instance from the {@link PersonalInventoryBuilder} with the given values.
     *
     * @param player    The player who owns the inventory
     */
    public SurvivorViewInventory(Player player, GameMapBuilder gameMapBuilder) {
        super(Component.text("Survivor positions"), InventoryType.CHEST_6_ROW, player);

        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), SetupItems.DECORATION_PANE);
        layout.setItems(LayoutCalculator.fillRow(getType()), SetupItems.DECORATION_PANE);

        setLayout(layout);

        register();
    }
}
