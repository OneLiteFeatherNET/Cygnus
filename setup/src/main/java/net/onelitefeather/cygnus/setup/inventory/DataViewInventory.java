package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.pageable.PageableInventory;
import de.icevizion.aves.inventory.slot.ISlot;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.inventory.slot.MultipleStringItemSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.PageItemSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.SpawnItemSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.StringItemSlot;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static de.icevizion.aves.inventory.util.InventoryConstants.CANCEL_CLICK;

public class DataViewInventory {

    private static final int[] BLOCKED_SLOTS = LayoutCalculator.fillRow(InventoryType.CHEST_5_ROW);
    private static final int[] DATA_SLOTS = LayoutCalculator.quad(0, InventoryType.CHEST_4_ROW.getSize() - 1);
    private final PageableInventory pageableInventory;
    private final ConfirmInventory confirmInventory;
    private final GameMap gameMap;
    private List<ISlot> slots;

    public DataViewInventory(@NotNull GameMap gameMap, @NotNull Player player) {
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_6_ROW);
        layout.setItems(BLOCKED_SLOTS, SetupItems.DECORATION, CANCEL_CLICK);
        this.slots = getSlots(gameMap);
        this.pageableInventory = PageableInventory
                .builder()
                .title(Component.text("Data view"))
                .type(InventoryType.CHEST_6_ROW)
                .layout(layout)
                .player(player)
                .slotRange(DATA_SLOTS)
                .values(slots)
                .build();
        this.confirmInventory = new ConfirmInventory(this::handleConfirmClick);
        this.gameMap = gameMap;
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
        MinecraftServer.getSchedulerManager().scheduleNextTick(this.pageableInventory::open);
    }

    public void openInventory() {
        pageableInventory.open();
    }

    public void clear() {
        this.pageableInventory.unregister();
    }

    private @NotNull List<ISlot> getSlots(@NotNull GameMap gameMap) {
        List<ISlot> slotMap = new ArrayList<>();

        if (gameMap.getName() != null) {
            slotMap.add(new StringItemSlot(Component.text("Name"), gameMap.getName()));
        }

        if (gameMap.getBuilders() != null && gameMap.getBuilders().length != 0) {
            String[] builders = gameMap.getBuilders();
            Component displayName = Component.text("Builders", NamedTextColor.GOLD);
            slotMap.add(new MultipleStringItemSlot(displayName, builders));
        }

        if (gameMap.hasSpawn()) {
            slotMap.add(SpawnItemSlot.asSpawn(gameMap.getSpawn(), this::openConfirmInventory));
        }

        if (gameMap.getSlenderSpawn() != null) {
            slotMap.add(SpawnItemSlot.asSlender(gameMap.getSlenderSpawn(), this::openConfirmInventory));
        }

        if (!gameMap.getSurvivorSpawns().isEmpty()) {
            for (Pos survivorSpawn : gameMap.getSurvivorSpawns()) {
                slotMap.add(SpawnItemSlot.asSurvivor(survivorSpawn, this::openConfirmInventory));
            }
        }

        if (!gameMap.getPageFaces().isEmpty()) {
            for (PageResource pageFace : gameMap.getPageFaces()) {
                slotMap.add(new PageItemSlot(pageFace, this::openConfirmInventory));
            }
        }

        return slotMap;
    }

    public void update() {
        this.pageableInventory.remove(this.slots);
        this.slots = getSlots(gameMap);
        this.pageableInventory.add(this.slots);
    }
}
