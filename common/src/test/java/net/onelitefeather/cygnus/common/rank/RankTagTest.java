package net.onelitefeather.cygnus.common.rank;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTagTest {

    private static final Key RANK_FONT = Key.key("olf", "rank_tags");

    @Test
    void testAdministratorGlyph() {
        assertGlyph(RankTag.ADMINISTRATOR, 0xF0190);
    }

    @Test
    void testAssistentGlyph() {
        assertGlyph(RankTag.ASSISTENT, 0xF219B);
    }

    @Test
    void testModGlyph() {
        assertGlyph(RankTag.MOD, 0xF119B);
    }

    @Test
    void testContentGlyph() {
        assertGlyph(RankTag.CONTENT, 0xF2190);
    }

    @Test
    void testMediaGlyph() {
        assertGlyph(RankTag.MEDIA, 0xF2195);
    }

    @Test
    void testLiteGlyph() {
        assertGlyph(RankTag.LITE, 0xF2191);
    }

    @Test
    void testPlayerGlyph() {
        assertGlyph(RankTag.PLAYER, 0xF1196);
    }

    @Test
    void testPrefixDoesNotLeakTheRankFontOntoTheName() {
        Component name = Component.text("theEvilReaper", NamedTextColor.GREEN);
        Component prefixed = RankTag.ADMINISTRATOR.prefix(name);

        assertNull(prefixed.style().font(), "the rank font must not leak onto the space and name, or the client shows missing-glyph boxes");
        assertEquals(3, prefixed.children().size());
        assertEquals(RankTag.ADMINISTRATOR.glyph(), prefixed.children().get(0));
        assertEquals(Component.space(), prefixed.children().get(1));
        assertEquals(name, prefixed.children().get(2));

        String plainText = PlainTextComponentSerializer.plainText().serialize(prefixed);
        assertTrue(plainText.endsWith(" theEvilReaper"));
    }

    @Test
    void testFromGroupMatchesCaseInsensitively() {
        assertEquals(Optional.of(RankTag.ADMINISTRATOR), RankTag.fromGroup("administrator"));
        assertEquals(Optional.of(RankTag.ADMINISTRATOR), RankTag.fromGroup("Administrator"));
        assertEquals(Optional.of(RankTag.PLAYER), RankTag.fromGroup("PLAYER"));
    }

    @Test
    void testFromGroupIsEmptyForUnknownOrNullGroup() {
        assertEquals(Optional.empty(), RankTag.fromGroup("default"));
        assertEquals(Optional.empty(), RankTag.fromGroup(null));
    }

    private void assertGlyph(RankTag tag, int expectedCodepoint) {
        assertEquals(RANK_FONT, tag.glyph().style().font());
        assertEquals(NamedTextColor.WHITE, tag.glyph().style().color());

        String expectedGlyph = new String(Character.toChars(expectedCodepoint));
        assertEquals(expectedGlyph, PlainTextComponentSerializer.plainText().serialize(tag.glyph()));
    }
}
