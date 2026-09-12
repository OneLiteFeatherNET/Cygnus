package net.onelitefeather.cygnus.common.icon;

import net.kyori.adventure.text.Component;
// import net.onelitefeather.cygnus.common.rank.RankTag;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GameIconTest {

    @Test
    void testGameIconProperties() {
        assertEquals("󰀀", GameIcon.GHOST.glyph());
        assertEquals("󰀅", GameIcon.PENTAGRAM.glyph());
        assertEquals("󰀆", GameIcon.CLOCK.glyph());
        assertEquals("󰀇", GameIcon.PAGE.glyph());
        assertEquals("󰀊", GameIcon.FLASHLIGHT.glyph());
        assertEquals("󰀋", GameIcon.MAP.glyph());
        assertEquals("󰀌", GameIcon.BUILDER.glyph());

        assertSame(GameIcon.GHOST, GameIcon.SPECTATOR);
        assertSame(GameIcon.FLASHLIGHT, GameIcon.SURVIVOR);
        assertSame(GameIcon.PENTAGRAM, GameIcon.SLENDER);
        assertSame(GameIcon.CLOCK, GameIcon.TIME);

        assertEquals(net.kyori.adventure.key.Key.key("cygnus", "icons"), GameIcon.FONT);
        assertEquals(GameIcon.FONT, GameIcon.GHOST.asComponent().style().font());
        assertEquals(GameIcon.FONT, GameIcon.FLASHLIGHT.asComponent().style().font());
        assertEquals(GameIcon.FONT, GameIcon.MAP.asComponent().style().font());
        assertEquals(GameIcon.FONT, GameIcon.BUILDER.asComponent().style().font());
    }

    @Disabled("Pending RankTag implementation")
    @Test
    void testGameIconFormat() {
        // Pending RankTag implementation
    }
}
