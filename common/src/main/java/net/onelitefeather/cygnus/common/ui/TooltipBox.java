package net.onelitefeather.cygnus.common.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs an authentic Adventure {@link Component} that renders a modular 3-part tooltip frame
 * and background from the {@code cygnus:tooltip} font family with cursor shifts, inline bookmark ribbon,
 * and crosshair alignment.
 *
 * @author theEvilReaper
 * @version 3.0.0
 * @since 2.15.0
 */
public final class TooltipBox {

    /**
     * Font keys for tooltip frame, text lines, and spacing glyphs.
     */
    public static final Key FONT = Key.key("cygnus", "tooltip");
    public static final Key TOOLTIP_FONT = FONT;
    public static final Key FONT_LINE1 = FONT;
    public static final Key FONT_LINE2 = Key.key("cygnus", "tooltip_line2");
    public static final Key FONT_LINE3 = Key.key("cygnus", "tooltip_line3");

    /**
     * The most lines a box has room for. The pack draws one box per line count, up to three.
     */
    public static final int MAX_LINES = 3;

    // Full-height box glyphs, one set per line count: left cap, 1px middle tile, right cap.
    // Each set comes with the ribbon that sits on its first line.
    public static final String BOX_1_LEFT = "\uE100";
    public static final String BOX_1_MIDDLE = "\uE101";
    public static final String BOX_1_RIGHT = "\uE102";
    public static final String RIBBON_1 = "\uE103";

    public static final String BOX_2_LEFT = "\uE104";
    public static final String BOX_2_MIDDLE = "\uE105";
    public static final String BOX_2_RIGHT = "\uE106";
    public static final String RIBBON_2 = "\uE10A";

    public static final String BOX_3_LEFT = "\uE107";
    public static final String BOX_3_MIDDLE = "\uE108";
    public static final String BOX_3_RIGHT = "\uE109";
    public static final String RIBBON_3 = "\uE10B";

    private static final String[][] BOX_GLYPHS = {
            {BOX_1_LEFT, BOX_1_MIDDLE, BOX_1_RIGHT, RIBBON_1},
            {BOX_2_LEFT, BOX_2_MIDDLE, BOX_2_RIGHT, RIBBON_2},
            {BOX_3_LEFT, BOX_3_MIDDLE, BOX_3_RIGHT, RIBBON_3},
    };

    /**
     * Pulls the cursor back by one pixel. Every glyph advances one pixel more than it is wide, so
     * box glyphs are each followed by this to sit flush against the next.
     */
    private static final String PULL_BACK = "\uF001";

    private static final Logger LOGGER = LoggerFactory.getLogger(TooltipBox.class);

    // Geometry constants
    public static final int CAP_WIDTH = 5;
    public static final int RIBBON_WIDTH = 4;
    public static final int RIBBON_GAP = 3;
    public static final int INNER_LEFT_PADDING = 3;
    public static final int INNER_RIGHT_PADDING = 2;
    public static final int TEXT_LEFT_INDENT = INNER_LEFT_PADDING + RIBBON_WIDTH + RIBBON_GAP; // 10px

    public static final int HORIZONTAL_PADDING = 4;
    public static final int MIN_CONTENT_WIDTH = 10;
    public static final int DEFAULT_CROSSHAIR_OFFSET = 16;

    private TooltipBox() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns the font key associated with the given 0-indexed line number.
     *
     * @param lineIndex 0-indexed line number (0 = line 1, 1 = line 2, 2+ = line 3)
     * @return font Key
     */
    public static Key getFontForLine(int lineIndex) {
        return switch (lineIndex) {
            case 0 -> FONT_LINE1;
            case 1 -> FONT_LINE2;
            default -> FONT_LINE3;
        };
    }

    /**
     * Decomposes a positive pixel offset into combinations of negative-space font glyphs
     * using the {@code space:default} font.
     *
     * @param pixels the number of pixels to move left (cursor shift)
     * @return the string sequence of negative space characters
     */
    @NotNull
    public static String getNegativeSpace(int pixels) {
        return SpaceHelper.getNegative(pixels);
    }

    /**
     * Decomposes a positive pixel offset into combinations of positive-space font glyphs
     * using the {@code space:default} font.
     *
     * @param pixels the number of pixels to move right (cursor advance)
     * @return the string sequence of positive space characters
     */
    @NotNull
    public static String getPositiveSpace(int pixels) {
        return SpaceHelper.getPositive(pixels);
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
     * Builder for constructing modular tooltip boxes with inline ribbon.
     */
    public static final class Builder {
        private final List<Component> lines = new ArrayList<>();
        private int crosshairOffsetX = DEFAULT_CROSSHAIR_OFFSET;
        private boolean centered;

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
         * Centers the box on the anchor of whatever renders it, instead of placing it beside the
         * crosshair. Meant for text displays in the world: they center each line on their own
         * position, so any leading space would push the box off the page. The crosshair offset
         * has no effect once this is set.
         *
         * @return this builder
         */
        @NotNull
        public Builder centered() {
            this.centered = true;
            return this;
        }

        /**
         * Builds the Adventure {@link Component} containing the positive centering shift,
         * modular 3-band container box, inline ribbon, negative text-alignment shift, and content.
         *
         * @return the built tooltip box component
         */
        @NotNull
        public Component build() {
            // Flatten input components into individual text lines by splitting on '\n'
            List<String> rawLines = new ArrayList<>();
            for (Component lineComp : this.lines) {
                extractLines(lineComp, rawLines);
            }
            if (rawLines.isEmpty()) {
                rawLines.add("");
            }
            if (rawLines.size() > MAX_LINES) {
                LOGGER.warn("Tooltip has {} lines, but a box holds at most {}; dropping the rest: {}",
                        rawLines.size(), MAX_LINES, rawLines.subList(MAX_LINES, rawLines.size()));
                rawLines = new ArrayList<>(rawLines.subList(0, MAX_LINES));
            }

            int numLines = rawLines.size();
            int maxTextLineWidth = 0;
            int[] lineWidths = new int[numLines];
            for (int i = 0; i < numLines; i++) {
                lineWidths[i] = FontWidthHelper.getTooltipWidth(rawLines.get(i));
                maxTextLineWidth = Math.max(maxTextLineWidth, lineWidths[i]);
            }

            int contentWidth = TEXT_LEFT_INDENT + maxTextLineWidth;
            int middleTiles = Math.max(MIN_CONTENT_WIDTH, contentWidth + INNER_RIGHT_PADDING);
            int boxWidth = CAP_WIDTH + middleTiles + CAP_WIDTH;

            // Step 1: Leading space for crosshair offset. A centered box has none: its total
            // advance is exactly boxWidth, so it sits centered on the renderer's anchor.
            TextComponent.Builder root = Component.text().font(FONT);
            if (!this.centered) {
                String leadingSpaceStr = getPositiveSpace(boxWidth + (2 * this.crosshairOffsetX));
                if (!leadingSpaceStr.isEmpty()) {
                    root.append(Component.text(leadingSpaceStr).font(FONT));
                }
            }

            // Step 2: The whole box in one run, drawn in the box set for this many lines.
            String[] glyphs = BOX_GLYPHS[numLines - 1];
            String step = glyphs[1] + PULL_BACK;
            String boxStr = glyphs[0] + PULL_BACK + step.repeat(middleTiles) + glyphs[2] + PULL_BACK;
            root.append(Component.text(boxStr, NamedTextColor.WHITE).font(FONT));

            // Step 3: Ribbon & Text
            // Cursor is currently at the right edge of the box (boxWidth from box left edge).
            // The ribbon starts at INNER_LEFT_PADDING + CAP_WIDTH = 3 + 5 = 8px from box left edge.
            int shiftToRibbon = boxWidth - (CAP_WIDTH + INNER_LEFT_PADDING);
            root.append(Component.text(getNegativeSpace(shiftToRibbon)).font(FONT));

            // Draw Ribbon (4px wide + 1px font advance = 5px), pulled back to an exact 4px advance
            root.append(Component.text(glyphs[3] + PULL_BACK, NamedTextColor.WHITE).font(FONT));

            // Gap between ribbon and text (3px)
            root.append(Component.text(getPositiveSpace(RIBBON_GAP)).font(FONT));

            // Draw Line 1 Text
            root.append(Component.text(rawLines.get(0)).font(FONT_LINE1));

            // Draw subsequent lines (Line 2+)
            for (int i = 1; i < numLines; i++) {
                int prevLineWidth = lineWidths[i - 1];
                // Cursor is at end of line (i-1).
                // Shift back: prevLineWidth + TEXT_LEFT_INDENT to return to box left + CAP_WIDTH.
                // Then advance by TEXT_LEFT_INDENT to align with text start of line 1.
                // Net cursor shift: -prevLineWidth!
                root.append(Component.text(getNegativeSpace(prevLineWidth)).font(FONT));
                Key lineFont = getFontForLine(i);
                root.append(Component.text(rawLines.get(i)).font(lineFont));
            }

            // Step 4: Compensate final cursor to reach full boxWidth for exact centering
            int lastLineWidth = lineWidths[numLines - 1];
            int currentCursorFromBoxLeft = (numLines == 1)
                    ? (CAP_WIDTH + INNER_LEFT_PADDING + RIBBON_WIDTH + RIBBON_GAP + lastLineWidth)
                    : (CAP_WIDTH + TEXT_LEFT_INDENT + lastLineWidth);
            int remainingSpace = boxWidth - currentCursorFromBoxLeft;
            if (remainingSpace > 0) {
                root.append(Component.text(getPositiveSpace(remainingSpace)).font(FONT));
            }

            return root.build();
        }

        private static void extractLines(Component component, List<String> lines) {
            if (component instanceof TextComponent textComp) {
                String content = textComp.content();
                if (!content.isEmpty()) {
                    String[] parts = content.split("\n", -1);
                    for (int i = 0; i < parts.length; i++) {
                        if (i == 0) {
                            if (lines.isEmpty()) {
                                lines.add(parts[0]);
                            } else {
                                int lastIdx = lines.size() - 1;
                                lines.set(lastIdx, lines.get(lastIdx) + parts[0]);
                            }
                        } else {
                            lines.add(parts[i]);
                        }
                    }
                }
            }
            for (Component child : component.children()) {
                extractLines(child, lines);
            }
        }
    }
}
