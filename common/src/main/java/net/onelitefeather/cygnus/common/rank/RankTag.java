package net.onelitefeather.cygnus.common.rank;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;
import java.util.Optional;

/**
 * Maps each LuckPerms rank to its name tag icon in the {@code olf:rank_tags} resource pack font.
 * <p>
 * The glyph is rendered {@link NamedTextColor#WHITE} because the pack's icons are full-color bitmaps
 * rather than the grayscale masks vanilla glyphs use - any other color would tint the artwork instead
 * of leaving it as designed.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public enum RankTag {

    ADMINISTRATOR(0xF0190),
    ASSISTENT(0xF219B),
    MOD(0xF119B),
    CONTENT(0xF2190),
    MEDIA(0xF2195),
    LITE(0xF2191),
    PLAYER(0xF1196);

    private final Component glyph;

    RankTag(int codepoint) {
        this.glyph = Component.text(new String(Character.toChars(codepoint)), NamedTextColor.WHITE).font(rankFont());
    }

    /**
     * Returns the {@code olf:rank_tags} resource pack font every {@link #glyph()} is drawn from.
     * <p>
     * A method rather than a static field: enum constants are initialized before the class's other
     * static fields, so a static field here would not yet be set while the constants above are built.
     * </p>
     *
     * @return the font key
     */
    private static Key rankFont() {
        return Key.key("olf", "rank_tags");
    }

    /**
     * Returns the styled glyph component for this rank.
     *
     * @return the icon component
     */
    public Component glyph() {
        return glyph;
    }

    /**
     * Prepends this rank's icon and a space in front of the given name.
     * <p>
     * The icon has to be appended as a child of a plain, font-less root rather than used as the root
     * itself: Adventure components inherit style from their parent, so a root carrying
     * {@code font(olf:rank_tags)} would leak that font onto the space and name appended after it,
     * which has no letter glyphs and renders them as missing-character boxes in the client.
     * </p>
     *
     * @param name the name component to prefix
     * @return the icon followed by a space and the given name
     */
    public Component prefix(Component name) {
        return Component.text()
                .append(glyph)
                .appendSpace()
                .append(name)
                .build();
    }

    /**
     * Looks up the rank tag whose name matches a LuckPerms group id (e.g. {@code "administrator"}),
     * case-insensitively.
     *
     * @param group the LuckPerms group id, or {@code null}
     * @return the matching tag, or empty when the group is {@code null} or names no known rank
     */
    public static Optional<RankTag> fromGroup(String group) {
        if (group == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(RankTag.valueOf(group.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
