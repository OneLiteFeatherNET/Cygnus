package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
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
                Arguments.of(1, "\uF001"),
                Arguments.of(2, "\uF002"),
                Arguments.of(3, "\uF003"),
                Arguments.of(4, "\uF004"),
                Arguments.of(8, "\uF008"),
                Arguments.of(15, "\uF008\uF007"),
                Arguments.of(16, "\uF009"),
                Arguments.of(32, "\uF00A"),
                Arguments.of(64, "\uF00B"),
                Arguments.of(65, "\uF00B\uF001"),
                Arguments.of(128, "\uF00C"),
                Arguments.of(137, "\uF00C\uF008\uF001"),
                Arguments.of(256, "\uF00D")
        );
    }

    @ParameterizedTest(name = "getNegativeSpace({0}) should return \"{1}\"")
    @MethodSource("negativeSpaceCases")
    void testNegativeSpaceComposition(int pixels, String expectedGlyphs) {
        assertEquals(expectedGlyphs, TooltipBox.getNegativeSpace(pixels));
    }

    static Stream<Arguments> positiveSpaceCases() {
        return Stream.of(
                Arguments.of(0, ""),
                Arguments.of(-5, ""),
                Arguments.of(1, "\uF00F"),
                Arguments.of(2, "\uF010"),
                Arguments.of(3, "\uF011"),
                Arguments.of(4, "\uF012"),
                Arguments.of(8, "\uF016"),
                Arguments.of(15, "\uF016\uF015"),
                Arguments.of(16, "\uF017"),
                Arguments.of(32, "\uF018"),
                Arguments.of(64, "\uF019"),
                Arguments.of(128, "\uF01A"),
                Arguments.of(160, "\uF01A\uF018"),
                Arguments.of(256, "\uF01B")
        );
    }

    @ParameterizedTest(name = "getPositiveSpace({0}) should return \"{1}\"")
    @MethodSource("positiveSpaceCases")
    void testPositiveSpaceComposition(int pixels, String expectedGlyphs) {
        assertEquals(expectedGlyphs, TooltipBox.getPositiveSpace(pixels));
    }

    @Test
    @DisplayName("Single line TooltipBox includes ribbon glyph and correct layout")
    void testSingleLineWithRibbon() {
        Component box = TooltipBox.of(Component.text("Help me"));
        String plain = PLAIN.serialize(box);
        assertTrue(plain.contains(TooltipBox.RIBBON), "Must contain ribbon glyph");
        assertTrue(plain.contains("Help me"), "Must contain note text");
        assertTrue(plain.contains(TooltipBox.TOP_LEFT), "Must contain top left cap");
        assertTrue(plain.contains(TooltipBox.TOP_RIGHT), "Must contain top right cap");
        assertTrue(plain.contains(TooltipBox.ROW_LEFT), "Must contain row left cap");
        assertTrue(plain.contains(TooltipBox.ROW_RIGHT), "Must contain row right cap");
        assertTrue(plain.contains(TooltipBox.BOTTOM_LEFT), "Must contain bottom left cap");
        assertTrue(plain.contains(TooltipBox.BOTTOM_RIGHT), "Must contain bottom right cap");
    }

    @Test
    @DisplayName("Multi-line string with newline builds multi-line components")
    void testMultiLineBoxBuild() {
        Component box = TooltipBox.of(Component.text("Always watches,\nno eyes"));
        String plain = PLAIN.serialize(box);
        assertTrue(plain.contains(TooltipBox.RIBBON), "Must contain ribbon glyph");
        assertTrue(plain.contains("Always watches,"), "Must contain line 1");
        assertTrue(plain.contains("no eyes"), "Must contain line 2");
        assertFalse(plain.contains("\n"), "Rendered component should not contain raw newline");
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

        // 1. Leading space
        Component leading = children.get(0);
        assertEquals(EXPECTED_FONT, leading.style().font(), "Leading space must use tooltip font");

        // 2. Header Band
        Component header = children.get(1);
        assertEquals(EXPECTED_FONT, header.style().font(), "Header must use tooltip font");
        assertEquals(NamedTextColor.WHITE, header.style().color(), "Header graphics must be white");

        // Ribbon and note text should be present in the box
        String plain = PLAIN.serialize(box);
        assertTrue(plain.contains(TooltipBox.RIBBON));
        assertTrue(plain.contains("Hello"));
    }

    @Test
    @DisplayName("Crosshair offset shifts leading space correctly")
    void testCrosshairOffset() {
        Component line = Component.text("Test"); // In compact 6px font: T(5)+e(5)+s(5)+t(3) = 18px
        int textWidth = 18;
        int contentWidth = TooltipBox.TEXT_LEFT_INDENT + textWidth; // 10 + 18 = 28
        int middleTiles = Math.max(TooltipBox.MIN_CONTENT_WIDTH, contentWidth + TooltipBox.INNER_RIGHT_PADDING); // 28 + 4 = 32
        int boxWidth = TooltipBox.CAP_WIDTH + middleTiles + TooltipBox.CAP_WIDTH; // 5 + 32 + 5 = 42

        int offset = 20;
        Component box = TooltipBox.builder()
                .line(line)
                .crosshairOffsetX(offset)
                .build();

        int expectedLeadingSpace = boxWidth + (2 * offset); // 42 + 40 = 82
        String expectedLeading = TooltipBox.getPositiveSpace(expectedLeadingSpace);

        List<Component> children = box.children();
        assertEquals(expectedLeading, PLAIN.serialize(children.get(0)));
    }

    @Test
    @DisplayName("Convenience factory methods TooltipBox.of(...) construct identical components")
    void testConvenienceMethods() {
        Component line = Component.text("Always watches, no eyes");

        Component boxFromVarargs = TooltipBox.of(line);
        Component boxFromList = TooltipBox.of(List.of(line));
        Component boxFromBuilder = TooltipBox.builder().line(line).build();

        assertEquals(PLAIN.serialize(boxFromBuilder), PLAIN.serialize(boxFromVarargs));
        assertEquals(PLAIN.serialize(boxFromBuilder), PLAIN.serialize(boxFromList));
    }
}
