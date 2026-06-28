package net.onelitefeather.cygnus.setup.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Tags;
import net.theevilreaper.aves.hotbar.HotBarLayout;

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
    public static final byte ZERO_INDEX = (byte) 0x00;
    public static final byte FOURTH_INDEX = (byte) 0x04;

    private static final HotBarLayout selectionLayout;
    private static final HotBarLayout lobbySetupLayout;
    private static final HotBarLayout gameSetupLayout;
    private static final HotBarLayout pageLayout;

    static {
        DECORATION_PANE = ItemStack.builder(Material.BLACK_STAINED_GLASS_PANE).customName(Component.empty()).build();
        selectionLayout = new HotBarLayout();
        selectionLayout.set(FOURTH_INDEX, ItemStack.builder(Material.CHEST)
                .customName(Component.text("Map selection", NamedTextColor.GREEN))
                .set(Tags.ITEM_TAG, ZERO_INDEX)
                .build()
        );
        ItemStack saveItem = ItemStack.builder(Material.BELL)
                .customName(Component.text("Save data", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, (byte) 0x01)
                .build();
        lobbySetupLayout = new HotBarLayout();
        lobbySetupLayout.set(2, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Data", NamedTextColor.AQUA))
                .set(Tags.ITEM_TAG, (byte) 0x02)
                .build()
        );
        lobbySetupLayout.set(6, saveItem);

        gameSetupLayout = new HotBarLayout();
        gameSetupLayout.set(2, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Data", NamedTextColor.AQUA))
                .set(Tags.ITEM_TAG, (byte) 0x02)
                .build()
        );
        gameSetupLayout.set(4, ItemStack.builder(Material.PAPER)
                .customName(Component.text("Page", NamedTextColor.AQUA))
                .set(Tags.ITEM_TAG, (byte) 0x03)
                .build()
        );
        gameSetupLayout.set(6, saveItem);

        pageLayout = new HotBarLayout();
        pageLayout.set(2, ItemStack.builder(Material.BARRIER)
                .customName(Component.text("Leave page mode", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, (byte) 0x04)
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
        player.setHeldItemSlot(FOURTH_INDEX);
    }

    /**
     * Set's the {@link ItemStack} which represents the save function into an inventory.
     *
     * @param player the player who should receive the item
     */
    public static void setLobbyLayout(Player player) {
        lobbySetupLayout.apply(player);
        player.setHeldItemSlot(ZERO_INDEX);
    }

    /**
     * Set's the {@link ItemStack} which are required for the game setup.
     *
     * @param player who should receive the items
     */
    public static void setGameLayout(Player player) {
        gameSetupLayout.apply(player);
        player.setHeldItemSlot(FOURTH_INDEX);
    }

    /**
     * Set's the {@link ItemStack} which are required for the page setup.
     *
     * @param player the player who should receive the item
     */
    public static void setPageItems(Player player) {
        pageLayout.apply(player);
        player.setHeldItemSlot(ZERO_INDEX);
    }

    private SetupItems() {
        // Nothing to do here
    }
}
