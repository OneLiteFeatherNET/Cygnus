package net.onelitefeather.cygnus.common.text;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Contract;

/**
 * Builds one reusable "icon + background-wrapped text" HUD segment: an icon
 * glyph from the {@code cygnus:misc} font, followed by
 * {@link BackgroundBar#wrap(Component, int, TextColor)}.
 */
public final class HudSegment {

    private static final Key FONT_KEY = Key.key("cygnus", "misc");

    private HudSegment() {}

    /**
     * Builds a segment consisting of an icon glyph followed by {@code text}
     * wrapped in a background bar.
     * <p>
     * The {@code tint} parameter doubles as the shader marker color: a
     * near-white, per-component-unique {@link TextColor} that a resource-pack
     * shader keys off to position the bar on screen. It is applied to both
     * the icon glyph and the bar.
     *
     * @param icon      the {@code cygnus:misc} icon glyph rendered before the bar
     * @param text      the component to render on top of the bar
     * @param paddingPx pixels of padding to add on each side of {@code text}
     * @param tint      the color applied to the icon and the bar's glyphs,
     *                  and used as the shader marker color
     * @return the composed segment component
     */
    @Contract(pure = true)
    public static Component segment(char icon, Component text, int paddingPx, TextColor tint) {
        Component iconGlyph = Component.text(String.valueOf(icon))
                .font(FONT_KEY).color(tint).shadowColor(ShadowColor.none());
        // BackgroundBar.wrap(...) returns a Component.empty()-rooted tree that never sets its own
        // font, so nesting it as a child of the icon component above would otherwise leak that
        // component's cygnus:misc font down onto `text` (Adventure/Minecraft resolve an unset font
        // from the nearest explicitly-styled ancestor). Pinning the wrap() result's own font back to
        // the default keeps `text` rendered in its intended font regardless of what this segment is
        // later nested under.
        Component bar = BackgroundBar.wrap(text, paddingPx, tint).font(Style.DEFAULT_FONT);
        return iconGlyph.append(bar);
    }
}
