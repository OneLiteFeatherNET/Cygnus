package net.onelitefeather.cygnus.setup.inventory.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.inventory.slot.PositionSlot;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;

import java.util.Iterator;

public class SurvivorViewInventory extends PersonalInventoryBuilder {

    private static final int[] SLOTS = LayoutCalculator.quad(InventoryType.CHEST_1_ROW.getSize() + 1, InventoryType.CHEST_5_ROW.getSize());
    private final GameMapBuilder mapBuilder;

    /**
     * Creates a new instance from the {@link PersonalInventoryBuilder} with the given values.
     *
     * @param player    The player who owns the inventory
     */
    public SurvivorViewInventory(Player player, GameMapBuilder mapBuilder) {
        super(Component.text("Survivor positions"), InventoryType.CHEST_6_ROW, player);
        this.mapBuilder = mapBuilder;

        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), SetupItems.DECORATION_PANE);
        layout.setItems(LayoutCalculator.fillRow(getType()), SetupItems.DECORATION_PANE);

        setLayout(layout);

        this.setDataLayoutFunction(dataLayout -> {
             dataLayout = dataLayout != null ? dataLayout : InventoryLayout.fromType(getType());

             if (this.mapBuilder.getSurvivorSpawns().isEmpty()) return dataLayout;

            Iterator<Pos> iterator = mapBuilder.getSurvivorSpawns().iterator();

            for (int i = 0; i < SLOTS.length && iterator.hasNext(); i++) {
                Pos pos = iterator.next();
                dataLayout.setItem(SLOTS[i], new PositionSlot(MapDataCategory.SPAWN, pos));
            }
            return dataLayout;
        });

        register();
    }
}
