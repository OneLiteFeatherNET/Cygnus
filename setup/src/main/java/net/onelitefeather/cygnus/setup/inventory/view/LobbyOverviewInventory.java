package net.onelitefeather.cygnus.setup.inventory.view;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.aves.map.BaseMapBuilder;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class LobbyOverviewInventory extends PersonalInventoryBuilder {

    private static final int[] DATA_SLOT = LayoutCalculator.from(10, 12, 14);
    private final BaseMapBuilder mapBuilder;

    public LobbyOverviewInventory(Player player, BaseMapBuilder builder) {
        super(Component.text("Lobby data"), InventoryType.CHEST_3_ROW, player);
        this.mapBuilder = builder;

        InventoryLayout backGround = InventoryLayout.fromType(getType());

        backGround.setItems(LayoutCalculator.quad(0, getType().getSize() - 1), SetupItems.DECORATION_PANE);

        setLayout(backGround);

        this.setDataLayoutFunction(dataLayoutFunction -> {
            InventoryLayout dataLayout = dataLayoutFunction == null ? InventoryLayout.fromType(getType()) : dataLayoutFunction;
            dataLayout.blank(DATA_SLOT);

            MapDataCategory[] categories = MapDataCategory.getValues();

            for (int i = 0; i < categories.length && i < DATA_SLOT.length; i++) {
                MapDataCategory currentType = categories[i];
                dataLayout.setItem(DATA_SLOT[i], getDataItem(currentType), this::handleClick);
            }
            return dataLayout;
        });

        register();
    }

    private void handleClick(Player player, int slot, Click click, ItemStack stack, Consumer<ClickHolder> consumer) {
        consumer.accept(ClickHolder.cancelClick());

        MapDataCategory category = stack.getTag(SetupTags.MAP_DATA_CATEGORY_TAG);

        if (click instanceof Click.Left) {

        }
    }

    /**
     * Returns the {@link ItemStack} for the given category.
     *
     * @param category the category to get the stack for
     * @return the stack for the given category
     */
    private ItemStack getDataItem(MapDataCategory category) {
        return switch (category) {
            case NAME -> resolveItem(category, () -> this.mapBuilder.getName().equals("Map"));
            case AUTHOR -> resolveItem(category, () -> this.mapBuilder.getBuilders().isEmpty());
            case SPAWN -> resolveItem(category, () -> this.mapBuilder.getSpawn() == null);
            default -> ItemStack.AIR;
        };
    }

    /**
     * Resolves the item for the given category based on the given condition.
     *
     * @param category  which category to resolve
     * @param condition the condition to check
     * @return the item for the given category
     */
    private ItemStack resolveItem(MapDataCategory category, BooleanSupplier condition) {
        return condition.getAsBoolean() ? MapDataCategory.getDefaultItem(category) : ItemStack.AIR;
    }
}
