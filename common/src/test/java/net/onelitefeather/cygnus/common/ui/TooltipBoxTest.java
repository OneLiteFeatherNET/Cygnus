package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TooltipBoxTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Key EXPECTED_FONT = Key.key("cygnus", "tooltip");

    static Stream<Arguments> negativeSpaceCases() {
        return Stream.of(
                Arguments.of(0, ""),
                Arguments.of(-5, ""),
                Arguments.of(1, "\uF801"),
                Arguments.of(2, "\uF802"),
                Arguments.of(3, "\uF802\uF801"),
                Arguments.of(4, "\uF804"),
                Arguments.of(8, "\uF808"),
                Arguments.of(15, "\uF808\uF804\uF802\uF801"),
                Arguments.of(16, "\uF810"),
                Arguments.of(32, "\uF820"),
                Arguments.of(64, "\uF840"),
                Arguments.of(65, "\uF840\uF801"),
                Arguments.of(128, "\uF880"),
                Arguments.of(137, "\uF880\uF808\uF801"),
                Arguments.of(256, "\uF880\uF880")
        );
    }

    @ParameterizedTest(name = "getNegativeSpace({0}) should return \"{1}\"")
    @MethodSource("negativeSpaceCases")
    void testNegativeSpaceComposition(int pixels, String expectedGlyphs) {
        assertEquals(expectedGlyphs, TooltipBox.getNegativeSpace(pixels));
    }

    @Test
    @DisplayName("Single line TooltipBox generates valid non-empty component with all 9-slice glyphs")
    void testSingleLineComponentBuild() {
        Component line = Component.text("Test");
        Component box = TooltipBox.builder()
                .line(line)
                .build();

        assertNotNull(box);
        String serialized = PLAIN.serialize(box);
        assertFalse(serialized.isEmpty());

        // Verify text content is embedded
        assertTrue(serialized.contains("Test"), "Box should contain the line content");

        // Verify 9-slice glyphs are present
        assertTrue(serialized.contains(TooltipBox.CORNER_TL), "Box should contain top-left corner");
        assertTrue(serialized.contains(TooltipBox.BORDER_TOP), "Box should contain top border");
        assertTrue(serialized.contains(TooltipBox.CORNER_TR), "Box should contain top-right corner");
        assertTrue(serialized.contains(TooltipBox.BORDER_LEFT), "Box should contain left border");
        assertTrue(serialized.contains(TooltipBox.BG_FILL), "Box should contain background fill");
        assertTrue(serialized.contains(TooltipBox.BORDER_RIGHT), "Box should contain right border");
        assertTrue(serialized.contains(TooltipBox.CORNER_BL), "Box should contain bottom-left corner");
        assertTrue(serialized.contains(TooltipBox.BORDER_BOTTOM), "Box should contain bottom border");
        assertTrue(serialized.contains(TooltipBox.CORNER_BR), "Box should contain bottom-right corner");
    }

    @Test
    @DisplayName("Component structure applies cygnus:tooltip font and white color to frame elements")
    void testComponentStructureAndFontKey() {
        Component line = Component.text("Hello");
        Component box = TooltipBox.builder()
                .line(line)
                .build();

        List<Component> children = box.children();
        assertFalse(children.isEmpty(), "Box should have child components");

        // Top border
        Component topBorder = children.get(0);
        assertEquals(EXPECTED_FONT, topBorder.style().font(), "Top border must use tooltip font");
        assertEquals(NamedTextColor.WHITE, topBorder.style().color(), "Top border must be white");
        assertTrue(PLAIN.serialize(topBorder).endsWith("\n"), "Top border row must end with newline");

        // Background row
        Component bg = children.get(1);
        assertEquals(EXPECTED_FONT, bg.style().font(), "Background must use tooltip font");
        assertEquals(NamedTextColor.WHITE, bg.style().color(), "Background must be white");

        // Shift component
        Component shift = children.get(2);
        assertEquals(EXPECTED_FONT, shift.style().font(), "Shift must use tooltip font");

        // Content component
        Component content = children.get(3);
        assertEquals("Hello", PLAIN.serialize(content), "Content must match input line");

        // Bottom border (last child)
        Component bottomBorder = children.get(children.size() - 1);
        assertEquals(EXPECTED_FONT, bottomBorder.style().font(), "Bottom border must use tooltip font");
        assertEquals(NamedTextColor.WHITE, bottomBorder.style().color(), "Bottom border must be white");
    }

    @Test
    @DisplayName("Inner width and total shift adapt to custom padding")
    void testCustomPadding() {
        // "Test" width: T(6) + e(6) + s(6) + t(4) = 22px
        Component line = Component.text("Test");
        int padding = 6;
        Component box = TooltipBox.builder()
                .line(line)
                .padding(padding)
                .build();

        int maxContentWidth = 22;
        int expectedInnerWidth = maxContentWidth + (padding * 2); // 22 + 12 = 34
        int expectedShift = expectedInnerWidth + 3 - padding; // 34 + 3 - 6 = 31

        String expectedTopBorder = TooltipBox.CORNER_TL + TooltipBox.BORDER_TOP.repeat(expectedInnerWidth) + TooltipBox.CORNER_TR + "\n";
        String expectedShiftGlyphs = TooltipBox.getNegativeSpace(expectedShift);

        List<Component> children = box.children();
        assertEquals(expectedTopBorder, PLAIN.serialize(children.get(0)));
        assertEquals(expectedShiftGlyphs, PLAIN.serialize(children.get(2)));
    }

    @Test
    @DisplayName("Multi-line tooltip sizes inner width according to the widest line")
    void testMultiLineTooltip() {
        Component line1 = Component.text("Short"); // S(6)+h(6)+o(6)+r(6)+t(4) = 28px
        Component line2 = Component.text("A much longer second line"); // longer than 28px
        int expectedMaxWidth = FontWidthHelper.getWidth(line2);
        assertTrue(expectedMaxWidth > 28);

        Component box = TooltipBox.builder()
                .line(line1)
                .line(line2)
                .build();

        int defaultPadding = 4;
        int expectedInnerWidth = expectedMaxWidth + (defaultPadding * 2);
        String expectedTop = TooltipBox.CORNER_TL + TooltipBox.BORDER_TOP.repeat(expectedInnerWidth) + TooltipBox.CORNER_TR + "\n";

        assertEquals(expectedTop, PLAIN.serialize(box.children().get(0)));
    }

    @Test
    @DisplayName("Convenience factory methods TooltipBox.of(...) construct identical components")
    void testConvenienceMethods() {
        Component line1 = Component.text("Line 1");
        Component line2 = Component.text("Line 2");

        Component boxFromVarargs = TooltipBox.of(line1, line2);
        Component boxFromList = TooltipBox.of(List.of(line1, line2));
        Component boxFromBuilder = TooltipBox.builder().line(line1).line(line2).build();

        assertEquals(PLAIN.serialize(boxFromBuilder), PLAIN.serialize(boxFromVarargs));
        assertEquals(PLAIN.serialize(boxFromBuilder), PLAIN.serialize(boxFromList));
    }

    @Test
    @DisplayName("Empty lines builder falls back to minimum content width of 10px")
    void testEmptyLinesFallback() {
        Component box = TooltipBox.builder().build();
        assertNotNull(box);

        int minWidth = 10;
        int expectedInnerWidth = minWidth + (4 * 2); // 18
        String expectedTop = TooltipBox.CORNER_TL + TooltipBox.BORDER_TOP.repeat(expectedInnerWidth) + TooltipBox.CORNER_TR + "\n";
        String expectedBottom = TooltipBox.CORNER_BL + TooltipBox.BORDER_BOTTOM.repeat(expectedInnerWidth) + TooltipBox.CORNER_BR;

        List<Component> children = box.children();
        assertEquals(2, children.size(), "Empty box should have top and bottom border rows");
        assertEquals(expectedTop, PLAIN.serialize(children.get(0)));
        assertEquals(expectedBottom, PLAIN.serialize(children.get(1)));
    }
}
