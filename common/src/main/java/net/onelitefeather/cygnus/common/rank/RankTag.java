package net.onelitefeather.cygnus.common.rank;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.Objects;

/**
 * Represents a graphical rank tag rendered with a custom font glyph.
 *
 * @param id          the unique identifier of the rank tag
 * @param glyph       the character for the tag bitmap
 * @param font        the resource pack font key
 * @param priority    the priority used for resolution (higher priority takes precedence)
 * @param asComponent the pre-built {@link Component} for rendering
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.8.0
 */
public record RankTag(
        String id,
        String glyph,
        Key font,
        int priority,
        Component asComponent
) implements Comparable<RankTag> {

    /**
     * The default font key for player rank tags defined in the resource pack.
     */
    public static final Key DEFAULT_FONT = Key.key("olf", "rank_tags");

    public static final RankTag ADMINISTRATOR = new RankTag("administrator", "󰆐", DEFAULT_FONT, 100);
    public static final RankTag ASSISTANT = new RankTag("assistent", "󲆛", DEFAULT_FONT, 90);
    public static final RankTag MOD = new RankTag("mod", "󱆛", DEFAULT_FONT, 80);
    public static final RankTag CONTENT = new RankTag("content", "󲆐", DEFAULT_FONT, 70);
    public static final RankTag MEDIA = new RankTag("media", "󲆕", DEFAULT_FONT, 60);
    public static final RankTag LITE = new RankTag("lite", "󲆑", DEFAULT_FONT, 50);
    public static final RankTag PLAYER = new RankTag("player", "󱆖", DEFAULT_FONT, 0);

    /**
     * Creates a new rank tag and builds the cached {@link Component}.
     *
     * @param id       the unique identifier of the rank tag
     * @param glyph    the unicode character for the tag bitmap
     * @param font     the resource pack font key
     * @param priority the priority used for resolution
     */
    public RankTag(String id, String glyph, Key font, int priority) {
        this(
                Objects.requireNonNull(id, "id must not be null"),
                Objects.requireNonNull(glyph, "glyph must not be null"),
                Objects.requireNonNull(font, "font must not be null"),
                priority,
                Component.text(glyph).font(font)
        );
    }

    /**
     * Creates a new rank tag using {@link #DEFAULT_FONT}.
     *
     * @param id       the unique identifier of the rank tag
     * @param glyph    the unicode character for the tag bitmap
     * @param priority the priority used for resolution
     */
    public RankTag(String id, String glyph, int priority) {
        this(id, glyph, DEFAULT_FONT, priority);
    }

    @Override
    public int compareTo(RankTag other) {
        return Integer.compare(other.priority, this.priority);
    }
}
