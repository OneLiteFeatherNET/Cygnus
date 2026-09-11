package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Utility helper to compute the pixel width of Minecraft text
 * (plain strings and Adventure {@link Component}s) using standard Minecraft font metrics.
 */
public final class FontWidthHelper {

    private static final int DEFAULT_CHAR_WIDTH = 6; // 5px character + 1px spacing
    private static final int[] ASCII_WIDTHS = new int[128];

    private static final int DEFAULT_TOOLTIP_CHAR_WIDTH = 5;
    private static final int[] TOOLTIP_ASCII_WIDTHS = new int[128];

    static {
        // Initialize all ASCII characters to default width (5px + 1px spacing = 6px)
        Arrays.fill(ASCII_WIDTHS, DEFAULT_CHAR_WIDTH);

        // Specific Minecraft standard font character widths (base width + 1px spacing)
        ASCII_WIDTHS[' '] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS['!'] = 2;   // 1px + 1px spacing
        ASCII_WIDTHS['"'] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS[';'] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS[':'] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS['\''] = 2;  // 1px + 1px spacing
        ASCII_WIDTHS['`'] = 2;   // 1px + 1px spacing
        ASCII_WIDTHS['i'] = 2;   // 1px + 1px spacing
        ASCII_WIDTHS['l'] = 2;   // 1px + 1px spacing
        ASCII_WIDTHS['I'] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS['['] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS[']'] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS['t'] = 4;   // 3px + 1px spacing
        ASCII_WIDTHS['k'] = 5;   // 4px + 1px spacing
        ASCII_WIDTHS['f'] = 5;   // 4px + 1px spacing

        // Specific compact tooltip font metrics (height 6, ascent 5 in 26.2)
        Arrays.fill(TOOLTIP_ASCII_WIDTHS, DEFAULT_TOOLTIP_CHAR_WIDTH);
        TOOLTIP_ASCII_WIDTHS[' '] = 4;
        TOOLTIP_ASCII_WIDTHS['!'] = 2;
        TOOLTIP_ASCII_WIDTHS['"'] = 3;
        TOOLTIP_ASCII_WIDTHS['\''] = 2;
        TOOLTIP_ASCII_WIDTHS['('] = 3;
        TOOLTIP_ASCII_WIDTHS[')'] = 3;
        TOOLTIP_ASCII_WIDTHS['*'] = 3;
        TOOLTIP_ASCII_WIDTHS[','] = 2;
        TOOLTIP_ASCII_WIDTHS['.'] = 2;
        TOOLTIP_ASCII_WIDTHS[':'] = 2;
        TOOLTIP_ASCII_WIDTHS[';'] = 2;
        TOOLTIP_ASCII_WIDTHS['<'] = 4;
        TOOLTIP_ASCII_WIDTHS['>'] = 4;
        TOOLTIP_ASCII_WIDTHS['@'] = 6;
        TOOLTIP_ASCII_WIDTHS['I'] = 3;
        TOOLTIP_ASCII_WIDTHS['['] = 3;
        TOOLTIP_ASCII_WIDTHS[']'] = 3;
        TOOLTIP_ASCII_WIDTHS['`'] = 3;
        TOOLTIP_ASCII_WIDTHS['f'] = 4;
        TOOLTIP_ASCII_WIDTHS['i'] = 2;
        TOOLTIP_ASCII_WIDTHS['k'] = 4;
        TOOLTIP_ASCII_WIDTHS['l'] = 2;
        TOOLTIP_ASCII_WIDTHS['t'] = 3;
        TOOLTIP_ASCII_WIDTHS['{'] = 3;
        TOOLTIP_ASCII_WIDTHS['|'] = 2;
        TOOLTIP_ASCII_WIDTHS['}'] = 3;
        TOOLTIP_ASCII_WIDTHS['~'] = 6;
    }

    private FontWidthHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns the pixel width of a single character in the standard font.
     *
     * @param c the character
     * @return width in pixels
     */
    public static int getCharWidth(char c) {
        return getCharWidth(c, false);
    }

    /**
     * Returns the pixel width of a single character in the standard font,
     * taking bold styling into account (+1px if bold).
     *
     * @param c    the character
     * @param bold whether bold formatting is enabled
     * @return width in pixels
     */
    public static int getCharWidth(char c, boolean bold) {
        int width = (c < 128) ? ASCII_WIDTHS[c] : DEFAULT_CHAR_WIDTH;
        return bold ? width + 1 : width;
    }

    /**
     * Returns the total pixel width of a plain text string in the standard font.
     *
     * @param text the string to measure, may be null
     * @return total width in pixels
     */
    public static int getWidth(@Nullable String text) {
        return getWidth(text, false);
    }

    /**
     * Returns the total pixel width of a plain text string in the standard font,
     * with an optional bold modifier.
     *
     * @param text the string to measure, may be null
     * @param bold whether bold formatting is enabled
     * @return total width in pixels
     */
    public static int getWidth(@Nullable String text, boolean bold) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += getCharWidth(text.charAt(i), bold);
        }
        return total;
    }

    /**
     * Traverses component children and flattens styles to compute the total pixel width
     * of all text parts, accounting for bold text decoration inheritance.
     *
     * @param component the component to measure, may be null
     * @return total width in pixels
     */
    public static int getWidth(@Nullable Component component) {
        if (component == null) {
            return 0;
        }
        return computeComponentWidth(component, false);
    }

    private static int computeComponentWidth(Component component, boolean parentBold) {
        TextDecoration.State boldState = component.decoration(TextDecoration.BOLD);
        boolean currentBold = (boldState == TextDecoration.State.TRUE)
                || (boldState == TextDecoration.State.NOT_SET && parentBold);

        int totalWidth = 0;
        if (component instanceof TextComponent textComponent) {
            totalWidth += getWidth(textComponent.content(), currentBold);
        }

        for (Component child : component.children()) {
            totalWidth += computeComponentWidth(child, currentBold);
        }
        return totalWidth;
    }

    /**
     * Returns the pixel width of a single character in the compact tooltip font (height 6).
     *
     * @param c the character
     * @return width in pixels
     */
    public static int getTooltipCharWidth(char c) {
        return getTooltipCharWidth(c, false);
    }

    /**
     * Returns the pixel width of a single character in the compact tooltip font (height 6),
     * taking bold styling into account (+1px if bold).
     *
     * @param c    the character
     * @param bold whether bold formatting is enabled
     * @return width in pixels
     */
    public static int getTooltipCharWidth(char c, boolean bold) {
        int width = (c < 128) ? TOOLTIP_ASCII_WIDTHS[c] : DEFAULT_TOOLTIP_CHAR_WIDTH;
        return bold ? width + 1 : width;
    }

    /**
     * Returns the total pixel width of a plain text string in the compact tooltip font.
     *
     * @param text the string to measure, may be null
     * @return total width in pixels
     */
    public static int getTooltipWidth(@Nullable String text) {
        return getTooltipWidth(text, false);
    }

    /**
     * Returns the total pixel width of a plain text string in the compact tooltip font,
     * with an optional bold modifier.
     *
     * @param text the string to measure, may be null
     * @param bold whether bold formatting is enabled
     * @return total width in pixels
     */
    public static int getTooltipWidth(@Nullable String text, boolean bold) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += getTooltipCharWidth(text.charAt(i), bold);
        }
        return total;
    }

    /**
     * Traverses component children and flattens styles to compute the total pixel width
     * in the compact tooltip font, accounting for bold text decoration inheritance.
     *
     * @param component the component to measure, may be null
     * @return total width in pixels
     */
    public static int getTooltipWidth(@Nullable Component component) {
        if (component == null) {
            return 0;
        }
        return computeTooltipComponentWidth(component, false);
    }

    private static int computeTooltipComponentWidth(Component component, boolean parentBold) {
        TextDecoration.State boldState = component.decoration(TextDecoration.BOLD);
        boolean currentBold = (boldState == TextDecoration.State.TRUE)
                || (boldState == TextDecoration.State.NOT_SET && parentBold);

        int totalWidth = 0;
        if (component instanceof TextComponent textComponent) {
            totalWidth += getTooltipWidth(textComponent.content(), currentBold);
        }

        for (Component child : component.children()) {
            totalWidth += computeTooltipComponentWidth(child, currentBold);
        }
        return totalWidth;
    }

    /**
     * Returns the maximum pixel width across lines separated by '\n' in the compact tooltip font.
     *
     * @param text the multi-line text to measure, may be null
     * @return maximum line width in pixels
     */
    public static int getMaxTooltipLineWidth(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int maxWidth = 0;
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, getTooltipWidth(line));
        }
        return maxWidth;
    }

    /**
     * Traverses component children and flattens styles to compute the maximum line width
     * across '\n'-separated lines in the compact tooltip font.
     *
     * @param component the component to measure, may be null
     * @return maximum line width in pixels
     */
    public static int getMaxTooltipLineWidth(@Nullable Component component) {
        if (component == null) {
            return 0;
        }
        // Extract flat plain text or measure line-by-line
        // For text components in Cygnus, notes are simple strings or simple component trees
        int[] lineAcc = new int[]{0};
        int[] maxAcc = new int[]{0};
        computeComponentMaxLineWidth(component, false, lineAcc, maxAcc);
        return Math.max(maxAcc[0], lineAcc[0]);
    }

    private static void computeComponentMaxLineWidth(Component component, boolean parentBold, int[] lineAcc, int[] maxAcc) {
        TextDecoration.State boldState = component.decoration(TextDecoration.BOLD);
        boolean currentBold = (boldState == TextDecoration.State.TRUE)
                || (boldState == TextDecoration.State.NOT_SET && parentBold);

        if (component instanceof TextComponent textComponent) {
            String content = textComponent.content();
            String[] parts = content.split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    maxAcc[0] = Math.max(maxAcc[0], lineAcc[0]);
                    lineAcc[0] = 0;
                }
                lineAcc[0] += getTooltipWidth(parts[i], currentBold);
            }
        }

        for (Component child : component.children()) {
            computeComponentMaxLineWidth(child, currentBold, lineAcc, maxAcc);
        }
    }
}
