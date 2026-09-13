package net.onelitefeather.cygnus.team;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleIconTest {

    private static final Key ICON_FONT = Key.key("cygnus", "icons");

    @Test
    void testSlenderGlyph() {
        assertGlyph(RoleIcon.SLENDER, GameConfig.SLENDER_KEY, 0xF0005);
    }

    @Test
    void testSurvivorGlyph() {
        assertGlyph(RoleIcon.SURVIVOR, GameConfig.SURVIVOR_KEY, 0xF000A);
    }

    @Test
    void testSpectatorGlyph() {
        assertGlyph(RoleIcon.SPECTATOR, GameConfig.SPECTATOR_KEY, 0xF0000);
    }

    @Test
    void testPrefixKeepsIconAndAppendsGivenName() {
        Component name = Component.text("theEvilReaper", NamedTextColor.GREEN);
        Component prefixed = RoleIcon.SURVIVOR.prefix(name);

        assertEquals(ICON_FONT, prefixed.style().font());
        assertEquals(2, prefixed.children().size());
        assertEquals(Component.space(), prefixed.children().get(0));
        assertEquals(name, prefixed.children().get(1));

        String plainText = PlainTextComponentSerializer.plainText().serialize(prefixed);
        assertTrue(plainText.endsWith(" theEvilReaper"));
    }

    private void assertGlyph(RoleIcon icon, Key expectedRoleKey, int expectedCodepoint) {
        assertEquals(expectedRoleKey, icon.roleKey());
        assertEquals(ICON_FONT, icon.glyph().style().font());
        assertEquals(NamedTextColor.WHITE, icon.glyph().style().color());

        String expectedGlyph = new String(Character.toChars(expectedCodepoint));
        assertEquals(expectedGlyph, PlainTextComponentSerializer.plainText().serialize(icon.glyph()));
    }
}
