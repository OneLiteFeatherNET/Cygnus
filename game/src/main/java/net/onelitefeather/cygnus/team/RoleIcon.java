package net.onelitefeather.cygnus.team;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onelitefeather.cygnus.common.config.GameConfig;

/**
 * Maps each game role to the tab list icon the {@code cygnus:icons} resource pack font provides.
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
public enum RoleIcon {

    SLENDER(GameConfig.SLENDER_KEY, 0xF0005),
    SURVIVOR(GameConfig.SURVIVOR_KEY, 0xF000A),
    SPECTATOR(GameConfig.SPECTATOR_KEY, 0xF0000);

    private final Key roleKey;
    private final Component glyph;

    RoleIcon(Key roleKey, int codepoint) {
        this.roleKey = roleKey;
        this.glyph = Component.text(new String(Character.toChars(codepoint)), NamedTextColor.WHITE).font(iconFont());
    }

    /**
     * Returns the {@code cygnus:icons} resource pack font every {@link #glyph()} is drawn from.
     * <p>
     * A method rather than a static field: enum constants are initialized before the class's other
     * static fields, so a static field here would not yet be set while the constants above are built.
     * </p>
     *
     * @return the font key
     */
    private static Key iconFont() {
        return Key.key("cygnus", "icons");
    }

    /**
     * Returns the {@link GameConfig} role key this icon represents.
     *
     * @return the role key
     */
    public Key roleKey() {
        return roleKey;
    }

    /**
     * Returns the styled glyph component for this role.
     *
     * @return the icon component
     */
    public Component glyph() {
        return glyph;
    }

    /**
     * Prepends this role's icon and a space in front of the given name.
     * <p>
     * The icon has to be appended as a child of a plain, font-less root rather than used as the root
     * itself: Adventure components inherit style from their parent, so a root carrying
     * {@code font(cygnus:icons)} would leak that font onto the space and name appended after it,
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
}
