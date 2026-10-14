package net.onelitefeather.cygnus.setup.inventory.view;

import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryModeTest {

    @Test
    void lobbyHasCorrectSlotsAndCategories() {
        assertArrayEquals(new int[]{11, 13, 15}, InventoryMode.LOBBY.getSlots());
        assertEquals(Set.of(MapDataCategory.NAME, MapDataCategory.AUTHOR, MapDataCategory.SPAWN),
                InventoryMode.LOBBY.getCategories());
    }

    @Test
    void gameHasCorrectSlotsAndCategories() {
        assertArrayEquals(new int[]{10, 11, 13, 15, 16}, InventoryMode.GAME.getSlots());
        assertEquals(Set.of(MapDataCategory.NAME, MapDataCategory.AUTHOR, MapDataCategory.SPAWN,
                        MapDataCategory.SLENDER, MapDataCategory.ATMOSPHERE),
                InventoryMode.GAME.getCategories());
    }
}
