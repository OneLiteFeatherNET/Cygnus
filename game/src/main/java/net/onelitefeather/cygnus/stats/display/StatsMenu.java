package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;
import net.onelitefeather.cygnus.stats.YearStatsView;

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only chest menu with one item per area of a player's year: general, survivor and Slender.
 * <p>
 * The menu is a plain {@link Inventory} of its own type so the {@link StatsDisplayFeature} can tell it
 * apart and cancel every click in it. It is built from an immutable view, so it does not change while it
 * is open.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class StatsMenu extends Inventory {

    /** Where the player's head sits, the middle of the top row. */
    static final int HEAD_SLOT = 4;
    /** Where the general area sits. */
    static final int GENERAL_SLOT = 11;
    /** Where the survivor area sits. */
    static final int SURVIVOR_SLOT = 13;
    /** Where the Slender area sits. */
    static final int SLENDER_SLOT = 15;

    private static final ItemStack DECORATION_PANE = ItemStack.builder(Material.BLACK_STAINED_GLASS_PANE)
            .customName(Component.empty())
            .build();

    private StatsMenu(String playerName, int year) {
        super(InventoryType.CHEST_3_ROW, Component.text(playerName + " - Statistics " + year));
    }

    /**
     * Builds the menu of a player's year.
     *
     * @param playerName the name to show
     * @param view       the year
     * @return the menu, ready to be opened
     */
    public static StatsMenu of(String playerName, YearStatsView view) {
        StatsMenu menu = new StatsMenu(playerName, view.year());
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItemStack(slot, DECORATION_PANE);
        }
        menu.setItemStack(HEAD_SLOT, ItemStack.builder(Material.PLAYER_HEAD)
                .customName(label(playerName + " - " + view.year(), NamedTextColor.GOLD))
                .set(DataComponents.PROFILE, new ResolvableProfile(new GameProfile(view.playerUuid(), playerName)))
                .build());
        menu.setItemStack(GENERAL_SLOT, area(Material.CLOCK, "General", NamedTextColor.GOLD, StatsOverview.general(view)));
        menu.setItemStack(SURVIVOR_SLOT, area(Material.PAPER, "Survivor", NamedTextColor.GREEN, StatsOverview.survivor(view)));
        menu.setItemStack(SLENDER_SLOT, area(Material.ENDER_EYE, "Slender", NamedTextColor.DARK_RED, StatsOverview.slender(view)));
        return menu;
    }

    private static ItemStack area(Material material, String title, NamedTextColor color, List<StatsOverview.Line> lines) {
        List<Component> lore = new ArrayList<>(lines.size() + 2);
        lore.add(Component.empty());
        for (StatsOverview.Line line : lines) {
            lore.add(Component.text(line.label() + ": ", NamedTextColor.GRAY)
                    .append(Component.text(line.value(), NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
        }
        return ItemStack.builder(material)
                .customName(label(title, color))
                .lore(lore)
                .build();
    }

    private static Component label(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
