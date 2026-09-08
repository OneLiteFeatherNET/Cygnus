package net.onelitefeather.cygnus.common.rank;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

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
}
