package net.onelitefeather.cygnus.setup.inventory.view;

import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class InventoryModeTest {

    @Test
    void lobbyHasCorrectSlots() {
        assertArrayEquals(new int[]{11, 13, 15}, InventoryMode.LOBBY.getSlots());
    }

    @Test
    void lobbyHasCorrectCategories() {
        Set<MapDataCategory> categories = InventoryMode.LOBBY.getCategories();
        assertEquals(3, categories.size());
        assertTrue(categories.contains(MapDataCategory.NAME));
        assertTrue(categories.contains(MapDataCategory.AUTHOR));
        assertTrue(categories.contains(MapDataCategory.SPAWN));
        assertFalse(categories.contains(MapDataCategory.SLENDER));
    }

    @Test
    void gameHasCorrectSlots() {
        assertArrayEquals(new int[]{10, 12, 14, 16}, InventoryMode.GAME.getSlots());
    }

    @Test
    void gameHasCorrectCategories() {
        Set<MapDataCategory> categories = InventoryMode.GAME.getCategories();
        assertEquals(4, categories.size());
        assertTrue(categories.contains(MapDataCategory.NAME));
        assertTrue(categories.contains(MapDataCategory.AUTHOR));
        assertTrue(categories.contains(MapDataCategory.SPAWN));
        assertTrue(categories.contains(MapDataCategory.SLENDER));
    }

    @Test
    void slotCountMatchesCategoryCount() {
        for (InventoryMode mode : InventoryMode.values()) {
            assertEquals(mode.getSlots().length, mode.getCategories().size(),
                    "Slot count and category count must match for mode: " + mode.name());
        }
    }
}