package net.onelitefeather.cygnus.common.util;

import net.minestom.server.color.Color;

/**
 * Converts HSL (hue, saturation, lightness) colors into the plain RGB
 * {@link Color} that Minestom expects.
 * <p>
 * The reason this exists: picking three random RGB values almost never looks
 * good together, but picking colors as variations on one hue almost always
 * does. HSL lets us say "same hue, just a bit darker" or "same hue, more
 * washed out" instead of guessing at RGB numbers by hand. That's exactly what
 * {@link SeededDimensionPreset} relies on to turn a single random seed into a
 * fog/sky/light color set that actually feels like it belongs together.
 * <p>
 * If you're not familiar with HSL: hue is the color itself (0 = red, 120 =
 * green, 240 = blue, and it loops back around at 360), saturation is how
 * intense or washed-out the color is, and lightness is how close it is to
 * black or white. Turning a knob on any one of these while leaving the others
 * alone gives you a predictable, related color — which is much harder to do
 * by nudging R, G, and B by hand.
 */
public final class ColorUtil {

    private ColorUtil() {
        // no instances, this is just a bag of static helpers
    }

    /**
     * Turns an HSL color into the RGB {@link Color} Minestom works with.
     * <p>
     * Hue can technically be any float, in or out of the usual 0-360 range —
     * negative values and values above 360 are wrapped around automatically,
     * so you don't need to normalize it yourself before calling this.
     * Saturation and lightness are expected in the 0-1 range, but are clamped
     * defensively at the end just in case something upstream (e.g. a
     * seed-based calculation) produces a value slightly outside that range.
     *
     * @param hue        the hue in degrees; 0/360 = red, 120 = green, 240 = blue.
     *                   Values outside 0-360 wrap around, so -30 behaves like 330.
     * @param saturation how intense the color is, from 0 (gray, no color at
     *                   all) to 1 (fully saturated)
     * @param lightness  how close to black or white the color is, from 0
     *                   (black) through 0.5 (the "pure" color) to 1 (white)
     * @return the equivalent RGB color, ready to hand to Minestom
     */
    public static Color hsl(float hue, float saturation, float lightness) {
        float h = ((hue % 360f) + 360f) % 360f / 360f;
        float r;
        float g;
        float b;

        if (saturation <= 0f) {
            r = g = b = lightness;
        } else {
            float q = lightness < 0.5f
                    ? lightness * (1f + saturation)
                    : lightness + saturation - lightness * saturation;
            float p = 2f * lightness - q;
            r = hueToRgb(p, q, h + 1f / 3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f / 3f);
        }

        return new Color(
                Math.round(clamp(r) * 255f),
                Math.round(clamp(g) * 255f),
                Math.round(clamp(b) * 255f)
        );
    }

    /**
     * The messy middle step of HSL -> RGB conversion. There's no intuitive
     * way to explain what {@code p}, {@code q}, and {@code t} "mean" here —
     * this is just the standard textbook formula for pulling one RGB channel
     * (red, green, or blue) out of an HSL color, applied three times with a
     * shifted {@code t} for each channel. Not worth reinventing.
     */
    private static float hueToRgb(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f / 6f) return p + (q - p) * 6f * t;
        if (t < 1f / 2f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f;
        return p;
    }

    /**
     * Keeps a value inside [0, 1] before it gets scaled up to a 0-255 color
     * channel. Rounding and slightly-out-of-range inputs can otherwise push a
     * channel just past 0 or 255, which would produce an invalid color.
     */
    private static float clamp(float value) {
        return Math.clamp(value, 0f, 1f);
    }
}