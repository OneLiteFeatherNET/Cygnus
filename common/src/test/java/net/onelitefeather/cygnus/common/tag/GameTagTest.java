package net.onelitefeather.cygnus.common.tag;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameTagTest {

    @Test
    void testSlenderTagFontAndGlyph() {
        GameTag tag = GameTag.SLENDER;
        assertEquals("\uDB80\uDC20", tag.glyph());
        assertEquals(Key.key("cygnus", "tags"), tag.asComponent().font());
    }

    @Test
    void testSlenderPrefix() {
        Component prefix = GameTag.SLENDER.asPrefix();
        assertNotNull(prefix);
        assertEquals("\uDB80\uDC20 ", PlainTextComponentSerializer.plainText().serialize(prefix));
    }

    @Test
    void testMapInfoTag() {
        GameTag tag = GameTag.MAP_INFO;
        assertEquals("\uDB80\uDC21", tag.glyph());
        assertEquals(Key.key("cygnus", "tags"), tag.asComponent().font());
    }
}
