package net.onelitefeather.cygnus.setup.inventory.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageHeaderFormatterTest {

    @Test
    void testFormatter() {
        Component component = PageHeaderFormatter.format(1, 2);
        assertNotNull(component);

        String componentText = PlainTextComponentSerializer.plainText().serialize(component);

        assertNotNull(componentText);
        assertEquals("Page 1 of 2", componentText);
    }
}
