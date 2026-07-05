package net.onelitefeather.cygnus.common.util;

import net.kyori.adventure.util.RGBLike;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {

    private static final int TOLERANCE = 1;

    @Test
    void zeroSaturationAndZeroLightnessIsBlack() {
        RGBLike color = ColorUtil.hsl(0f, 0f, 0f);
        assertRgb(color, 0, 0, 0);
    }

    @Test
    void zeroSaturationAndFullLightnessIsWhite() {
        RGBLike color = ColorUtil.hsl(0f, 0f, 1f);
        assertRgb(color, 255, 255, 255);
    }

    @Test
    void zeroSaturationIsGrayRegardlessOfHue() {
        RGBLike atZeroHue = ColorUtil.hsl(0f, 0f, 0.5f);
        RGBLike atOtherHue = ColorUtil.hsl(275f, 0f, 0.5f);

        assertRgb(atZeroHue, 128, 128, 128);
        assertEquals(atZeroHue.red(), atOtherHue.red());
        assertEquals(atZeroHue.green(), atOtherHue.green());
        assertEquals(atZeroHue.blue(), atOtherHue.blue());
    }

    @ParameterizedTest
    @CsvSource({
            "0,   255, 0,   0",   // red
            "120, 0,   255, 0",   // green
            "240, 0,   0,   255"  // blue
    })
    void fullySaturatedPrimaryHues(float hue, int r, int g, int b) {
        RGBLike color = ColorUtil.hsl(hue, 1f, 0.5f);
        assertRgb(color, r, g, b);
    }

    @Test
    void hueWrapsAt360Degrees() {
        RGBLike atZero = ColorUtil.hsl(0f, 1f, 0.5f);
        RGBLike atFull = ColorUtil.hsl(360f, 1f, 0.5f);

        assertRgb(atFull, atZero.red(), atZero.green(), atZero.blue());
    }

    @Test
    void negativeHueWrapsIntoValidRange() {
        // -120 degrees should behave the same as 240 degrees (blue)
        RGBLike negative = ColorUtil.hsl(-120f, 1f, 0.5f);
        RGBLike equivalent = ColorUtil.hsl(240f, 1f, 0.5f);

        assertRgb(negative, equivalent.red(), equivalent.green(), equivalent.blue());
    }

    @Test
    void hueGreaterThan360WrapsCorrectly() {
        RGBLike overRotated = ColorUtil.hsl(480f, 1f, 0.5f);
        assertRgb(overRotated, 0, 255, 0);
    }

    @Test
    void outputChannelsStayWithinValidByteRange() {
        // sweep a range of inputs, including ones that could overflow rounding,
        // and make sure every channel stays within [0, 255]
        for (float hue = -720f; hue <= 720f; hue += 37.5f) {
            for (float sat = -0.5f; sat <= 1.5f; sat += 0.5f) {
                for (float light = -0.5f; light <= 1.5f; light += 0.5f) {
                    RGBLike color = ColorUtil.hsl(hue, sat, light);

                    assertTrue(color.red() >= 0 && color.red() <= 255,
                            "red out of range for hue=" + hue + " sat=" + sat + " light=" + light);
                    assertTrue(color.green() >= 0 && color.green() <= 255,
                            "green out of range for hue=" + hue + " sat=" + sat + " light=" + light);
                    assertTrue(color.blue() >= 0 && color.blue() <= 255,
                            "blue out of range for hue=" + hue + " sat=" + sat + " light=" + light);
                }
            }
        }
    }

    /**
     * Custom assertion for RGBLike colors.
     *
     * @param color         to check
     * @param expectedRed   value
     * @param expectedGreen value
     * @param expectedBlue  value
     */
    private static void assertRgb(RGBLike color, int expectedRed, int expectedGreen, int expectedBlue) {
        assertTrue(Math.abs(color.red() - expectedRed) <= TOLERANCE,
                "red: expected " + expectedRed + " but was " + color.red());
        assertTrue(Math.abs(color.green() - expectedGreen) <= TOLERANCE,
                "green: expected " + expectedGreen + " but was " + color.green());
        assertTrue(Math.abs(color.blue() - expectedBlue) <= TOLERANCE,
                "blue: expected " + expectedBlue + " but was " + color.blue());
    }
}