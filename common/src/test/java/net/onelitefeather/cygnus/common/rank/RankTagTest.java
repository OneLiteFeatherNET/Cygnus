package net.onelitefeather.cygnus.common.rank;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTagTest {

    @Test
    void testRankTagProperties() {
        RankTag custom = new RankTag("custom", "󰆐", 42);

        assertEquals("custom", custom.id());
        assertEquals("󰆐", custom.glyph());
        assertEquals(RankTag.DEFAULT_FONT, custom.font());
        assertEquals(42, custom.priority());

        Component comp = custom.asComponent();
        assertNotNull(comp);
        assertEquals("󰆐", ((net.kyori.adventure.text.TextComponent) comp).content());
        assertEquals(RankTag.DEFAULT_FONT, comp.style().font());
    }

    @Test
    void testRankTagComparison() {
        assertTrue(RankTag.ADMINISTRATOR.compareTo(RankTag.MOD) < 0);
        assertTrue(RankTag.MOD.compareTo(RankTag.ADMINISTRATOR) > 0);
        assertEquals(0, RankTag.PLAYER.compareTo(new RankTag("other", "󱆖", 0)));
    }

    @Test
    void testRankTagFormat() {
        Component formatted = RankTag.ADMINISTRATOR.format("Steve");
        assertNotNull(formatted);
        List<Component> children = formatted.children();
        assertEquals(3, children.size());
        assertEquals(RankTag.ADMINISTRATOR.asComponent(), children.get(0));
        assertEquals(RankTag.DEFAULT_MINECRAFT_FONT, children.get(1).style().font());
        assertEquals(RankTag.DEFAULT_MINECRAFT_FONT, children.get(2).style().font());
    }

    @Test
    void testStandardDefaults() {
        RankTagRegistry standard = RankTagRegistry.standard();
        assertNotNull(standard);

        assertTrue(standard.findById("administrator").isPresent());
        assertTrue(standard.findById("assistent").isPresent());
        assertTrue(standard.findById("mod").isPresent());
        assertTrue(standard.findById("content").isPresent());
        assertTrue(standard.findById("media").isPresent());
        assertTrue(standard.findById("lite").isPresent());
        assertTrue(standard.findById("player").isPresent());
    }
}
