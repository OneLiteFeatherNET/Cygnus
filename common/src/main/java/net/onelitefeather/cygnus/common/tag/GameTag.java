package net.onelitefeather.cygnus.common.tag;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
//import net.onelitefeather.cygnus.common.rank.RankTag;

/**
 * Represents custom game and system badges/tags (e.g. Slender Powerline NameTag)
 * rendered using the {@code cygnus:tags} font.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.8.0
 */
public enum GameTag {
    SLENDER("\uDB80\uDC20"),
    MAP_INFO("\uDB80\uDC21");

    /**
     * The font key for game tags defined in the resource pack.
     */
    public static final Key FONT = Key.key("cygnus", "tags");

    private final String glyph;

    GameTag(String glyph) {
        this.glyph = glyph;
    }

    /**
     * Returns the unicode glyph representing this game tag.
     *
     * @return the glyph string
     */
    public String glyph() {
        return this.glyph;
    }

    /**
     * Returns the pre-built {@link Component} with the custom tag font.
     *
     * @return the component
     */
    public Component asComponent() {
        return Component.text(this.glyph).font(FONT);
    }

    /**
     * Returns the pre-built prefix component with font reset.
     *
     * @return the prefix component
     */
    public Component asPrefix() {
        return Component.empty()
                .append(asComponent())
                .append(Component.space());
    }

    /**
     * Formats a component by prepending this tag and resetting the font for subsequent text.
     *
     * @param trailing the trailing component to append after the prefix
     * @return the combined component with default font reset
     */
    public Component format(Component trailing) {
        return Component.empty()
                .append(asComponent())
               // .append(Component.space().font(RankTag.DEFAULT_MINECRAFT_FONT))
                .append(trailing);
    }

    /**
     * Formats a text string by prepending this tag and resetting the font for subsequent text.
     *
     * @param text the text to append after the prefix
     * @return the combined component with default font reset
     */
    public Component format(String text) {
        return Component.empty();
        //return format(Component.text(text).font(RankTag.DEFAULT_MINECRAFT_FONT));
    }
}
