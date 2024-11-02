package net.onelitefeather.spectator.item;

import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SpectatorHotBarItemTest {

    private static final Consumer<Player> NO_OP = player -> {
    };

    @Test
    void testInvalidItemStackUsage() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorHotBarItem(ItemStack.AIR, 0, 0, NO_OP),
                "The ItemStack can not be referenced to ItemStack.AIR"
        );
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorHotBarItem(ItemStack.builder(Material.AIR).build(), 0, 0, NO_OP),
                "Item cannot be air"
        );
    }

    @Test
    void testInvalidItemIndex() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorHotBarItem(ItemStack.builder(Material.DIAMOND).build(), -1, 0, NO_OP),
                "Item index cannot be negative"
        );
    }

    @ParameterizedTest(name = "Test invalid slot index usage with index: {0}")
    @ValueSource(ints = {-1, 9})
    void testInvalidSlotIndex(int slotIndex) {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SpectatorHotBarItem(ItemStack.builder(Material.DIAMOND).build(), 0, slotIndex, NO_OP),
                "Item index must be between 0 and 8"
        );
    }

    @ParameterizedTest(name = "Test valid slot index usage with index: {0}")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void testValidSlotIndex(int slotIndex) {
        assertDoesNotThrow(
                () -> new SpectatorHotBarItem(ItemStack.builder(Material.DIAMOND), 0, slotIndex, NO_OP)
        );
    }
}