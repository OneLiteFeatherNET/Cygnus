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

    private static final ItemStack slenderEye = ItemStack.builder(Material.ENDER_EYE)
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
                    "Survivors close to it glow for you", "Sneak to return to your body"))
            .set(Tags.ITEM_TAG, POSSESSION_ITEM)
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
