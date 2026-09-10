package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constructs an authentic Adventure {@link Component} that renders a modular 9-slice tooltip frame
 * and background from the {@code cygnus:tooltip} font with negative space shifting behind text content.
 */
public final class TooltipBox {

    /**
     * Font key for tooltip frame and spacing glyphs.
     */
    public static final Key FONT = Key.key("cygnus", "tooltip");
    public static final Key TOOLTIP_FONT = FONT;

    // 9-slice glyph constants
    public static final String CORNER_TL = "\uE100";
    public static final String BORDER_TOP = "\uE101";
    public static final String CORNER_TR = "\uE102";
    public static final String BORDER_LEFT = "\uE103";
    public static final String BG_FILL = "\uE104";
    public static final String BORDER_RIGHT = "\uE105";
    public static final String CORNER_BL = "\uE106";
    public static final String BORDER_BOTTOM = "\uE107";
    public static final String CORNER_BR = "\uE108";

    public static final int DEFAULT_PADDING = 4;
    public static final int MIN_CONTENT_WIDTH = 10;

    private static final int[] NEGATIVE_SPACE_VALUES = {128, 64, 32, 16, 8, 4, 2, 1};
    private static final char[] NEGATIVE_SPACE_GLYPHS = {'\uF880', '\uF840', '\uF820', '\uF810', '\uF808', '\uF804', '\uF802', '\uF801'};

    private TooltipBox() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Decomposes a positive pixel offset into binary combinations of negative-space font glyphs
     * configured in the {@code cygnus:tooltip} font.
     *
     * @param pixels the number of pixels to move left (cursor shift)
     * @return the string sequence of negative space characters
     */
    @NotNull
    public static String getNegativeSpace(int pixels) {
        if (pixels <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int remaining = pixels;
        for (int i = 0; i < NEGATIVE_SPACE_VALUES.length; i++) {
            while (remaining >= NEGATIVE_SPACE_VALUES[i]) {
                sb.append(NEGATIVE_SPACE_GLYPHS[i]);
                remaining -= NEGATIVE_SPACE_VALUES[i];
            }
        }
        return sb.toString();
    }

    /**
     * Creates a new {@link Builder} to construct a custom tooltip box.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience method to construct a tooltip box with default padding around the given lines.
     *
     * @param lines the lines of text content
     * @return the constructed component
     */
    @NotNull
    public static Component of(@Nullable Component... lines) {
        Builder builder = builder();
        if (lines != null) {
            for (Component line : lines) {
                builder.line(line);
            }
        }
        return builder.build();
    }

    /**
     * Convenience method to construct a tooltip box with default padding around the given lines.
     *
     * @param lines the lines of text content
     * @return the constructed component
     */
    @NotNull
    public static Component of(@Nullable List<Component> lines) {
        return builder().lines(lines).build();
    }

    /**
     * Builder for constructing modular 9-slice tooltip boxes.
     */
    public static final class Builder {
        private final List<Component> lines = new ArrayList<>();
        private int padding = DEFAULT_PADDING;

        private Builder() {
        }

        /**
         * Adds a line of text content to the tooltip box.
         *
         * @param line the line component to add
         * @return this builder
         */
        @NotNull
        public Builder line(@Nullable Component line) {
            if (line != null) {
                this.lines.add(line);
            }
            return this;
        }

        /**
         * Adds multiple lines of text content to the tooltip box.
         *
         * @param lines the line components to add
         * @return this builder
         */
        @NotNull
        public Builder lines(@Nullable List<Component> lines) {
            if (lines != null) {
                for (Component line : lines) {
                    line(line);
                }
            }
            return this;
        }

        /**
         * Adds multiple lines of text content to the tooltip box.
         *
         * @param lines the line components to add
         * @return this builder
         */
        @NotNull
        public Builder lines(@Nullable Component... lines) {
            if (lines != null) {
                for (Component line : lines) {
                    line(line);
                }
            }
            return this;
        }

        /**
         * Configures the horizontal padding (in pixels) between the tooltip borders and content.
         *
         * @param padding the horizontal padding in pixels (>= 0)
         * @return this builder
         */
        @NotNull
        public Builder padding(int padding) {
            this.padding = Math.max(0, padding);
            return this;
        }

        /**
         * Builds the Adventure {@link Component} containing the top border, body lines
         * with negative space cursor shifts, and bottom border.
         *
         * @return the built tooltip box component
         */
        @NotNull
        public Component build() {
            int maxContentWidth = MIN_CONTENT_WIDTH;
            for (Component line : this.lines) {
                maxContentWidth = Math.max(maxContentWidth, FontWidthHelper.getWidth(line));
            }

            int innerWidth = maxContentWidth + (this.padding * 2);
            int totalShift = innerWidth + 3 - this.padding;
            String shiftStr = getNegativeSpace(totalShift);

            TextComponent.Builder root = Component.text();

            // Top border row
            String topBorderStr = CORNER_TL + BORDER_TOP.repeat(innerWidth) + CORNER_TR + "\n";
            root.append(Component.text(topBorderStr, NamedTextColor.WHITE).font(FONT));

            // Line rows
            String bgStr = BORDER_LEFT + BG_FILL.repeat(innerWidth) + BORDER_RIGHT;
            for (Component line : this.lines) {
                root.append(Component.text(bgStr, NamedTextColor.WHITE).font(FONT));
                root.append(Component.text(shiftStr, NamedTextColor.WHITE).font(FONT));
                root.append(line);
                root.append(Component.newline());
            }

            // Bottom border row
            String bottomBorderStr = CORNER_BL + BORDER_BOTTOM.repeat(innerWidth) + CORNER_BR;
            root.append(Component.text(bottomBorderStr, NamedTextColor.WHITE).font(FONT));

            return root.build();
        }
    }
}
