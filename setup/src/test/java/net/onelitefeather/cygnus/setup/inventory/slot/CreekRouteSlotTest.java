package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekRouteSlotTest {

    @Test
    @DisplayName("The item shows the route's name and point count")
    void itemShowsNameAndPoints() {
        ItemStack item = new CreekRouteSlot("Waldweg", 4, true, (_, _) -> {}, (_, _) -> {}).getItem();

        assertEquals("Waldweg", PlainTextComponentSerializer.plainText().serialize(item.get(DataComponents.CUSTOM_NAME)));
        assertTrue(item.get(DataComponents.LORE).stream()
                .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                .anyMatch(line -> line.equals("Points: 4")));
    }
}
