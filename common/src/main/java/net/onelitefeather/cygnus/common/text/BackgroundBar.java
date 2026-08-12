package net.onelitefeather.cygnus.common.text;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Contract;

public final class BackgroundBar {

    private static final Key FONT_KEY = Key.key("cygnus", "misc");

    private static final char GLYPH_1   = '\ue001';
    private static final char GLYPH_2   = '\ue002';
    private static final char GLYPH_4   = '\ue004';
    private static final char GLYPH_8   = '\ue008';
    private static final char GLYPH_16  = '\ue010';
    private static final char GLYPH_32  = '\ue020';
    private static final char GLYPH_64  = '\ue040';
    private static final char GLYPH_128 = '\ue080';
    private static final char GLYPH_END = '\ue100';

    private BackgroundBar() {}

    /**
     * Wraps {@code text} in a dynamically-sized background bar built from
     * {@code manis:misc} font glyphs, sized to exactly cover the pixel width of
     * {@code text} plus {@code paddingPx} pixels of padding on each side.
     * <p>
     * Every {@code manis:misc} glyph is a {@code type: "bitmap"} provider, and per
     * Minecraft's bitmap-provider advance formula every such glyph's real rendered
     * advance is {@code actualOpaqueWidth + 1}, one pixel wider than its nominal
     * width. To keep the bar visually solid (no transparent seams) and its total
     * rendered advance exactly equal to the computed {@code barWidth}, each glyph
     * emitted by {@link #buildBarGlyphs(int)} is immediately followed by a 1px
     * {@code space:default} rewind ({@link SpaceFont#negative(int)}) that cancels
     * out that glyph's extra pixel.
     *
     * The returned component's own rendered advance (as measured by {@link
     * TextWidth#widthOf(Component)}) is exactly {@code barWidth}, i.e. the same
     * padding is trailed after {@code text} as was inserted before it. This lets
     * callers {@code .append(...)} further, un-barred content (e.g. an icon or a
     * URL) directly onto the result and have it start flush after the bar's right
     * edge instead of overlapping the bar's trailing padding.
     *
     * @param text      the component to render on top of the bar; only its own
     *                  style is used, this method never mutates or restyles it
     * @param paddingPx pixels of padding to add on each side of {@code text}'s
     *                  measured width; must be {@code >= 0}
     * @param tint      the color applied to the bar's {@code manis:misc} glyphs
     * @return {@code text} wrapped with a background bar, or {@code text}
     * unchanged if its measured pixel width is {@code <= 0} (e.g. it is empty or
     * only contains non-{@link net.kyori.adventure.text.TextComponent} content)
     * @throws IllegalArgumentException if {@code paddingPx} is negative
     */
    @Contract(pure = true)
    public static Component wrap(Component text, int paddingPx, TextColor tint) {
        if (paddingPx < 0) throw new IllegalArgumentException("paddingPx must be >= 0, got " + paddingPx);

        int textWidth = TextWidth.widthOf(text);
        if (textWidth <= 0) return text;

        int barWidth = 2 * paddingPx + textWidth;

        return Component.empty()
                .append(buildBar(barWidth, tint))
                .append(SpaceFont.negative(barWidth))
                .append(SpaceFont.positive(paddingPx))
                .append(text)
                .append(SpaceFont.positive(paddingPx));
    }

    /**
     * Builds the renderable bar segment: a {@code manis:misc}-styled component
     * whose total rendered pixel advance (as measured by {@link
     * TextWidth#widthOf(Component)}) is exactly {@code barWidth}.
     * <p>
     * The cap glyph ({@code end.png}) is only tapered on its top/bottom row, not
     * left/right, so it reads as a rounded corner regardless of which end it sits
     * on. For {@code barWidth >= 2} one cap is placed at the start and one at the
     * end, with {@code buildBarGlyphs} decomposing the remaining {@code (barWidth
     * - 2)} px of fill into power-of-two glyphs between them (start cap's 1px +
     * decomposition sum + end cap's 1px, embedded as {@code buildBarGlyphs}'s own
     * trailing glyph, == {@code barWidth}). For {@code barWidth < 2} there isn't
     * room for two caps, so only the trailing end cap from {@code
     * buildBarGlyphs(barWidth - 1)} is emitted, same as before this method
     * supported a start cap. Every {@code manis:misc} glyph's real rendered
     * advance is {@code nominalWidth + 1} px (see the bitmap-provider note on
     * {@link #wrap(Component, int, TextColor)}), so each glyph is immediately
     * followed by a 1px {@code space:default} rewind ({@link
     * SpaceFont#negative(int)}) that cancels the extra pixel, keeping the net
     * advance per glyph equal to its nominal width.
     * <p>
     * The bar's {@link ShadowColor} is explicitly disabled: vanilla's default
     * text shadow would render a visibly offset, darkened duplicate of each
     * bitmap glyph, breaking the seamless bar look.
     *
     * @param barWidth the intended total rendered pixel width of the bar
     * @param tint     the color applied to the bar's glyphs
     * @return the bar segment component
     */
    @Contract(pure = true)
    static Component buildBar(int barWidth, TextColor tint) {
        Component bar = Component.empty().font(FONT_KEY).color(tint).shadowColor(ShadowColor.none());

        if (barWidth >= 2) {
            bar = bar.append(Component.text(String.valueOf(GLYPH_END)))
                    .append(SpaceFont.negative(1));
        }

        int fillWidth = barWidth >= 2 ? barWidth - 2 : barWidth - 1;
        for (char glyph : buildBarGlyphs(fillWidth).toCharArray()) {
            bar = bar.append(Component.text(String.valueOf(glyph)))
                    .append(SpaceFont.negative(1));
        }
        return bar;
    }

    static String buildBarGlyphs(int width) {
        StringBuilder sb = new StringBuilder();
        int remaining = width;

        while (remaining >= 128) {
            sb.append(GLYPH_128);
            remaining -= 128;
        }
        if (remaining >= 64) { sb.append(GLYPH_64); remaining -= 64; }
        if (remaining >= 32) { sb.append(GLYPH_32); remaining -= 32; }
        if (remaining >= 16) { sb.append(GLYPH_16); remaining -= 16; }
        if (remaining >= 8)  { sb.append(GLYPH_8);  remaining -= 8; }
        if (remaining >= 4)  { sb.append(GLYPH_4);  remaining -= 4; }
        if (remaining >= 2)  { sb.append(GLYPH_2);  remaining -= 2; }
        if (remaining >= 1)  { sb.append(GLYPH_1);  remaining -= 1; }

        sb.append(GLYPH_END);
        return sb.toString();
    }
}
