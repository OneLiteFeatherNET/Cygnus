package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekRouteSlotTest {

    @Test
    @DisplayName("The item shows the route's name, point count and pauses")
    void itemShowsNameAndPoints() {
        CreekRoute route = new CreekRoute("Waldweg", List.of(
                CreekWaypoint.of(new Vec(0, 80, 0)).withPause(2000),
                CreekWaypoint.of(new Vec(5, 80, 0)),
                CreekWaypoint.of(new Vec(9, 80, 0)),
                CreekWaypoint.of(new Vec(12, 80, 0)).withPause(4000)));
        ItemStack item = new CreekRouteSlot(route, true, (_, _) -> {}, (_, _) -> {}).getItem();

        assertEquals("Waldweg", PlainTextComponentSerializer.plainText().serialize(item.get(DataComponents.CUSTOM_NAME)));
        List<String> lore = item.get(DataComponents.LORE).stream()
                .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                .toList();
        assertTrue(lore.contains("Points: 4"));
        assertTrue(lore.contains("Pause: start 2.0 s · end 4.0 s"));
    }
}
