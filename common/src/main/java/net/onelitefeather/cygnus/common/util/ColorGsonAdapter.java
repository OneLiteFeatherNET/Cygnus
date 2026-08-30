package net.onelitefeather.cygnus.common.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minestom.server.color.Color;

import java.lang.reflect.Type;

/**
 * Reads and writes a {@link Color} as a hex string such as {@code "#0F6034"}.
 * <p>
 * Gson's default treatment turns a color into an object with three numeric channels. That is
 * accurate but unreadable in a file a map builder edits by hand, and it makes for noisy diffs when
 * one channel changes. A hex string is what anyone picking colors is already used to.
 * </p>
 * <p>
 * Writing always produces upper-case with a leading {@code #}; reading accepts either case and
 * tolerates a missing {@code #}, because both show up when values are pasted from elsewhere.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class ColorGsonAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {

    private static final int HEX_LENGTH = 6;
    private static final int HEX_RADIX = 16;

    /**
     * Writes the given color as an upper-case {@code #RRGGBB} string.
     *
     * @param color   the color to write
     * @param type    the declared type, unused
     * @param context the serialization context, unused
     * @return the color as a JSON string
     */
    @Override
    public JsonElement serialize(Color color, Type type, JsonSerializationContext context) {
        return new JsonPrimitive("#%02X%02X%02X".formatted(color.red(), color.green(), color.blue()));
    }

    /**
     * Reads a {@code #RRGGBB} string back into a color.
     *
     * @param json    the element to read
     * @param type    the target type, unused
     * @param context the deserialization context, unused
     * @return the parsed color
     * @throws JsonParseException if the value is not six hex digits, with or without a leading hash
     */
    @Override
    public Color deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        String raw = json.getAsString().trim();
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;

        if (hex.length() != HEX_LENGTH) {
            throw new JsonParseException("Expected a #RRGGBB color, got: " + raw);
        }

        try {
            return new Color(Integer.parseInt(hex, HEX_RADIX));
        } catch (NumberFormatException exception) {
            throw new JsonParseException("Expected a #RRGGBB color, got: " + raw, exception);
        }
    }
}
