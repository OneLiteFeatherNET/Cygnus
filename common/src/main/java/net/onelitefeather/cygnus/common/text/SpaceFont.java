package net.theevilreaper.manis.common.font;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.Contract;

/**
 * Utility for building pixel-offset strings using the {@code space:default} font.
 * <p>
 * Maps to the negative-space font definition under {@code <font:space:default>}.
 * Usage: combine characters to reach any offset, similar to how you'd make
 * change with coins.
 */
public final class SpaceFont {

    private static final Key FONT_KEY = Key.key("space", "default");
    private static final Component RESET = Component.empty().font(Style.DEFAULT_FONT);

    // --- Negative offsets ---
    private static final char NEG_1   = '\uF001';
    private static final char NEG_2   = '\uF002';
    private static final char NEG_3   = '\uF003';
    private static final char NEG_4   = '\uF004';
    private static final char NEG_5   = '\uF005';
    private static final char NEG_6   = '\uF006';
    private static final char NEG_7   = '\uF007';
    private static final char NEG_8   = '\uF008';
    private static final char NEG_16  = '\uF009';
    private static final char NEG_32  = '\uF00A';
    private static final char NEG_64  = '\uF00B';
    private static final char NEG_128 = '\uF00C';
    private static final char NEG_256 = '\uF00D';

    // --- Positive offsets ---
    private static final char ZERO    = '\uF00E'; // 0 (no-op, useful as placeholder)
    private static final char POS_1   = '\uF00F';
    // '\u0020' (space) = 1 as well
    private static final char POS_2   = '\uF010';
    private static final char POS_3   = '\uF011';
    private static final char POS_4   = '\uF012';
    private static final char POS_5   = '\uF013';
    private static final char POS_6   = '\uF014';
    private static final char POS_7   = '\uF015';
    private static final char POS_8   = '\uF016';
    private static final char POS_16  = '\uF017';
    private static final char POS_32  = '\uF018';
    private static final char POS_64  = '\uF019';
    private static final char POS_128 = '\uF01A';
    private static final char POS_256 = '\uF01B';

    private static final int[] MAGNITUDES = {256, 128, 64, 32, 16, 8, 7, 6, 5, 4, 3, 2, 1};

    private SpaceFont() {}

    /**
     * Builds a string that shifts the cursor {@code pixels} pixels to the left (negative).
     *
     * @param pixels positive number of pixels to shift left, e.g. {@code 128}
     * @return the composed character string to embed in a Component
     */
    @Contract(pure = true)
    public static Component negative(int pixels) {
        return Component.text(build(pixels, true))
                .font(FONT_KEY).append(RESET);
    }

    /**
     * Builds a string that shifts the cursor {@code pixels} pixels to the right (positive).
     *
     * @param pixels positive number of pixels to shift right, e.g. {@code 32}
     * @return the composed character string to embed in a Component
     */
    @Contract(pure = true)
    public static Component positive(int pixels) {
        return Component.text(build(pixels, false))
                .font(FONT_KEY).append(RESET);
    }
    /**
     * Offset with sign: negative value → shift left, positive → shift right.
     */
    @Contract(pure = true)
    public static Component offset(int pixels) {
        if (pixels < 0) return negative(-pixels);
        if (pixels > 0) return positive(pixels);
        return Component.empty();
    }

    // --- Internal ---

    /**
     * Converts a given amount of pixels into a char representation to shift them around
     * @param pixels the amount of pixels to shift
     * @param negative if negative or not
     * @return a string which contains the pixel data
     */
    private static String build(int pixels, boolean negative) {
        if (pixels < 0) throw new IllegalArgumentException("pixels must be >= 0, got " + pixels);

        StringBuilder sb = new StringBuilder();
        int remaining = pixels;

        for (int magnitude : MAGNITUDES) {
            if (remaining >= magnitude) {
                sb.append(negative ? negChar(magnitude) : posChar(magnitude));
                remaining -= magnitude;
            }
        }
        return sb.toString();
    }

    private static char negChar(int magnitude) {
        return switch (magnitude) {
            case 256 -> NEG_256;
            case 128 -> NEG_128;
            case 64  -> NEG_64;
            case 32  -> NEG_32;
            case 16  -> NEG_16;
            case 8   -> NEG_8;
            case 7   -> NEG_7;
            case 6   -> NEG_6;
            case 5   -> NEG_5;
            case 4   -> NEG_4;
            case 3   -> NEG_3;
            case 2   -> NEG_2;
            case 1   -> NEG_1;
            default  -> throw new IllegalArgumentException("Unsupported magnitude: " + magnitude);
        };
    }

    private static char posChar(int magnitude) {
        return switch (magnitude) {
            case 256 -> POS_256;
            case 128 -> POS_128;
            case 64  -> POS_64;
            case 32  -> POS_32;
            case 16  -> POS_16;
            case 8   -> POS_8;
            case 7   -> POS_7;
            case 6   -> POS_6;
            case 5   -> POS_5;
            case 4   -> POS_4;
            case 3   -> POS_3;
            case 2   -> POS_2;
            case 1   -> POS_1;
            default  -> throw new IllegalArgumentException("Unsupported magnitude: " + magnitude);
        };
    }
}
