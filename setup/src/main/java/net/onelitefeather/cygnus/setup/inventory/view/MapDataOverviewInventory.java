package net.onelitefeather.cygnus.setup.inventory.view;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.inventory.slot.MultiStringSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.PositionSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.StringSlot;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.inventory.slot.ISlot;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.aves.map.BaseMapBuilder;

import java.util.Iterator;
import java.util.Set;

public class MapDataOverviewInventory extends PersonalInventoryBuilder {

    private final BaseMapBuilder mapBuilder;

    public MapDataOverviewInventory(Player player, BaseMapBuilder builder, InventoryMode mode) {
        super(Component.text("Lobby data"), InventoryType.CHEST_3_ROW, player);
        this.mapBuilder = builder;
        InventoryLayout backGround = InventoryLayout.fromType(getType());

        backGround.setItems(LayoutCalculator.quad(0, getType().getSize() - 1), SetupItems.DECORATION_PANE);

        setLayout(backGround);

        this.setDataLayoutFunction(dataLayoutFunction -> {
            InventoryLayout dataLayout = dataLayoutFunction == null ? InventoryLayout.fromType(getType()) : dataLayoutFunction;

            int[] slots = mode.getSlots();
            Set<MapDataCategory> categories = mode.getCategories();
            Iterator<MapDataCategory> iterator = categories.iterator();
            for (int i = 0; i < slots.length && iterator.hasNext(); i++) {
                MapDataCategory category = iterator.next();
                dataLayout.setItem(slots[i], getDataSlot(category));
            }
            return dataLayout;
        });
        this.invalidateLayout();

        register();
    }

    /**
     * Returns the slot for the given category.
     *
     * @param category to get the slot for
     * @return the slot for the given category
     */
    private ISlot getDataSlot(MapDataCategory category) {
        return switch (category) {
            case NAME ->  new StringSlot(MapDataCategory.NAME, mapBuilder.getName());
            case AUTHOR -> new MultiStringSlot(MapDataCategory.AUTHOR, mapBuilder.getBuilders());
            case SPAWN -> new PositionSlot(MapDataCategory.SPAWN, mapBuilder.getSpawn());
            case SLENDER -> new PositionSlot(MapDataCategory.SLENDER, ((GameMapBuilder) mapBuilder).getSlenderSpawn());
            default -> throw new IllegalStateException("Unknown category: " + category);
        };
    }
}
