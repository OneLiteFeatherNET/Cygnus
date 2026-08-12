package net.onelitefeather.cygnus.common.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudSegmentTest {

    private static final TextColor TINT = TextColor.color(254, 254, 250);

    @Test
    void segmentWidthMatchesIconWidthPlusPaddingPlusTextWidth() {
        Component text = Component.text("12 / 34", NamedTextColor.GREEN);
        int iconWidthPx = 9;
        int paddingPx = 2;

        Component segment = HudSegment.segment(text, iconWidthPx, paddingPx, TINT);

        int expected = iconWidthPx + 2 * paddingPx + TextWidth.widthOf(text);
        assertEquals(expected, TextWidth.widthOf(segment));
    }

    @Test
    void segmentWidthHoldsForDifferentIconAndPaddingValues() {
        Component text = Component.text("00:00");
        int iconWidthPx = 16;
        int paddingPx = 4;

        Component segment = HudSegment.segment(text, iconWidthPx, paddingPx, TINT);

        int expected = iconWidthPx + 2 * paddingPx + TextWidth.widthOf(text);
        assertEquals(expected, TextWidth.widthOf(segment));
    }
}
