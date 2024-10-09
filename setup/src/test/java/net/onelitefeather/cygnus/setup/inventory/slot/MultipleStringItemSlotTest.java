package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipleStringItemSlotTest {
    private static final Component BUILDERS = Component.text("Builders", NamedTextColor.GRAY).append(Component.space());

    @Test
    void testSlotCreationViaStringList() {
        List<String> authors = List.of("Bob", "Cat", "Mep");
        MultipleStringItemSlot stringItemSlot = new MultipleStringItemSlot(BUILDERS, authors);
        assertNotNull(stringItemSlot);

        ItemStack slotItem = stringItemSlot.getItem();
        assertNotNull(slotItem);
        assertTrue(slotItem.has(ItemComponent.LORE));

        List<Component> loreList = slotItem.get(ItemComponent.LORE);
        assertNotNull(loreList);
        List<Component> mapNameComponent = loreList.stream().filter(
                component -> !PlainTextComponentSerializer.plainText().serialize(component).isEmpty()
        ).toList();
        assertFalse(mapNameComponent.isEmpty());
        assertMultipleStringSlotContent(authors, mapNameComponent);
    }

    @Test
    void testSlotCreationViaArrayStrings() {
        String[] authors = {"Bob", "Cat", "Schlepp"};
        MultipleStringItemSlot stringItemSlot = new MultipleStringItemSlot(BUILDERS, authors);
        assertNotNull(stringItemSlot);

        ItemStack slotItem = stringItemSlot.getItem();
        assertNotNull(slotItem);
        assertTrue(slotItem.has(ItemComponent.LORE));

        List<Component> loreList = slotItem.get(ItemComponent.LORE);
        assertNotNull(loreList);
        List<Component> mapNameComponent = loreList.stream().filter(
                component -> !PlainTextComponentSerializer.plainText().serialize(component).isEmpty()
        ).toList();
        assertFalse(mapNameComponent.isEmpty());
        assertMultipleStringSlotContent(List.of(authors), mapNameComponent);
    }

    /**
     * Asserts the content of the lore list.
     *
     * @param expectedAuthors the expected authors.
     * @param slotLore        the lore list.
     */
    private void assertMultipleStringSlotContent(@NotNull List<String> expectedAuthors, @NotNull List<Component> slotLore) {
        List<Component> filteredLore = slotLore.stream().filter(
                component -> !PlainTextComponentSerializer.plainText().serialize(component).isEmpty()
        ).toList();
        assertFalse(filteredLore.isEmpty());

        for (int i = 0; i < filteredLore.size(); i++) {
            Component component = filteredLore.get(i);
            assertInstanceOf(Component.class, component);
            String builder = PlainTextComponentSerializer.plainText().serialize(component).replace("- ", "");
            assertFalse(builder.isEmpty());
            assertEquals(expectedAuthors.get(i), builder);
        }
    }
}
