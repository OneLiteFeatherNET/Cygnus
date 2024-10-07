package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.GlobalInventoryBuilder;
import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.InventorySlot;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import de.icevizion.aves.map.BaseMap;
import de.icevizion.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import org.jetbrains.annotations.NotNull;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static de.icevizion.aves.inventory.util.InventoryConstants.CANCEL_CLICK;
import static net.onelitefeather.cygnus.setup.util.FormatHelper.DECIMAL_FORMAT;

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

    private static final ItemStack NO_MAP_NAME = ItemStack.builder(Material.OAK_SIGN)
            .customName(Component.text("No map name", NamedTextColor.RED))
            .build();
    private static final ItemStack NO_SPAWN = ItemStack.builder(Material.BARRIER)
            .customName(Component.text("No spawn set", NamedTextColor.RED))
            .build();

    private static final ItemStack NO_BUILDERS = ItemStack.builder(Material.DARK_OAK_SIGN)
            .customName(Component.text("No builders set", NamedTextColor.RED))
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
                dataLayout.setItem(DATA_SLOTS[1], SetupItems.NO_DATA, CANCEL_CLICK);
                dataLayout.setItem(DATA_SLOTS[2], SetupItems.DECORATION, CANCEL_CLICK);
                return dataLayout;
            }

            dataLayout.setItem(DATA_SLOTS[0], getMapNameSlot(this.lobbyMap), CANCEL_CLICK);
            dataLayout.setItem(DATA_SLOTS[1], getSpawnSlot(this.lobbyMap));
            dataLayout.setItem(DATA_SLOTS[2], getBuilderSlot(this.lobbyMap), CANCEL_CLICK);
            return dataLayout;
        });

        this.invalidateLayout();
        this.invalidateDataLayout();
        this.register();
    }

    /**
     * Returns the map name slot for the map.
     *
     * @param baseMap the map to get the name from
     * @return the map name slot
     */
    private @NotNull InventorySlot getMapNameSlot(@NotNull BaseMap baseMap) {
        if (baseMap.getName() == null || baseMap.getName().isEmpty()) {
            return new InventorySlot(NO_MAP_NAME);
        }

        Component mapName = Component.text("Name:", NamedTextColor.GRAY)
                .append(Component.space())
                .append(Component.text(baseMap.getName(), NamedTextColor.GOLD));

        return new InventorySlot(ItemStack.builder(Material.OAK_SIGN)
                .customName(mapName)
                .build()
        );
    }

    /**
     * Returns the spawn slot for the map.
     *
     * @param baseMap the map to get the spawn from
     * @return the spawn slot
     */
    private @NotNull InventorySlot getSpawnSlot(@NotNull BaseMap baseMap) {
        if (!baseMap.hasSpawn()) return new InventorySlot(NO_SPAWN);
        List<Component> components = Components.pointToLore(MiniMessage.miniMessage(), baseMap.getSpawn(), DECIMAL_FORMAT);
        List<Component> loreList = new ArrayList<>();
        loreList.add(Component.empty());
        loreList.addAll(components);
        loreList.add(Component.empty());
        loreList.addAll(SetupMessages.ACTION_LORE);
        return new InventorySlot(ItemStack.builder(Material.ENDER_EYE)
                .customName(Component.text("Spawn", NamedTextColor.GOLD))
                .lore(loreList)
                .build(),
                this::handleSpawnClick
        );
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
     * Returns the builder slot for the map.
     *
     * @param baseMap the map to get the builders from
     * @return the builder slot
     */
    private @NotNull InventorySlot getBuilderSlot(@NotNull BaseMap baseMap) {
        if (baseMap.getBuilders() == null || baseMap.getBuilders().length == 0) return new InventorySlot(NO_BUILDERS);
        List<Component> builders = new ArrayList<>();
        builders.add(Component.empty());
        for (int i = 0; i < baseMap.getBuilders().length; i++) {
            String builder = baseMap.getBuilders()[i];
            if (builder == null) continue;
            Component component = Component.text("-", NamedTextColor.GRAY)
                    .append(Component.space())
                    .append(Component.text(builder, NamedTextColor.GOLD));
            builders.add(component);
        }
        builders.add(Component.empty());
        return new InventorySlot(ItemStack.builder(Material.DARK_OAK_SIGN)
                .customName(Component.text("Builders", NamedTextColor.GOLD))
                .lore(builders)
                .build()
        );
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
