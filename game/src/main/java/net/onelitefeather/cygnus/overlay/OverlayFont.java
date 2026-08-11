package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;

/**
 * The bitmap font from {@code cygnus-pack} that carries every full-screen overlay glyph, and the
 * negative spacer that lets two of them sit on top of each other.
 * <p>
 * One font for all layers rather than one per effect: a component can only carry a single font,
 * and stacking layers means putting them into the same component.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class OverlayFont {

    /** The font all overlay glyphs live in. */
    public static final Key KEY = Key.key("cygnus", "overlay");

    /**
     * A {@code space} provider with a negative advance, wide enough to walk back over a full
     * overlay glyph so the next layer starts at the same place.
     */
    static final int SPACER_CODE_POINT = 0xE0FF;

    private OverlayFont() {
    }

    /**
     * Builds the component for a glyph of this font.
     * <p>
     * The shadow is switched off explicitly: with it, Minecraft renders the whole overlay a second
     * time, offset by a pixel, underneath itself.
     * </p>
     *
     * @param codePoint the code point of the glyph
     * @return the component to send
     */
    public static Component glyph(int codePoint) {
        return Component.text(new String(Character.toChars(codePoint)))
                .font(KEY)
                .shadowColor(ShadowColor.none());
    }

    /**
     * The spacer that moves the cursor back to the start of the previous glyph.
     *
     * @return the spacer component
     */
    static Component spacer() {
        return glyph(SPACER_CODE_POINT);
    }
}
