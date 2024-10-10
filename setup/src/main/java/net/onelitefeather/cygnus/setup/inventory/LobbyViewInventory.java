package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.GlobalInventoryBuilder;
import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.slot.ISlot;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import de.icevizion.aves.map.BaseMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.setup.inventory.slot.MultipleStringItemSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.SpawnItemSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.StringItemSlot;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import org.jetbrains.annotations.NotNull;

import static de.icevizion.aves.inventory.util.InventoryConstants.CANCEL_CLICK;

/**
 * The {@link LobbyViewInventory} is used to display the data from a lobby map.
 * It shows the name, spawn and builders of the map.
 * The inventory is used during the setup process.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @see GlobalInventoryBuilder
 * @since 1.0.0
 */
@SuppressWarnings("java:S3252")
public class LobbyViewInventory extends GlobalInventoryBuilder {

    private static final ItemStack NO_SPAWN = ItemStack.builder(Material.BARRIER)
            .customName(Component.text("No spawn set", NamedTextColor.RED))
            .build();

    private static final int[] DATA_SLOTS = LayoutCalculator.from(11, 13, 15);

    private final BaseMap lobbyMap;

    /**
     * Creates a new {@link LobbyViewInventory} instance.
     *
     * @param lobbyMap the map to display
     */
    public LobbyViewInventory(@NotNull BaseMap lobbyMap) {
        super(Component.text("Lobby data"), InventoryType.CHEST_3_ROW);
        this.lobbyMap = lobbyMap;
        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(LayoutCalculator.quad(0, getType().getSize() - 1), SetupItems.DECORATION, CANCEL_CLICK);
        this.setLayout(layout);

        this.setDataLayoutFunction(dataLayout -> {
            dataLayout = dataLayout == null ? InventoryLayout.fromType(getType()) : dataLayout;
            dataLayout.blank(DATA_SLOTS);
            if (hasNoData()) {
                dataLayout.setItem(DATA_SLOTS[0], SetupItems.DECORATION, CANCEL_CLICK);
                dataLayout.setItem(DATA_SLOTS[1], NO_SPAWN, CANCEL_CLICK);
                dataLayout.setItem(DATA_SLOTS[2], SetupItems.DECORATION, CANCEL_CLICK);
                return dataLayout;
            }
            ISlot mapNameSlot = new StringItemSlot(Component.text("Map-Name", NamedTextColor.GOLD), lobbyMap.getName());
            ISlot builderSlot = new MultipleStringItemSlot(Component.text("Builders", NamedTextColor.GOLD), lobbyMap.getBuilders());
            ISlot spawnSlot = new SpawnItemSlot(lobbyMap.getSpawn());
            dataLayout.setItem(DATA_SLOTS[0], mapNameSlot, CANCEL_CLICK);
            dataLayout.setItem(DATA_SLOTS[1], spawnSlot);
            dataLayout.setItem(DATA_SLOTS[2], builderSlot, CANCEL_CLICK);
            return dataLayout;
        });

        this.invalidateLayout();
        this.invalidateDataLayout();
        this.register();
    }

    /**
     * Handles the click logic when a {@link Player} interacts with the {@link ItemStack} that represents the spawn of a map.
     *
     * @param player    the player who clicked
     * @param slot      the slot index
     * @param clickType the type of the click
     * @param result    the result of the click
     */
    private void handleSpawnClick(@NotNull Player player, int slot, @NotNull ClickType clickType, @NotNull InventoryConditionResult result) {
        result.setCancel(true);

        if (!(clickType == ClickType.LEFT_CLICK || clickType == ClickType.RIGHT_CLICK)) return;

        if (clickType == ClickType.LEFT_CLICK && this.lobbyMap.hasSpawn()) {
            player.closeInventory();
            player.teleport(this.lobbyMap.getSpawn());
            return;
        }

        player.closeInventory();
        ConfirmInventory confirmInventory = new ConfirmInventory(() -> {
            this.lobbyMap.setSpawn(null);
            this.invalidateDataLayout();
        });
        player.openInventory(confirmInventory.getInventory());
    }

    /**
     * Checks if the map has no data.
     *
     * @return true if the map has no data otherwise false
     */
    private boolean hasNoData() {
        boolean hasMapName = this.lobbyMap.getName() != null && !this.lobbyMap.getName().isEmpty();
        return !this.lobbyMap.hasSpawn() && !hasMapName && (this.lobbyMap.getBuilders() == null || this.lobbyMap.getBuilders().length == 0);
    }
}
