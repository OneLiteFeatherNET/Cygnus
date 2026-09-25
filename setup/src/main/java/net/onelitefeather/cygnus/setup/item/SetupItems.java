package net.onelitefeather.cygnus.setup.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Tags;
import net.theevilreaper.aves.hotbar.HotBarLayout;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.SPACE_SEPARATOR;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.getLore;

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
    private static final HotBarLayout creekRouteLayout;

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
        gameSetupLayout.set(6, ItemStack.builder(Material.LEAD)
                .customName(Component.text("Creek routes", NamedTextColor.DARK_GREEN))
                .lore(getLore(SPACE_SEPARATOR
                        .append(Component.text("Switch to the ", NamedTextColor.WHITE))
                        .append(Component.text("Creek route ", NamedTextColor.DARK_GREEN))
                        .append(Component.text("setup mode", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.CREEK_ROUTES)
                .build()
        );

        creekRouteLayout = new HotBarLayout();
        creekRouteLayout.set(1, ItemStack.builder(Material.NAME_TAG)
                .customName(Component.text("New route", NamedTextColor.GREEN))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("Create a route and name it", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.CREEK_NEW)
                .build()
        );
        creekRouteLayout.set(3, ItemStack.builder(Material.FEATHER)
                .customName(Component.text("Remove last point", NamedTextColor.YELLOW))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("Undo the last point of the active route", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.CREEK_UNDO)
                .build()
        );
        creekRouteLayout.set(5, ItemStack.builder(Material.LEAD)
                .customName(Component.text("Routes", NamedTextColor.AQUA))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("Select or delete a route", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.CREEK_LIST)
                .build()
        );
        creekRouteLayout.set(7, ItemStack.builder(Material.BARRIER)
                .customName(Component.text("Leave mode", NamedTextColor.RED))
                .lore(getLore(SPACE_SEPARATOR.append(Component.text("Exit the current mode", NamedTextColor.WHITE))))
                .set(Tags.ITEM_TAG, SetupItemId.CREEK_LEAVE)
                .build()
        );

        pageLayout = new HotBarLayout();
        pageLayout.set(2, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Pages", NamedTextColor.YELLOW))
                .lore(
                        getLore(SPACE_SEPARATOR
                                .append(Component.text("View all available ", NamedTextColor.WHITE))
                                .append(Component.text("Pages", NamedTextColor.YELLOW))
                        )
                )
                .set(Tags.ITEM_TAG, SetupItemId.PAGES)
                .build()
        );
        pageLayout.set(6, ItemStack.builder(Material.BARRIER)
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

    /**
     * Sets the items for the creek route setup.
     *
     * @param player the player
     */
    public static void setCreekRouteItems(Player player) {
        creekRouteLayout.apply(player);
        player.setHeldItemSlot(DEFAULT);
    }
}
