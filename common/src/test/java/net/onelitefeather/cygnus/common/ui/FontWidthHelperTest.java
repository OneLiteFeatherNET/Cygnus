package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FontWidthHelperTest {

    static Stream<Arguments> specificCharWidths() {
        return Stream.of(
                Arguments.of(' ', 4),
                Arguments.of('!', 2),
                Arguments.of('"', 4),
                Arguments.of(';', 4),
                Arguments.of(':', 4),
                Arguments.of('\'', 2),
                Arguments.of('`', 2),
                Arguments.of('i', 2),
                Arguments.of('l', 2),
                Arguments.of('I', 4),
                Arguments.of('[', 4),
                Arguments.of(']', 4),
                Arguments.of('t', 4),
                Arguments.of('k', 5),
                Arguments.of('f', 5),
                Arguments.of('a', 6),
                Arguments.of('A', 6),
                Arguments.of('0', 6),
                Arguments.of('z', 6),
                Arguments.of('Z', 6),
                Arguments.of('ä', 6)
        );
    }

    @ParameterizedTest(name = "getCharWidth(''{0}'') should return {1}")
    @MethodSource("specificCharWidths")
    void testSpecificCharWidths(char c, int expectedWidth) {
        assertEquals(expectedWidth, FontWidthHelper.getCharWidth(c));
    }

    static Stream<Arguments> boldCharWidths() {
        return Stream.of(
                Arguments.of(' ', 5),
                Arguments.of('!', 3),
                Arguments.of('"', 5),
                Arguments.of(';', 5),
                Arguments.of(':', 5),
                Arguments.of('\'', 3),
                Arguments.of('`', 3),
                Arguments.of('i', 3),
                Arguments.of('l', 3),
                Arguments.of('I', 5),
                Arguments.of('[', 5),
                Arguments.of(']', 5),
                Arguments.of('t', 5),
                Arguments.of('k', 6),
                Arguments.of('f', 6),
                Arguments.of('a', 7)
        );
    }

    @ParameterizedTest(name = "getCharWidth(''{0}'', bold=true) should return {1}")
    @MethodSource("boldCharWidths")
    void testBoldCharWidths(char c, int expectedWidth) {
        assertEquals(expectedWidth, FontWidthHelper.getCharWidth(c, true));
    }

    @Test
    @DisplayName("Empty or null text returns 0")
    void testEmptyAndNullString() {
        assertEquals(0, FontWidthHelper.getWidth((String) null));
        assertEquals(0, FontWidthHelper.getWidth(""));
        assertEquals(0, FontWidthHelper.getWidth("", true));
    }

    @Test
    @DisplayName("String width calculation without bold")
    void testStringWidthPlain() {
        // 'f'(5) + 'i'(2) + 't'(4) = 11
        assertEquals(11, FontWidthHelper.getWidth("fit"));
        // 'H'(6) + 'e'(6) + 'l'(2) + 'p'(6) + ' '(4) + 'm'(6) + 'e'(6) = 36
        assertEquals(36, FontWidthHelper.getWidth("Help me"));
    }

    @Test
    @DisplayName("String width calculation with bold")
    void testStringWidthBold() {
        // 'f'(6) + 'i'(3) + 't'(5) = 14
        assertEquals(14, FontWidthHelper.getWidth("fit", true));
        // 'H'(7) + 'e'(7) + 'l'(3) + 'p'(7) + ' '(5) + 'm'(7) + 'e'(7) = 43
        assertEquals(43, FontWidthHelper.getWidth("Help me", true));
    }

    @Test
    @DisplayName("Component width with null or empty")
    void testComponentNullOrEmpty() {
        assertEquals(0, FontWidthHelper.getWidth((Component) null));
        assertEquals(0, FontWidthHelper.getWidth(Component.empty()));
    }

    @Test
    @DisplayName("Component width for plain text")
    void testComponentPlain() {
        Component component = Component.text("Help me");
        assertEquals(36, FontWidthHelper.getWidth(component));
    }

    @Test
    @DisplayName("Component width with bold styling")
    void testComponentBold() {
        Component component = Component.text("Help me", Style.style(TextDecoration.BOLD));
        assertEquals(43, FontWidthHelper.getWidth(component));
    }

    @Test
    @DisplayName("Component width with child inheriting bold")
    void testComponentChildInheritsBold() {
        // "Hi" (bold): H(7) + i(3) = 10
        // " " (bold): 5
        // "there" (bold): t(5) + h(7) + e(7) + r(7) + e(7) = 33
        // Total = 10 + 5 + 33 = 48
        Component root = Component.text("Hi", Style.style(TextDecoration.BOLD))
                .append(Component.text(" there"));
        assertEquals(48, FontWidthHelper.getWidth(root));
    }

    @Test
    @DisplayName("Component width where child overrides bold to false")
    void testComponentChildOverridesBold() {
        // "Hi" (bold): H(7) + i(3) = 10
        // "!" (explicitly bold=false): !(2) = 2
        // Total = 12
        Component root = Component.text("Hi", Style.style(TextDecoration.BOLD))
                .append(Component.text("!").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));
        assertEquals(12, FontWidthHelper.getWidth(root));
    }

    @Test
    @DisplayName("Tooltip font width calculation for 6px compact font")
    void testTooltipFontWidth() {
        // "Test": T(5) + e(5) + s(5) + t(3) = 18
        assertEquals(18, FontWidthHelper.getTooltipWidth("Test"));
        // "Help me": H(5) + e(5) + l(2) + p(5) + ' '(4) + m(5) + e(5) = 31
        assertEquals(31, FontWidthHelper.getTooltipWidth("Help me"));
        // Component measurement
        assertEquals(31, FontWidthHelper.getTooltipWidth(Component.text("Help me")));
    }
}
