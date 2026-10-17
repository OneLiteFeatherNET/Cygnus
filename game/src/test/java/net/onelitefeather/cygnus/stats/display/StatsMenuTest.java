package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.stats.YearStatsView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what the menu shows. That it cannot be changed is covered with the feature, which owns the
 * click listener.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
class StatsMenuTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final YearStatsView VIEW = new YearStatsView(PLAYER, 2026, 12, 4, 8, 2, 4, 1, 3, 1, 3_900L, 12, 5, 3, 4,
            1_200L, 600L, 2, 1, 55.0D, 3, 9, 4, 480L, 14, 1);

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String name(ItemStack stack) {
        return plain(stack.get(DataComponents.CUSTOM_NAME));
    }

    private static List<String> lore(ItemStack stack) {
        return stack.get(DataComponents.LORE, List.<Component>of()).stream().map(StatsMenuTest::plain).toList();
    }

    @Test
    @DisplayName("The title names the player and the year")
    void titleNamesPlayerAndYear() {
        StatsMenu menu = StatsMenu.of("Alice", VIEW);

        String title = plain(menu.getTitle());
        assertTrue(title.contains("Alice") && title.contains("2026"), "title was: " + title);
        assertEquals(InventoryType.CHEST_3_ROW, menu.getInventoryType());
    }

    @Test
    @DisplayName("There is one item per area, each listing its counters")
    void oneItemPerArea() {
        StatsMenu menu = StatsMenu.of("Alice", VIEW);

        ItemStack general = menu.getItemStack(StatsMenu.GENERAL_SLOT);
        ItemStack survivor = menu.getItemStack(StatsMenu.SURVIVOR_SLOT);
        ItemStack slender = menu.getItemStack(StatsMenu.SLENDER_SLOT);

        assertEquals("General", name(general));
        assertEquals("Survivor", name(survivor));
        assertEquals("Slender", name(slender));
        assertTrue(lore(general).contains("Rounds played: 12"), "general lore: " + lore(general));
        assertTrue(lore(survivor).contains("Pages found: 12 (1.5 per round)"), "survivor lore: " + lore(survivor));
        assertTrue(lore(slender).contains("Kills: 9 (K/D 2.25)"), "slender lore: " + lore(slender));
    }

    @Test
    @DisplayName("The player's head sits in the top row")
    void headInTheTopRow() {
        StatsMenu menu = StatsMenu.of("Alice", VIEW);

        ItemStack head = menu.getItemStack(StatsMenu.HEAD_SLOT);

        assertEquals(Material.PLAYER_HEAD, head.material());
        assertTrue(name(head).contains("Alice"), "the head carries the name");
    }

    @Test
    @DisplayName("Every other slot is decoration")
    void everySlotIsFilled() {
        StatsMenu menu = StatsMenu.of("Alice", VIEW);
        List<Integer> content = List.of(StatsMenu.HEAD_SLOT, StatsMenu.GENERAL_SLOT, StatsMenu.SURVIVOR_SLOT, StatsMenu.SLENDER_SLOT);

        for (int slot = 0; slot < menu.getSize(); slot++) {
            if (!content.contains(slot)) {
                assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getItemStack(slot).material(), "slot " + slot);
            }
        }
    }

    @Test
    @DisplayName("An empty year builds a menu of zeros")
    void emptyYearBuildsAMenu() {
        StatsMenu menu = StatsMenu.of("Alice", YearStatsView.empty(PLAYER, 2026));

        assertTrue(lore(menu.getItemStack(StatsMenu.GENERAL_SLOT)).contains("Rounds played: 0"));
    }
}
