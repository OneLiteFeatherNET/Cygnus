package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Utility helper for negative and positive pixel space shifts using the {@code space:default} font.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class SpaceHelper {

    public static final Key FONT = Key.key("space", "default");

    // Negative space values and glyphs in space:default
    // \uF001: -1 ... \uF008: -8, \uF009: -16, \uF00A: -32, \uF00B: -64, \uF00C: -128, \uF00D: -256
    private static final int[] NEGATIVE_VALUES = {256, 128, 64, 32, 16, 8, 7, 6, 5, 4, 3, 2, 1};
    private static final char[] NEGATIVE_GLYPHS = {
            '\uF00D', '\uF00C', '\uF00B', '\uF00A', '\uF009',
            '\uF008', '\uF007', '\uF006', '\uF005', '\uF004', '\uF003', '\uF002', '\uF001'
    };

    // Positive space values and glyphs in space:default
    // \uF00F: 1 ... \uF016: 8, \uF017: 16, \uF018: 32, \uF019: 64, \uF01A: 128, \uF01B: 256
    private static final int[] POSITIVE_VALUES = {256, 128, 64, 32, 16, 8, 7, 6, 5, 4, 3, 2, 1};
    private static final char[] POSITIVE_GLYPHS = {
            '\uF01B', '\uF01A', '\uF019', '\uF018', '\uF017',
            '\uF016', '\uF015', '\uF014', '\uF013', '\uF012', '\uF011', '\uF010', '\uF00F'
    };

    private SpaceHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Decomposes a positive pixel amount into a string sequence of negative space glyphs
     * (shifting the cursor left by the given number of pixels).
     *
     * @param pixels the number of pixels to move left (>= 0)
     * @return the string of negative space characters
     */
    @NotNull
    public static String getNegative(int pixels) {
        if (pixels <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int remaining = pixels;
        for (int i = 0; i < NEGATIVE_VALUES.length; i++) {
            while (remaining >= NEGATIVE_VALUES[i]) {
                sb.append(NEGATIVE_GLYPHS[i]);
                remaining -= NEGATIVE_VALUES[i];
            }
        }
        return sb.toString();
    }

    /**
     * Decomposes a positive pixel amount into a string sequence of positive space glyphs
     * (shifting the cursor right by the given number of pixels).
     *
     * @param pixels the number of pixels to move right (>= 0)
     * @return the string of positive space characters
     */
    @NotNull
    public static String getPositive(int pixels) {
        if (pixels <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int remaining = pixels;
        for (int i = 0; i < POSITIVE_VALUES.length; i++) {
            while (remaining >= POSITIVE_VALUES[i]) {
                sb.append(POSITIVE_GLYPHS[i]);
                remaining -= POSITIVE_VALUES[i];
            }
        }
        return sb.toString();
    }

    /**
     * Returns a {@link Component} shifting the cursor to the left by the given positive pixel amount,
     * styled with {@code space:default}.
     *
     * @param pixels the number of pixels to shift left
     * @return the styled negative space component
     */
    @NotNull
    public static Component negative(int pixels) {
        if (pixels <= 0) {
            return Component.empty();
        }
        return Component.text(getNegative(pixels)).font(FONT);
    }

    /**
     * Returns a {@link Component} shifting the cursor to the right by the given positive pixel amount,
     * styled with {@code space:default}.
     *
     * @param pixels the number of pixels to shift right
     * @return the styled positive space component
     */
    @NotNull
    public static Component positive(int pixels) {
        if (pixels <= 0) {
            return Component.empty();
        }
        return Component.text(getPositive(pixels)).font(FONT);
    }

    /**
     * Returns a {@link Component} shifting the cursor by the given signed pixel amount.
     * Negative values shift left, positive values shift right, and zero returns empty.
     *
     * @param pixels the signed pixel amount
     * @return the styled space component
     */
    @NotNull
    public static Component space(int pixels) {
        if (pixels < 0) {
            return negative(-pixels);
        } else if (pixels > 0) {
            return positive(pixels);
        }
        return Component.empty();
    }
}
