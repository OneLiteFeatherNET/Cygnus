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

    static Stream<Arguments> positiveSpaceCases() {
        return Stream.of(
                Arguments.of(0, ""),
                Arguments.of(-5, ""),
                Arguments.of(1, "\uF803"),
                Arguments.of(2, "\uF805"),
                Arguments.of(3, "\uF805\uF803"),
                Arguments.of(4, "\uF806"),
                Arguments.of(8, "\uF807"),
                Arguments.of(16, "\uF809"),
                Arguments.of(32, "\uF811"),
                Arguments.of(64, "\uF821"),
                Arguments.of(128, "\uF841"),
                Arguments.of(160, "\uF841\uF811")
        );
    }

    @ParameterizedTest(name = "getPositiveSpace({0}) should return \"{1}\"")
    @MethodSource("positiveSpaceCases")
    void testPositiveSpaceComposition(int pixels, String expectedGlyphs) {
        assertEquals(expectedGlyphs, TooltipBox.getPositiveSpace(pixels));
    }

    @Test
    @DisplayName("Single line TooltipBox generates valid component with 3-part container glyphs and content")
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

        // Verify 3-part glyphs are present
        assertTrue(serialized.contains(TooltipBox.CAP_LEFT), "Box should contain left cap");
        assertTrue(serialized.contains(TooltipBox.MIDDLE), "Box should contain middle tile");
        assertTrue(serialized.contains(TooltipBox.CAP_RIGHT), "Box should contain right cap");
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

        // 2. Box graphics
        Component boxGraphics = children.get(1);
        assertEquals(EXPECTED_FONT, boxGraphics.style().font(), "Box graphics must use tooltip font");
        assertEquals(NamedTextColor.WHITE, boxGraphics.style().color(), "Box graphics must be white");

        // 3. Shift component
        Component shift = children.get(2);
        assertEquals(EXPECTED_FONT, shift.style().font(), "Shift must use tooltip font");

        // 4. Content component
        Component content = children.get(3);
        assertEquals("Hello", PLAIN.serialize(content), "Content must match input line");
    }

    @Test
    @DisplayName("Crosshair offset shifts leading space correctly")
    void testCrosshairOffset() {
        Component line = Component.text("Test"); // T(6)+e(6)+s(6)+t(4) = 22px
        int textWidth = 22;
        int boxWidth = TooltipBox.CAP_WIDTH + textWidth + TooltipBox.CAP_WIDTH; // 5 + 22 + 5 = 32

        int offset = 20;
        Component box = TooltipBox.builder()
                .line(line)
                .crosshairOffsetX(offset)
                .build();

        int expectedLeadingSpace = boxWidth + (2 * offset); // 32 + 40 = 72
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
