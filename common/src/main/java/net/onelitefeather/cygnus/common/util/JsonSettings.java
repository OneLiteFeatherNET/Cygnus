package net.onelitefeather.cygnus.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the {@code settings} block of a map's side file.
 * <p>
 * A key the block leaves out takes its default and is remembered, so the loader can say once which
 * values it filled in. A value that is there but cannot be read is never replaced by its default:
 * it names the file and the key instead, because a silently ignored value hides a broken map.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class JsonSettings {

    private static final String SETTINGS_KEY = "settings";

    private final @Nullable JsonObject settings;
    private final Path file;
    private final List<String> missing;

    private JsonSettings(@Nullable JsonObject settings, Path file) {
        this.settings = settings;
        this.file = file;
        this.missing = new ArrayList<>();
    }

    /**
     * Takes the {@code settings} block from the root object of a side file.
     *
     * @param root the parsed file
     * @param file the file, named in every error
     * @return the reader, without settings if the file has no block
     * @throws JsonSyntaxException if the block is there but not an object
     */
    public static JsonSettings of(JsonObject root, Path file) {
        JsonElement element = root.get(SETTINGS_KEY);
        if (element == null || element.isJsonNull()) {
            return new JsonSettings(null, file);
        }
        if (!element.isJsonObject()) {
            throw new JsonSyntaxException("The settings in " + file + " have to be an object");
        }
        return new JsonSettings(element.getAsJsonObject(), file);
    }

    /**
     * A reader for a file in the old layout, which has no settings at all.
     *
     * @param file the file
     * @return the reader, without settings
     */
    public static JsonSettings none(Path file) {
        return new JsonSettings(null, file);
    }

    /**
     * Reads a whole number.
     *
     * @param key      the key in the block
     * @param fallback the value if the key is missing
     * @return the value from the file, or the fallback
     * @throws JsonSyntaxException if the value is not a whole number
     */
    public int getInt(String key, int fallback) {
        JsonElement element = this.element(key);
        if (element == null) return fallback;
        try {
            return number(element).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new JsonSyntaxException("settings." + key + " in " + this.file + " has to be a whole number", exception);
        }
    }

    /**
     * Reads a number.
     *
     * @param key      the key in the block
     * @param fallback the value if the key is missing
     * @return the value from the file, or the fallback
     * @throws JsonSyntaxException if the value is not a number
     */
    public double getDouble(String key, double fallback) {
        JsonElement element = this.element(key);
        if (element == null) return fallback;
        try {
            return number(element).doubleValue();
        } catch (NumberFormatException exception) {
            throw new JsonSyntaxException("settings." + key + " in " + this.file + " has to be a number", exception);
        }
    }

    /**
     * Returns whether the file has a settings block at all.
     *
     * @return {@code true} if it has one
     */
    public boolean hasSettings() {
        return this.settings != null;
    }

    /**
     * Returns the keys that were read but missing from the block, in the order they were asked for.
     *
     * @return the missing keys, empty without a block
     */
    public List<String> missing() {
        return List.copyOf(this.missing);
    }

    /**
     * Logs which values the file did not set: one line for a file without settings, one line naming
     * the filled keys otherwise. Complete settings log nothing.
     *
     * @param logger where to log
     */
    public void logMissing(Logger logger) {
        if (!this.hasSettings()) {
            logger.info("{} has no settings, the map uses the defaults", this.file);
            return;
        }
        if (!this.missing.isEmpty()) {
            logger.info("{} leaves out {}, the map uses the defaults for them", this.file, String.join(", ", this.missing));
        }
    }

    private @Nullable JsonElement element(String key) {
        if (this.settings == null) return null;
        JsonElement element = this.settings.get(key);
        if (element == null || element.isJsonNull()) {
            this.missing.add(key);
            return null;
        }
        return element;
    }

    private static BigDecimal number(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new NumberFormatException("not a number: " + element);
        }
        return element.getAsBigDecimal();
    }
}
