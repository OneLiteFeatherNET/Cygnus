package net.onelitefeather.cygnus.common.text;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudSegmentTest {

    private static final Key FONT_KEY = Key.key("cygnus", "misc");
    private static final TextColor TINT = TextColor.color(254, 254, 250);
    private static final char ICON_CLOCK = '\ue101';
    private static final char ICON_PAGE = '\ue102';

    private static int iconWidth(char icon) {
        return TextWidth.widthOf(Component.text(String.valueOf(icon)).font(FONT_KEY));
    }

    @Test
    void segmentWidthMatchesIconWidthPlusPaddingPlusTextWidth() {
        Component text = Component.text("12 / 34", NamedTextColor.GREEN);
        int paddingPx = 2;

        Component segment = HudSegment.segment(ICON_PAGE, text, paddingPx, TINT);

        int expected = iconWidth(ICON_PAGE) + 2 * paddingPx + TextWidth.widthOf(text);
        assertEquals(expected, TextWidth.widthOf(segment));
    }

    @Test
    void segmentWidthHoldsForDifferentIconAndPaddingValues() {
        Component text = Component.text("00:00");
        int paddingPx = 4;

        Component segment = HudSegment.segment(ICON_CLOCK, text, paddingPx, TINT);

        int expected = iconWidth(ICON_CLOCK) + 2 * paddingPx + TextWidth.widthOf(text);
        assertEquals(expected, TextWidth.widthOf(segment));
    }
}
