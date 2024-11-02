package net.onelitefeather.spectator;

import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.spectator.item.SpectatorItemContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SpectatorItemContainerTest {

    @Test
    void testInvalidItemStackUsage() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorItemContainer(ItemStack.AIR, 0, 0),
                "The ItemStack can not be referenced to ItemStack.AIR"
        );
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorItemContainer(ItemStack.builder(Material.AIR).build(), 0, 0),
                "Item cannot be air"
        );
    }

    @Test
    void testInvalidItemIndex() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorItemContainer(ItemStack.builder(Material.DIAMOND).build(), -1, 0),
                "Item index cannot be negative"
        );
    }

    @ParameterizedTest(name = "Test invalid slot index usage with index: {0}")
    @ValueSource(ints = {-1, 9})
    void testInvalidSlotIndex(int slotIndex) {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorItemContainer(ItemStack.builder(Material.DIAMOND).build(), 0, slotIndex),
                "Item index must be between 0 and 8"
        );
    }

    @ParameterizedTest(name = "Test valid slot index usage with index: {0}")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void testValidSlotIndex(int slotIndex) {
        assertDoesNotThrow(
                () -> new SpectatorItemContainer(ItemStack.builder(Material.DIAMOND).build(), 0, slotIndex)
        );
    }
}