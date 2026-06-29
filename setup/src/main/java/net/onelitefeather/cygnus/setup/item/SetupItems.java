package net.onelitefeather.cygnus.setup.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.theevilreaper.aves.hotbar.HotBarLayout;

import java.util.ArrayList;
import java.util.List;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.SPACE_SEPARATOR;

/**
 * The class holds the {@link ItemStack} references which have some functionality during a setup process from an map.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("java:S3252")
public final class SetupItems {

    public static final ItemStack DECORATION_PANE;
    public static final byte DEFAULT = (byte) 0x00;
    public static final byte SELECTION = (byte) 0x04;
    private static final HotBarLayout selectionLayout;
    private static final HotBarLayout lobbySetupLayout;
    private static final HotBarLayout gameSetupLayout;
    private static final HotBarLayout pageLayout;
    private static final HotBarLayout survivorSpawnLayout;

    static {
        DECORATION_PANE = ItemStack.builder(Material.BLACK_STAINED_GLASS_PANE)
                .customName(Component.empty())
                .build();

        ItemStack saveItem = ItemStack.builder(Material.BELL)
                .customName(Component.text("Save data", NamedTextColor.RED))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("Save your current progress of the map", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.SAVE_DATA)
                .build();

        selectionLayout = new HotBarLayout();
        selectionLayout.set(SELECTION, ItemStack.builder(Material.CHEST)
                .customName(Component.text("Map selection", NamedTextColor.GREEN))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("Chose your map to setup", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.MAP_SELECTION)
                .build()
        );

        lobbySetupLayout = new HotBarLayout();
        lobbySetupLayout.set(2, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Data", NamedTextColor.AQUA))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("View basic map data", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.DATA)
                .build()
        );
        lobbySetupLayout.set(6, saveItem);

        gameSetupLayout = new HotBarLayout();
        gameSetupLayout.set(1, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Data", NamedTextColor.AQUA))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("View basic map data", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.DATA)
                .build()
        );
        gameSetupLayout.set(3, ItemStack.builder(Material.PAPER)
                .customName(Component.text("Page", NamedTextColor.AQUA))
                .lore(
                        getLore(
                                SPACE_SEPARATOR
                                        .append(Component.text("Switch to the ", NamedTextColor.WHITE)
                                                .append(Component.text("Page ", NamedTextColor.AQUA))
                                                .append(Component.text("setup mode", NamedTextColor.WHITE))
                                        )
                        )
                )
                .set(Tags.ITEM_TAG, SetupItemId.PAGE)
                .build()
        );

        gameSetupLayout.set(5, ItemStack.builder(Material.MINECART)
                .customName(Component.text("Survivor", NamedTextColor.GREEN))
                .lore(
                        getLore(
                                SPACE_SEPARATOR
                                        .append(Component.text("Switch to the ", NamedTextColor.WHITE)
                                                .append(Component.text("Survivor ", NamedTextColor.GREEN))
                                                .append(Component.text("setup mode", NamedTextColor.WHITE)))
                        )
                )
                .set(Tags.ITEM_TAG, SetupItemId.SURVIVOR)
                .build()
        );
        gameSetupLayout.set(7, saveItem);

        pageLayout = new HotBarLayout();
        pageLayout.set(4, ItemStack.builder(Material.BARRIER)
                .customName(Component.text("Leave mode", NamedTextColor.RED))
                .lore(
                        getLore(
                                SPACE_SEPARATOR.append(
                                        Component.text("Exit the current mode", NamedTextColor.WHITE)
                                )
                        )
                )
                .set(Tags.ITEM_TAG, SetupItemId.LEAVE_PAGE)
                .build()
        );

        survivorSpawnLayout = new HotBarLayout();
        survivorSpawnLayout.set(2, ItemStack.builder(Material.CHEST)
                        .customName(Component.text("Spawns", NamedTextColor.AQUA))
                        .lore(
                                getLore(SPACE_SEPARATOR
                                        .append(Component.text("View all available ", NamedTextColor.WHITE))
                                        .append(Component.text("Survivor ", NamedTextColor.GREEN))
                                        .append(Component.text("spawns", NamedTextColor.WHITE)))
                )
                .set(Tags.ITEM_TAG, SetupItemId.SPAWNS)
                .build()
        );
        survivorSpawnLayout.set(6, ItemStack.builder(Material.BARRIER)
                .customName(Component.text("Leave mode", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, SetupItemId.LEAVE_MODE)
                .lore(
                        getLore(
                                SPACE_SEPARATOR.append(
                                        Component.text("Exit the current mode", NamedTextColor.WHITE)
                                )
                        )
                )
                .build()
        );
    }

    /**
     * Set's the {@link ItemStack} which represents the map selection into an inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setMapSelection(Player player) {
        selectionLayout.apply(player);
        player.setHeldItemSlot(SELECTION);
    }

    /**
     * Set's the {@link ItemStack} which represents the save function into an inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setLobbyLayout(Player player) {
        lobbySetupLayout.apply(player);
        player.setHeldItemSlot(DEFAULT);
    }

    /**
     * Set's the {@link ItemStack} which are required for the game setup.
     *
     * @param player who should receive the items
     */
    public static void setGameLayout(Player player) {
        gameSetupLayout.apply(player);
        player.setHeldItemSlot(SELECTION);
    }

    /**
     * Set's the {@link ItemStack} which are required for the survivor setup.
     *
     * @param player who should receive the items
     */
    public static void setSurvivorSpawn(Player player) {
        survivorSpawnLayout.apply(player);
        player.setHeldItemSlot(DEFAULT);
    }

    /**
     * Set's the {@link ItemStack} which are required for the page setup.
     *
     * @param player the player who should receive the item
     */
    public static void setPageItems(Player player) {
        pageLayout.apply(player);
        player.setHeldItemSlot(DEFAULT);
    }

    private SetupItems() {
    }

    private static List<Component> getLore(Component... components) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.addAll(List.of(components));
        lore.add(Component.empty());
        return lore;
    }
}