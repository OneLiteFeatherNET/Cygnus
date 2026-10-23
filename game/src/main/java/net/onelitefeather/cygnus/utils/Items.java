package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Tags;
import net.theevilreaper.aves.hotbar.HotBarLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The class contains each {@link ItemStack} reference which is required for the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings({"java:S3252"})
public final class Items {

    public static final byte SLENDER_ITEM = (byte) 0x00;
    public static final byte SPECTATE_ITEM = (byte) 0x01;
    public static final byte LEAVE_ITEM = (byte) 0x02;
    public static final byte TRACKING_ITEM = (byte) 0x03;
    public static final byte POSSESSION_ITEM = (byte) 0x04;
    public static final byte HEAL_ITEM = (byte) 0x05;
    public static final byte STATS_ITEM = (byte) 0x06;
    public static final byte SERVER_ITEM = (byte) 0x07;
    public static final byte LOBBY_ITEM = (byte) 0x08;

    /** The tracker's material, which is also the client's cooldown group for it. */
    public static final Material TRACKING_MATERIAL = Material.RABBIT_FOOT;

    /** The hotbar slot of the tracker, the second to last one, away from the SlenderEye. */
    public static final int TRACKING_SLOT = 7;

    /**
     * The possession item's material, which is also the client's cooldown group for it. Not a block
     * item, so a right click against a block does not try to place it.
     */
    public static final Material POSSESSION_MATERIAL = Material.HEART_OF_THE_SEA;

    /** The hotbar slot of the possession item, the last one, next to the tracker. */
    public static final int POSSESSION_SLOT = 8;

    /** The hotbar slot of the survivors' painkillers, the last one, away from the pages. */
    public static final int HEAL_SLOT = 8;

    /** The hotbar slot of the lobby's statistics item, the middle one. */
    public static final int STATS_SLOT = 4;

    /** The hotbar slot of the lobby's server item, the first one. */
    public static final int SERVER_SLOT = 0;

    /** The hotbar slot of the lobby's item that leads back to the network lobby, the last one. */
    public static final int LOBBY_SLOT = 8;

    private static final ItemStack lobbyReturn = ItemStack.builder(Material.DARK_OAK_DOOR)
            .customName(Component.text("Back to Lobby", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            .lore(lore("Right click to leave this", "round and return to the lobby"))
            .set(Tags.ITEM_TAG, LOBBY_ITEM)
            .build();

    private static final ItemStack slenderEye =ItemStack.builder(Material.ENDER_EYE)
            .customName(Component.text("SlenderEye").color(TextColor.fromHexString("#ff00d4")))
            .lore(lore("Right click to hide yourself", "or to show yourself again"))
            .set(Tags.ITEM_TAG, SLENDER_ITEM)
            .build();

    private static final ItemStack tracker = ItemStack.builder(TRACKING_MATERIAL)
            .customName(Component.text("Tracker", NamedTextColor.DARK_RED))
            .lore(lore("Right click to reveal where survivors", "walked around you a little while ago",
                    "Only you can see the tracks"))
            .set(Tags.ITEM_TAG, TRACKING_ITEM)
            .build();

    private static final ItemStack possession = ItemStack.builder(POSSESSION_MATERIAL)
            .customName(Component.text("Creek's Eyes", NamedTextColor.DARK_GREEN))
            .lore(lore("Right click while hidden to look", "through the eyes of the creek",
                    "Survivors close to it show on your locator bar", "Sneak to return to your body"))
            .set(Tags.ITEM_TAG, POSSESSION_ITEM)
            .build();

    private static final ItemStack painkillers = ItemStack.builder(Material.SUGAR)
            .customName(Component.text("Painkillers", NamedTextColor.WHITE))
            .lore(lore("Right click to win back some health", "Only once per round",
                    "A hit takes from what they give back"))
            .set(Tags.ITEM_TAG, HEAL_ITEM)
            .build();

    private static final ItemStack statistics = ItemStack.builder(Material.BOOK)
            .customName(Component.text("Statistics", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
            .lore(lore("Right click to open your", "statistics of this year"))
            .set(Tags.ITEM_TAG, STATS_ITEM)
            .build();

    private static final HotBarLayout SPECTATOR_LAYOUT;

    static {
        SPECTATOR_LAYOUT = new HotBarLayout();
        SPECTATOR_LAYOUT.set(2, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Spectate"))
                .set(Tags.ITEM_TAG, SPECTATE_ITEM)
                .build()
        );

        SPECTATOR_LAYOUT.set(6, ItemStack.builder(Material.OAK_DOOR)
                .customName(Component.text("Leave", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, LEAVE_ITEM)
                .build()
        );
    }

    /**
     * Gives the player the slender's items, the SlenderEye and the tracker, and the slender's look.
     *
     * @param player who should receive the item
     */
    public static void setSlenderItems(Player player) {
        player.getInventory().clear();
        player.getInventory().addItemStack(slenderEye);
        player.getInventory().setItemStack(TRACKING_SLOT, tracker);
        player.switchEntityType(EntityType.ENDERMAN);
    }

    /**
     * Gives the slender the item to look through the creek's eyes. Only handed out while the creek
     * takes part in the round.
     *
     * @param player the slender
     */
    public static void setPossessionItem(Player player) {
        player.getInventory().setItemStack(POSSESSION_SLOT, possession);
    }

    /**
     * Gives a survivor the painkillers for the round.
     *
     * @param player the survivor
     */
    public static void setHealItem(Player player) {
        player.getInventory().setItemStack(HEAL_SLOT, painkillers);
    }

    /**
     * Gives the player the lobby's statistics item.
     *
     * @param player who should receive the item
     */
    public static void setStatsItem(Player player) {
        player.getInventory().setItemStack(STATS_SLOT, statistics);
    }

    /**
     * Takes the statistics item out of the player's inventory, wherever it ended up. Everything else,
     * the painkillers included, stays.
     *
     * @param player who should lose the item
     */
    public static void removeStatsItem(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isStatsItem(inventory.getItemStack(slot))) {
                inventory.setItemStack(slot, ItemStack.AIR);
            }
        }
    }

    /**
     * Tells whether the stack is the statistics item.
     *
     * @param stack the stack to look at
     * @return {@code true} if the stack carries the statistics tag
     */
    public static boolean isStatsItem(ItemStack stack) {
        Byte tag = stack.getTag(Tags.ITEM_TAG);
        return tag != null && tag == STATS_ITEM;
    }

    /**
     * Tells whether the player carries the statistics item anywhere in their inventory.
     *
     * @param player the player to look at
     * @return {@code true} if one slot holds it
     */
    public static boolean hasStatsItem(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isStatsItem(inventory.getItemStack(slot))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives the player the lobby's item that shows the server they are on.
     *
     * @param player     who should receive the item
     * @param serverName the name of the server, shown on the item
     * @since 2.19.0
     */
    public static void setServerItem(Player player, String serverName) {
        player.getInventory().setItemStack(SERVER_SLOT, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Server: ", NamedTextColor.GOLD)
                        .append(Component.text(serverName, NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false))
                .lore(lore("Right click to see which", "server you are playing on"))
                .set(Tags.ITEM_TAG, SERVER_ITEM)
                .build());
    }

    /**
     * Gives the player the lobby's item that leads back to the network lobby.
     *
     * @param player who should receive the item
     * @since 2.19.0
     */
    public static void setLobbyItem(Player player) {
        player.getInventory().setItemStack(LOBBY_SLOT, lobbyReturn);
    }

    /**
     * Takes the server item and the lobby item out of the player's inventory, wherever they ended up.
     *
     * @param player who should lose the items
     * @since 2.19.0
     */
    public static void removeLobbyItems(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (isServerItem(stack) || isLobbyItem(stack)) {
                inventory.setItemStack(slot, ItemStack.AIR);
            }
        }
    }

    /**
     * Tells whether the stack is the server item.
     *
     * @param stack the stack to look at
     * @return {@code true} if the stack carries the server tag
     * @since 2.19.0
     */
    public static boolean isServerItem(ItemStack stack) {
        Byte tag = stack.getTag(Tags.ITEM_TAG);
        return tag != null && tag == SERVER_ITEM;
    }

    /**
     * Tells whether the stack is the item that leads back to the lobby.
     *
     * @param stack the stack to look at
     * @return {@code true} if the stack carries the lobby tag
     * @since 2.19.0
     */
    public static boolean isLobbyItem(ItemStack stack) {
        Byte tag = stack.getTag(Tags.ITEM_TAG);
        return tag != null && tag == LOBBY_ITEM;
    }

    /**
     * Sets the spectator layout to a given {@link Player}.
     *
     * @param player to set the items
     */
    public static void setSpectatorLayout(Player player) {
        SPECTATOR_LAYOUT.apply(player, true);
    }

    /**
     * Builds an item description: gray, upright lines between two empty ones, like the other menus.
     * The cooldowns are left out, the client draws them on the item itself.
     *
     * @param lines the lines of the description
     * @return the lore
     */
    private static List<Component> lore(String... lines) {
        List<Component> lore = new ArrayList<>(lines.length + 2);
        lore.add(Component.empty());
        for (String line : lines) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        return lore;
    }

    private Items() {
        // Nothing do to here
    }
}
