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

    private static final String OLD_SLICES = "\uE110\uE111\uE112\uE113\uE114\uE115\uE116\uE117\uE118";

    private static void assertNoOldSlices(String plain) {
        for (char c : OLD_SLICES.toCharArray()) {
            assertFalse(plain.indexOf(c) >= 0, "the pack no longer has slice glyph U+" + Integer.toHexString(c));
        }
    }

    @Test
    @DisplayName("One line uses the one-line box and its ribbon")
    void testSingleLineUsesOneLineBox() {
        String plain = PLAIN.serialize(TooltipBox.of(Component.text("Help me")));
        assertTrue(plain.contains(TooltipBox.BOX_1_LEFT), "Must contain one-line left cap");
        assertTrue(plain.contains(TooltipBox.BOX_1_MIDDLE), "Must contain one-line middle");
        assertTrue(plain.contains(TooltipBox.BOX_1_RIGHT), "Must contain one-line right cap");
        assertTrue(plain.contains(TooltipBox.RIBBON_1), "Must contain one-line ribbon");
        assertFalse(plain.contains(TooltipBox.BOX_2_LEFT));
        assertFalse(plain.contains(TooltipBox.BOX_3_LEFT));
        assertTrue(plain.contains("Help me"), "Must contain note text");
        assertNoOldSlices(plain);
    }

    @Test
    @DisplayName("Two lines use the two-line box and its ribbon")
    void testTwoLinesUseTwoLineBox() {
        String plain = PLAIN.serialize(TooltipBox.of(Component.text("Always watches,\nno eyes")));
        assertTrue(plain.contains(TooltipBox.BOX_2_LEFT));
        assertTrue(plain.contains(TooltipBox.BOX_2_MIDDLE));
        assertTrue(plain.contains(TooltipBox.BOX_2_RIGHT));
        assertTrue(plain.contains(TooltipBox.RIBBON_2));
        assertFalse(plain.contains(TooltipBox.BOX_1_LEFT));
        assertFalse(plain.contains(TooltipBox.RIBBON_1));
        assertTrue(plain.contains("Always watches,"), "Must contain line 1");
        assertTrue(plain.contains("no eyes"), "Must contain line 2");
        assertFalse(plain.contains("\n"), "Rendered component should not contain raw newline");
        assertNoOldSlices(plain);
    }

    @Test
    @DisplayName("Three lines use the three-line box and its ribbon")
    void testThreeLinesUseThreeLineBox() {
        String plain = PLAIN.serialize(TooltipBox.of(Component.text("one\ntwo\nthree")));
        assertTrue(plain.contains(TooltipBox.BOX_3_LEFT));
        assertTrue(plain.contains(TooltipBox.BOX_3_MIDDLE));
        assertTrue(plain.contains(TooltipBox.BOX_3_RIGHT));
        assertTrue(plain.contains(TooltipBox.RIBBON_3));
        assertFalse(plain.contains(TooltipBox.BOX_2_LEFT));
        assertNoOldSlices(plain);
    }

    @Test
    @DisplayName("More than three lines are cut to three")
    void testMoreThanThreeLinesAreCut() {
        String plain = PLAIN.serialize(TooltipBox.of(Component.text("one\ntwo\nthree\nfour")));
        assertTrue(plain.contains(TooltipBox.BOX_3_LEFT), "a cut note still gets the three-line box");
        assertTrue(plain.contains("three"));
        assertFalse(plain.contains("four"), "the fourth line has no box to sit in");
    }

    @Test
    @DisplayName("Each line uses the font that places it on its row")
    void testLinesUseTheirRowFonts() {
        Component box = TooltipBox.builder().centered().line(Component.text("one\ntwo\nthree")).build();
        assertEquals(TooltipBox.FONT_LINE1, fontOf(box, "one"));
        assertEquals(TooltipBox.FONT_LINE2, fontOf(box, "two"));
        assertEquals(TooltipBox.FONT_LINE3, fontOf(box, "three"));
    }

    private static Key fontOf(Component box, String text) {
        for (Component child : box.children()) {
            if (PLAIN.serialize(child).equals(text)) {
                return child.style().font();
            }
        }
        throw new AssertionError("no child with text " + text);
    }

    @Test
    @DisplayName("Box and ribbon use cygnus:tooltip in white")
    void testComponentStructureAndFontKey() {
        Component box = TooltipBox.builder().line(Component.text("Hello")).build();

        List<Component> children = box.children();
        assertFalse(children.isEmpty(), "Box should have child components");

        // 1. Leading space
        Component leading = children.get(0);
        assertEquals(EXPECTED_FONT, leading.style().font(), "Leading space must use tooltip font");

        // 2. The whole box in one run of glyphs
        Component frame = children.get(1);
        assertEquals(EXPECTED_FONT, frame.style().font(), "Box must use tooltip font");
        assertEquals(NamedTextColor.WHITE, frame.style().color(), "Box graphics must be white");
        assertTrue(PLAIN.serialize(frame).startsWith(TooltipBox.BOX_1_LEFT));

        String plain = PLAIN.serialize(box);
        assertTrue(plain.contains(TooltipBox.RIBBON_1));
        assertTrue(plain.contains("Hello"));
    }

    @Test
    @DisplayName("Crosshair offset shifts leading space correctly")
    void testCrosshairOffset() {
        Component line = Component.text("Test"); // In compact 5px font: T(4)+e(4)+s(4)+t(3) = 15px
        int textWidth = 15;
        int contentWidth = TooltipBox.TEXT_LEFT_INDENT + textWidth; // 10 + 15 = 25
        int middleTiles = Math.max(TooltipBox.MIN_CONTENT_WIDTH, contentWidth + TooltipBox.INNER_RIGHT_PADDING); // 25 + 2 = 27
        int boxWidth = TooltipBox.CAP_WIDTH + middleTiles + TooltipBox.CAP_WIDTH; // 5 + 27 + 5 = 37

        int offset = 20;
        Component box = TooltipBox.builder()
                .line(line)
                .crosshairOffsetX(offset)
                .build();

        int expectedLeadingSpace = boxWidth + (2 * offset); // 37 + 40 = 77
        String expectedLeading = TooltipBox.getPositiveSpace(expectedLeadingSpace);

        List<Component> children = box.children();
        assertEquals(expectedLeading, PLAIN.serialize(children.get(0)));
    }

    @Test
    @DisplayName("The box is exactly its computed width: every glyph is followed by a -1px shift")
    void testBoxRunCompensatesGlyphSpacing() {
        Component box = TooltipBox.builder().centered().line(Component.text("Test")).build();
        String frame = PLAIN.serialize(box.children().get(0));
        int middleTiles = 27; // see testCrosshairOffset
        String expected = TooltipBox.BOX_1_LEFT + "\uF001"
                + (TooltipBox.BOX_1_MIDDLE + "\uF001").repeat(middleTiles)
                + TooltipBox.BOX_1_RIGHT + "\uF001";
        assertEquals(expected, frame);
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

    @Test
    @DisplayName("Centered box has no leading space and starts with the top-left corner")
    void testCenteredBoxHasNoLeadingSpace() {
        Component centered = TooltipBox.builder().centered().line(Component.text("Help me")).build();
        String plain = PLAIN.serialize(centered);
        assertTrue(plain.startsWith(TooltipBox.BOX_1_LEFT),
                "a centered box must start with the box itself, got: " + plain.codePointAt(0));
    }

    @Test
    @DisplayName("Centered box is the default box without its leading space")
    void testCenteredBoxEqualsDefaultWithoutLeadingSpace() {
        Component line = Component.text("Don't look\nor it takes you");
        String centered = PLAIN.serialize(TooltipBox.builder().centered().line(line).build());
        String anchored = PLAIN.serialize(TooltipBox.builder().line(line).build());
        assertTrue(anchored.endsWith(centered), "only the leading space may differ");
        String leading = anchored.substring(0, anchored.length() - centered.length());
        assertFalse(leading.isEmpty(), "the default box must keep its crosshair offset");
        for (char c : leading.toCharArray()) {
            assertTrue(c >= '\uF00F' && c <= '\uF01B', "leading part must be positive space only");
        }
    }

    @Test
    @DisplayName("centered() ignores the crosshair offset")
    void testCenteredIgnoresCrosshairOffset() {
        Component line = Component.text("Help me");
        Component a = TooltipBox.builder().centered().crosshairOffsetX(40).line(line).build();
        Component b = TooltipBox.builder().centered().line(line).build();
        assertEquals(PLAIN.serialize(b), PLAIN.serialize(a));
    }
}
