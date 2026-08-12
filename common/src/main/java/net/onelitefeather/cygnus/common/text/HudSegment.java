package net.onelitefeather.cygnus.common.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Contract;

/**
 * Builds one reusable "icon + background-wrapped text" HUD segment: a fixed
 * pixel offset reserved for an icon glyph (not yet assigned - deferred to a
 * later iteration), followed by {@link BackgroundBar#wrap(Component, int, TextColor)}.
 */
public final class HudSegment {

    private HudSegment() {}

    /**
     * Builds a segment consisting of an icon-width pixel offset followed by
     * {@code text} wrapped in a background bar.
     * <p>
     * The {@code tint} parameter doubles as the shader marker color: a
     * near-white, per-component-unique {@link TextColor} that a resource-pack
     * shader keys off to position the bar on screen.
     *
     * @param text        the component to render on top of the bar
     * @param iconWidthPx pixels reserved for an icon glyph before the bar
     * @param paddingPx   pixels of padding to add on each side of {@code text}
     * @param tint        the color applied to the bar's glyphs and used as the
     *                    shader marker color
     * @return the composed segment component
     */
    @Contract(pure = true)
    public static Component segment(Component text, int iconWidthPx, int paddingPx, TextColor tint) {
        // BackgroundBar.wrap(...) returns a Component.empty()-rooted tree that never sets its own
        // font, so nesting it as a child of the icon-offset component below would otherwise leak
        // that component's space:default font down onto `text` (Adventure/Minecraft resolve an
        // unset font from the nearest explicitly-styled ancestor). Pinning the wrap() result's own
        // font back to the default keeps `text` rendered in its intended font regardless of what
        // this segment is later nested under.
        Component bar = BackgroundBar.wrap(text, paddingPx, tint).font(Style.DEFAULT_FONT);
        return SpaceFont.positive(iconWidthPx).append(bar);
    }
}
