package net.onelitefeather.cygnus.common.icon;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
//import net.onelitefeather.cygnus.common.rank.RankTag;

/**
 * Represents custom game, role, and UI icons (Ghost, Flashlight, Pentagram, Clock, Page, Map, Builder)
 * rendered using the {@code cygnus:icons} font.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.8.0
 */
public enum GameIcon {
    GHOST("󰀀"),
    PENTAGRAM("󰀅"),
    CLOCK("󰀆"),
    PAGE("󰀇"),
    FLASHLIGHT("󰀊"),
    MAP("󰀋"),
    BUILDER("󰀌");

    /**
     * The font key for game and UI icons defined in the resource pack.
     */
    public static final Key FONT = Key.key("cygnus", "icons");

    // Standard role and UI mappings
    public static final GameIcon SPECTATOR = GHOST;
    public static final GameIcon SURVIVOR = FLASHLIGHT;
    public static final GameIcon SLENDER = PENTAGRAM;
    public static final GameIcon TIME = CLOCK;

    private final String glyph;

    GameIcon(String glyph) {
        this.glyph = glyph;
    }

    /**
     * Returns the unicode glyph representing this icon.
     *
     * @return the glyph string
     */
    public String glyph() {
        return this.glyph;
    }

    /**
     * Returns the pre-built {@link Component} with the custom icon font.
     *
     * @return the component
     */
    public Component asComponent() {
        return Component.text(this.glyph).font(FONT);
    }

    /**
     * Formats a component by prepending this icon and resetting the font for subsequent text.
     *
     * @param trailing the trailing component to append after the icon
     * @return the combined component with default font reset
     */
    public Component format(Component trailing) {
        return Component.empty()
                .append(asComponent())
                //.append(Component.space().font(RankTag.DEFAULT_MINECRAFT_FONT))
                .append(trailing);
    }

    /**
     * Formats a text string by prepending this icon and resetting the font for subsequent text.
     *
     * @param text the text to append after the icon
     * @return the combined component with default font reset
     */
    public Component format(String text) {
        return Component.empty();
       /// return format(Component.text(text).font(RankTag.DEFAULT_MINECRAFT_FONT));
    }

    /**
     * Formats multiple icons together, followed by the trailing component with default font reset.
     *
     * @param trailing the component to append after the icons
     * @param icons    the icons to prepend
     * @return the combined component with default font reset
     */
    public static Component formatMultiple(Component trailing, GameIcon... icons) {
        Component result = Component.empty();
        for (GameIcon icon : icons) {
            result = result.append(icon.asComponent());
                 //   .append(Component.space().font(RankTag.DEFAULT_MINECRAFT_FONT));
        }
        return result.append(trailing);
    }
}
