package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs an authentic Adventure {@link Component} that renders a modular 3-part tooltip frame
 * and background from the {@code cygnus:tooltip} font with cursor shifts and crosshair alignment.
 *
 * @author theEvilReaper
 * @version 2.1.0
 * @since 2.15.0
 */
public final class TooltipBox {

    /**
     * Font key for tooltip frame and spacing glyphs.
     */
    public static final Key FONT = Key.key("cygnus", "tooltip");
    public static final Key TOOLTIP_FONT = FONT;

    // 3-part full-height glyph constants (height 16, ascent 11)
    public static final String CAP_LEFT = "\uE100";
    public static final String MIDDLE = "\uE101";
    public static final String CAP_RIGHT = "\uE102";

    public static final int CAP_WIDTH = 5;
    public static final int MIN_CONTENT_WIDTH = 10;
    public static final int DEFAULT_CROSSHAIR_OFFSET = 16;

    private static final int[] SPACE_VALUES = {128, 64, 32, 16, 8, 4, 2, 1};
    private static final char[] NEGATIVE_SPACE_GLYPHS = {'\uF880', '\uF840', '\uF820', '\uF810', '\uF808', '\uF804', '\uF802', '\uF801'};
    private static final char[] POSITIVE_SPACE_GLYPHS = {'\uF841', '\uF821', '\uF811', '\uF809', '\uF807', '\uF806', '\uF805', '\uF803'};

    private TooltipBox() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Decomposes a positive pixel offset into binary combinations of negative-space font glyphs.
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
        for (int i = 0; i < SPACE_VALUES.length; i++) {
            while (remaining >= SPACE_VALUES[i]) {
                sb.append(NEGATIVE_SPACE_GLYPHS[i]);
                remaining -= SPACE_VALUES[i];
            }
        }
        return sb.toString();
    }

    /**
     * Decomposes a positive pixel offset into binary combinations of positive-space font glyphs.
     *
     * @param pixels the number of pixels to move right (cursor advance)
     * @return the string sequence of positive space characters
     */
    @NotNull
    public static String getPositiveSpace(int pixels) {
        if (pixels <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int remaining = pixels;
        for (int i = 0; i < SPACE_VALUES.length; i++) {
            while (remaining >= SPACE_VALUES[i]) {
                sb.append(POSITIVE_SPACE_GLYPHS[i]);
                remaining -= SPACE_VALUES[i];
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
     * Convenience method to construct a tooltip box with default crosshair offset around the given lines.
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
     * Convenience method to construct a tooltip box with default crosshair offset around the given lines.
     *
     * @param lines the lines of text content
     * @return the constructed component
     */
    @NotNull
    public static Component of(@Nullable List<Component> lines) {
        return builder().lines(lines).build();
    }

    /**
     * Builder for constructing 3-part tooltip boxes.
     */
    public static final class Builder {
        private final List<Component> lines = new ArrayList<>();
        private int crosshairOffsetX = DEFAULT_CROSSHAIR_OFFSET;

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
         * Configures the horizontal offset (in pixels) from the screen center / crosshair.
         *
         * @param offsetX horizontal pixels to shift to the right of the crosshair (>= 0)
         * @return this builder
         */
        @NotNull
        public Builder crosshairOffsetX(int offsetX) {
            this.crosshairOffsetX = Math.max(0, offsetX);
            return this;
        }

        /**
         * Builds the Adventure {@link Component} containing the positive centering shift,
         * 3-part container box, negative text-alignment shift, and content.
         *
         * @return the built tooltip box component
         */
        @NotNull
        public Component build() {
            Component content = this.lines.isEmpty() ? Component.empty() : this.lines.get(0);
            int textWidth = Math.max(MIN_CONTENT_WIDTH, FontWidthHelper.getWidth(content));

            // Total box width = left cap (5px) + middle repeats (textWidth px) + right cap (5px)
            int boxWidth = CAP_WIDTH + textWidth + CAP_WIDTH;

            // In Minecraft subtitle/title (centered on screen), shifting the box so its left edge starts
            // at (center + crosshairOffsetX) requires prepending positive space S = boxWidth + 2 * crosshairOffsetX.
            int leadingSpace = boxWidth + (2 * this.crosshairOffsetX);
            String leadingSpaceStr = getPositiveSpace(leadingSpace);

            // Minecraft font renderer adds +1px font spacing after every bitmap character.
            // Appending \uF801 (-1px) after each glyph ensures exact 1px step per middle tile
            // (eliminating vertical striped gaps) and exact 5px step for the caps.
            String step = MIDDLE + "\uF801";
            String boxStr = CAP_LEFT + "\uF801" + step.repeat(textWidth) + CAP_RIGHT + "\uF801";

            // Shift back cursor from right edge of right cap to start of text:
            // Text starts at left cap width (5px) from the left edge of the box.
            // Total box width is boxWidth (= textWidth + 10).
            // Shift back = boxWidth - CAP_WIDTH = textWidth + 5.
            int textShiftBack = textWidth + CAP_WIDTH;
            String textShiftBackStr = getNegativeSpace(textShiftBack);

            TextComponent.Builder root = Component.text();

            // 1. Initial offset to place the box right of the crosshair
            if (!leadingSpaceStr.isEmpty()) {
                root.append(Component.text(leadingSpaceStr).font(FONT));
            }

            // 2. The 3-part container box (with -1px spacer per tile for seamless fill and exact pixel width)
            root.append(Component.text(boxStr, NamedTextColor.WHITE).font(FONT));

            // 3. Shift cursor back to inner text start
            root.append(Component.text(textShiftBackStr).font(FONT));

            // 4. The actual note text (renders on top of the dark container)
            root.append(content);

            // 5. Compensate final cursor to reach the full boxWidth for exact centering
            root.append(Component.text(getPositiveSpace(CAP_WIDTH)).font(FONT));

            return root.build();
        }
    }
}
