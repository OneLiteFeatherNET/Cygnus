package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.Tags;
import net.theevilreaper.aves.hotbar.HotBarLayout;

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

    private static final ItemStack slenderEye = ItemStack.builder(Material.ENDER_EYE)
            .customName(Component.text("SlenderEye").color(TextColor.fromHexString("#ff00d4")))
            .set(Tags.ITEM_TAG, SLENDER_ITEM)
            .build();

    private static final HotBarLayout SPECTATOR_LAYOUT;

    static {
        SPECTATOR_LAYOUT = new HotBarLayout();
        SPECTATOR_LAYOUT.set(2, ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Spectate"))
                .set(Tags.ITEM_TAG, SPECTATE_ITEM)
                .build()
        );

        SPECTATOR_LAYOUT.set(5, ItemStack.builder(Material.OAK_DOOR)
                .customName(Component.text("Leave", NamedTextColor.RED))
                .set(Tags.ITEM_TAG, LEAVE_ITEM)
                .build()
        );
    }

    /**
     * Sets the {@link ItemStack} for the SlenderEye to the player inventory.
     *
     * @param player who should receive the item
     */
    public static void setSlenderEye(Player player) {
        player.getInventory().clear();
        player.getInventory().addItemStack(slenderEye);
        player.switchEntityType(EntityType.ENDERMAN);
    }

    /**
     * Sets the spectator layout to a given {@link Player}.
     *
     * @param player to set the items
     */
    public static void setSpectatorLayout(Player player) {
        SPECTATOR_LAYOUT.apply(player, true);
    }

    private Items() {
        // Nothing do to here
    }
}
