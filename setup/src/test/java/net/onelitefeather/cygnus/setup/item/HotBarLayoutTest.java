package net.onelitefeather.cygnus.setup.item;

import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HotBarLayoutTest {

    @Test
    void testDefaultSlotsAreAir() {
        var layout = new HotBarLayout();
        for (int i = 0; i < 9; i++) {
            assertEquals(ItemStack.AIR, layout.get(i), "Slot " + i + " should default to AIR");
        }
    }

    @Test
    void testSetAndGet() {
        var item = ItemStack.of(Material.DIAMOND);
        var layout = new HotBarLayout().set(4, item);
        assertEquals(item, layout.get(4));
    }

    @Test
    void testSetIsChainable() {
        var first = ItemStack.of(Material.COMPASS);
        var second = ItemStack.of(Material.CLOCK);
        var layout = new HotBarLayout()
                .set(7, first)
                .set(8, second);

        assertEquals(first, layout.get(7));
        assertEquals(second, layout.get(8));
    }

    @Test
    void testSetOverwritesPreviousItem() {
        var original = ItemStack.of(Material.STONE);
        var replacement = ItemStack.of(Material.DIAMOND);

        var layout = new HotBarLayout()
                .set(0, original)
                .set(0, replacement);

        assertEquals(replacement, layout.get(0));
    }

    @Test
    void testUntouchedSlotsRemainAir() {
        var layout = new HotBarLayout()
                .set(3, ItemStack.of(Material.COMPASS))
                .set(8, ItemStack.of(Material.CLOCK));

        for (int i = 0; i < 9; i++) {
            if (i != 3 && i != 8) {
                assertEquals(ItemStack.AIR, layout.get(i), "Slot " + i + " should still be AIR");
            }
        }
    }

    @ParameterizedTest(name = "Invalid slot index: {0}")
    @ValueSource(ints = {-1, 9, 10, -100, Integer.MAX_VALUE})
    void testSetWithInvalidSlotThrows(int slot) {
        var layout = new HotBarLayout();
        assertThrows(IndexOutOfBoundsException.class, () -> layout.set(slot, ItemStack.AIR));
    }

    @ParameterizedTest(name = "Invalid get index: {0}")
    @ValueSource(ints = {-1, 9, 10, -100, Integer.MAX_VALUE})
    void testGetWithInvalidSlotThrows(int slot) {
        var layout = new HotBarLayout();
        assertThrows(IndexOutOfBoundsException.class, () -> layout.get(slot));
    }
}
