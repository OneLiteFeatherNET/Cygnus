package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringItemSlotTest {

    @Test
    void testStringItemSlot() {
        String name = "TestMap";
        Component mapDisplayComponent = Component.text("Mapname:", NamedTextColor.GRAY).append(Component.space());
        StringItemSlot stringItemSlot = new StringItemSlot(mapDisplayComponent, name);
        assertNotNull(stringItemSlot);

        ItemStack slotItem = stringItemSlot.getItem();
        assertNotNull(slotItem);
        assertTrue(slotItem.has(ItemComponent.LORE));

        List<Component> loreList = slotItem.get(ItemComponent.LORE);
        assertNotNull(loreList);
        Component mapNameComponent = loreList.stream().filter(
                component -> !PlainTextComponentSerializer.plainText().serialize(component).isEmpty()
        ).findFirst().orElse(null);
        assertNotNull(mapNameComponent);
        assertInstanceOf(Component.class, mapNameComponent);
        String rawName = PlainTextComponentSerializer.plainText().serialize(mapNameComponent).replace("- ", "");
        System.out.println("Raw name is " + rawName);
        assertFalse(rawName.isEmpty());
        assertEquals(name, rawName);
    }
}
