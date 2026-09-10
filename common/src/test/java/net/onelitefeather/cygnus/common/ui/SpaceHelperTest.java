package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaceHelperTest {

    private static final Key EXPECTED_FONT = Key.key("space", "default");

    static Stream<Arguments> negativeCases() {
        return Stream.of(
                Arguments.of(0, ""),
                Arguments.of(-5, ""),
                Arguments.of(1, "\uF001"),
                Arguments.of(2, "\uF002"),
                Arguments.of(3, "\uF003"),
                Arguments.of(4, "\uF004"),
                Arguments.of(7, "\uF007"),
                Arguments.of(8, "\uF008"),
                Arguments.of(9, "\uF008\uF001"),
                Arguments.of(15, "\uF008\uF007"),
                Arguments.of(16, "\uF009"),
                Arguments.of(32, "\uF00A"),
                Arguments.of(64, "\uF00B"),
                Arguments.of(128, "\uF00C"),
                Arguments.of(256, "\uF00D")
        );
    }

    @ParameterizedTest(name = "getNegative({0}) should return \"{1}\"")
    @MethodSource("negativeCases")
    void testNegativeDecomposition(int pixels, String expected) {
        assertEquals(expected, SpaceHelper.getNegative(pixels));
    }

    static Stream<Arguments> positiveCases() {
        return Stream.of(
                Arguments.of(0, ""),
                Arguments.of(-5, ""),
                Arguments.of(1, "\uF00F"),
                Arguments.of(2, "\uF010"),
                Arguments.of(3, "\uF011"),
                Arguments.of(4, "\uF012"),
                Arguments.of(7, "\uF015"),
                Arguments.of(8, "\uF016"),
                Arguments.of(9, "\uF016\uF00F"),
                Arguments.of(15, "\uF016\uF015"),
                Arguments.of(16, "\uF017"),
                Arguments.of(32, "\uF018"),
                Arguments.of(64, "\uF019"),
                Arguments.of(128, "\uF01A"),
                Arguments.of(256, "\uF01B")
        );
    }

    @ParameterizedTest(name = "getPositive({0}) should return \"{1}\"")
    @MethodSource("positiveCases")
    void testPositiveDecomposition(int pixels, String expected) {
        assertEquals(expected, SpaceHelper.getPositive(pixels));
    }

    @Test
    @DisplayName("Component helpers attach space:default font")
    void testComponentFont() {
        Component neg = SpaceHelper.negative(10);
        assertEquals(EXPECTED_FONT, neg.style().font());
        assertEquals("\uF008\uF002", ((net.kyori.adventure.text.TextComponent) neg).content());

        Component pos = SpaceHelper.positive(10);
        assertEquals(EXPECTED_FONT, pos.style().font());
        assertEquals("\uF016\uF010", ((net.kyori.adventure.text.TextComponent) pos).content());

        assertEquals(Component.empty(), SpaceHelper.space(0));
        assertEquals(neg, SpaceHelper.space(-10));
        assertEquals(pos, SpaceHelper.space(10));
    }
}
