package net.onelitefeather.cygnus.common.map.settings.adapter;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializer;

import java.math.BigDecimal;

/**
 * The shared part of the adapters that read and write the settings a map sets for itself.
 * <p>
 * A key the block leaves out takes its default, so a map only has to name what it changes. A value
 * that is there but cannot be read is never replaced by its default: it names the key instead,
 * because a silently ignored value hides a broken map.
 * </p>
 *
 * @param <S> the settings record
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
abstract class SettingsAdapter<S> implements JsonSerializer<S>, JsonDeserializer<S> {

    /**
     * Turns a settings block into the object it was written from.
     *
     * @param json the settings block
     * @return the block as an object
     * @throws JsonParseException if the block is no object
     */
    static JsonObject block(JsonElement json) {
        if (!json.isJsonObject()) {
            throw new JsonParseException("The settings have to be an object");
        }
        return json.getAsJsonObject();
    }

    /**
     * Reads a whole number.
     *
     * @param block    the settings block
     * @param key      the key in the block
     * @param fallback the value if the key is missing
     * @return the value from the block, or the fallback
     * @throws JsonParseException if the value is not a whole number
     */
    static int getInt(JsonObject block, String key, int fallback) {
        JsonElement element = block.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return number(element).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new JsonParseException("settings." + key + " has to be a whole number", exception);
        }
    }

    /**
     * Reads a number.
     *
     * @param block    the settings block
     * @param key      the key in the block
     * @param fallback the value if the key is missing
     * @return the value from the block, or the fallback
     * @throws JsonParseException if the value is not a number
     */
    static double getDouble(JsonObject block, String key, double fallback) {
        JsonElement element = block.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return number(element).doubleValue();
        } catch (NumberFormatException exception) {
            throw new JsonParseException("settings." + key + " has to be a number", exception);
        }
    }

    private static BigDecimal number(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new NumberFormatException("not a number: " + element);
        }
        return element.getAsBigDecimal();
    }
}
