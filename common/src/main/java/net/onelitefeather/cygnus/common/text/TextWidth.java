package net.onelitefeather.cygnus.common.text;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TextWidth {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextWidth.class);
    private static final Key DEFAULT_FONT = Key.key("minecraft", "default");
    private static final int FALLBACK_WIDTH = 6;

    private static final Map<String, Map<Integer, Integer>> WIDTHS = load();
    private static final Set<Integer> WARNED_CODEPOINTS = ConcurrentHashMap.newKeySet();

    private TextWidth() {}

    /**
     * Measures the pixel width of {@code component}, recursing into its children
     * and resolving each child's font from its own explicit {@link
     * net.kyori.adventure.text.format.Style#font()} or, if unset, from the
     * nearest ancestor's font (defaulting to {@code minecraft:default} at the
     * root).
     * <p>
     * Only {@link TextComponent} content is measured, character by character,
     * against the font-widths table loaded from {@code /font/font-widths.json}.
     * Other component types (e.g. {@code TranslatableComponent}) contribute
     * {@code 0} width for their own content, since translation keys are not
     * resolved to their translated text.
     * <p>
     * This method does not account for {@code bold} (+1px per character in
     * vanilla Minecraft) or {@code italic} styling; widths are measured as if
     * the text were rendered in the regular style.
     *
     * @param component the component to measure
     * @return the total measured pixel width
     */
    public static int widthOf(Component component) {
        return widthOf(component, DEFAULT_FONT);
    }

    private static int widthOf(Component component, Key inheritedFont) {
        Key font = component.style().font() != null ? component.style().font() : inheritedFont;
        int width = 0;

        if (component instanceof TextComponent text) {
            String content = text.content();
            for (int i = 0; i < content.length(); i++) {
                width += widthOfChar(content.charAt(i), font);
            }
        }

        for (Component child : component.children()) {
            width += widthOf(child, font);
        }

        return width;
    }

    private static int widthOfChar(char c, Key font) {
        Map<Integer, Integer> fontWidths = WIDTHS.get(font.asString());
        Integer width = fontWidths != null ? fontWidths.get((int) c) : null;

        if (width == null) {
            if (WARNED_CODEPOINTS.add((int) c)) {
                LOGGER.warn("No width entry for codepoint {} (U+{}) in font {}, using fallback width {}",
                        (int) c, Integer.toHexString(c), font.asString(), FALLBACK_WIDTH);
            }
            return FALLBACK_WIDTH;
        }

        return width;
    }

    private static Map<String, Map<Integer, Integer>> load() {
        InputStream in = TextWidth.class.getResourceAsStream("/font/font-widths.json");
        if (in == null) {
            throw new IllegalStateException(
                    "Missing classpath resource /font/font-widths.json - run the font-width-generator tool");
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, new TypeToken<Map<String, Map<Integer, Integer>>>() {}.getType());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load font-widths.json", e);
        }
    }
}
