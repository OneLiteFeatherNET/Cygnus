package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.GlobalInventoryBuilder;
import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.slot.ISlot;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import de.icevizion.aves.map.BaseMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
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
    private final ConfirmInventory confirmInventory;

    /**
     * Creates a new {@link LobbyViewInventory} instance.
     *
     * @param lobbyMap the map to display
     */
    public LobbyViewInventory(@NotNull BaseMap lobbyMap) {
        super(Component.text("Lobby data"), InventoryType.CHEST_3_ROW);
        this.lobbyMap = lobbyMap;
        this.confirmInventory = new ConfirmInventory(this::handleConfirmClick);
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
            ISlot spawnSlot;

            if (!lobbyMap.hasSpawn()) {
                 spawnSlot = SpawnItemSlot.empty();
            } else {
                spawnSlot = SpawnItemSlot.asSpawn(lobbyMap.getSpawn(), this::openConfirmInventory);
            }

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
     * Opens the confirmation inventory.
     *
     * @param player the player to open the inventory
     */
    private void openConfirmInventory(@NotNull Player player) {
        player.closeInventory();
        confirmInventory.register();
        player.openInventory(confirmInventory.getInventory());
    }

    /**
     * Handles the confirmation logic.
     *
     * @param player the player who clicked
     */
    private void handleConfirmClick(@NotNull Player player) {
        player.closeInventory();
        lobbyMap.setSpawn(null);
        System.out.println("MEEPO");
        invalidateDataLayout();
        System.out.println("MEEPO2");
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> player.openInventory(this.getInventory()));
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
