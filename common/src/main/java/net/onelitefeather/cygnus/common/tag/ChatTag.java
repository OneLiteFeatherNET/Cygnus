package net.onelitefeather.cygnus.common.tag;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;

/**
 * Maps each chat message context to its badge icon in the {@code cygnus:tags} resource pack font.
 * <p>
 * The glyph is rendered {@link NamedTextColor#WHITE} because the pack's badges are full-color bitmaps
 * rather than the grayscale masks vanilla glyphs use - any other color would tint the artwork instead
 * of leaving it as designed. The client's default drop shadow is disabled for the same reason: the
 * badge already bakes its own shadow into the artwork, and the client's shadow would double up on it.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ChatTag {

    SLENDER(0xF0020),
    MAP(0xF0021);

    private final Component glyph;

    ChatTag(int codepoint) {
        this.glyph = Component.text(new String(Character.toChars(codepoint)), NamedTextColor.WHITE)
                .font(tagFont())
                .shadowColor(ShadowColor.none());
    }

    /**
     * Returns the {@code cygnus:tags} resource pack font every {@link #glyph()} is drawn from.
     * <p>
     * A method rather than a static field: enum constants are initialized before the class's other
     * static fields, so a static field here would not yet be set while the constants above are built.
     * </p>
     *
     * @return the font key
     */
    private static Key tagFont() {
        return Key.key("cygnus", "tags");
    }

    /**
     * Returns the styled glyph component for this tag.
     *
     * @return the badge component
     */
    public Component glyph() {
        return glyph;
    }

    /**
     * Prepends this tag's badge and a space in front of the given message.
     * <p>
     * The badge has to be appended as a child of a plain, font-less root rather than used as the root
     * itself: Adventure components inherit style from their parent, so a root carrying
     * {@code font(cygnus:tags)} would leak that font onto the space and message appended after it,
     * which has no letter glyphs and renders them as missing-character boxes in the client.
     * </p>
     *
     * @param message the message component to prefix
     * @return the badge followed by a space and the given message
     */
    public Component prefix(Component message) {
        return Component.text()
                .append(glyph)
                .appendSpace()
                .append(message)
                .build();
    }
}
