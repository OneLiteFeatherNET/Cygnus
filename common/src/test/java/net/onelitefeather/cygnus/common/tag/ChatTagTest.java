package net.onelitefeather.cygnus.common.tag;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTagTest {

    private static final Key TAG_FONT = Key.key("cygnus", "tags");

    @Test
    void testSlenderGlyph() {
        assertGlyph(ChatTag.SLENDER, 0xF0020);
    }

    @Test
    void testMapGlyph() {
        assertGlyph(ChatTag.MAP, 0xF0021);
    }

    @Test
    void testPrefixDoesNotLeakTheTagFontOntoTheMessage() {
        Component message = Component.text("theEvilReaper", NamedTextColor.GREEN);
        Component prefixed = ChatTag.SLENDER.prefix(message);

        assertNull(prefixed.style().font(), "the tag font must not leak onto the space and message, or the client shows missing-glyph boxes");
        assertEquals(3, prefixed.children().size());
        assertEquals(ChatTag.SLENDER.glyph(), prefixed.children().get(0));
        assertEquals(Component.space(), prefixed.children().get(1));
        assertEquals(message, prefixed.children().get(2));

        String plainText = PlainTextComponentSerializer.plainText().serialize(prefixed);
        assertTrue(plainText.endsWith(" theEvilReaper"));
    }

    private void assertGlyph(ChatTag tag, int expectedCodepoint) {
        assertEquals(TAG_FONT, tag.glyph().style().font());
        assertEquals(NamedTextColor.WHITE, tag.glyph().style().color());
        assertEquals(ShadowColor.none(), tag.glyph().style().shadowColor());

        String expectedGlyph = new String(Character.toChars(expectedCodepoint));
        assertEquals(expectedGlyph, PlainTextComponentSerializer.plainText().serialize(tag.glyph()));
    }
}
