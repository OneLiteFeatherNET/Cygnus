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
}
