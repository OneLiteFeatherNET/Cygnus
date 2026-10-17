package net.onelitefeather.cygnus.common.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Reads and writes the files that sit next to a map's {@code map.json} and hold its data together
 * with the settings the map sets for itself, such as {@code pages.json} and {@code creek.json}.
 * <p>
 * Such a file is an object with the data under its own key and the settings under {@code settings}.
 * A file in the old layout is the bare data list and has no settings; the map then uses the defaults.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class MapSideFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapSideFile.class);
    private static final String SETTINGS_KEY = "settings";

    private MapSideFile() {
        // Nothing to do here
    }

    /**
     * Parses a side file.
     *
     * @param file what the file holds, for error messages, e.g. {@code "page faces"}
     * @param path the file
     * @return the parsed content, or {@code null} if the file does not exist
     * @throws JsonSyntaxException if the file is no valid JSON, naming the file
     */
    public static @Nullable JsonElement parse(String file, Path path) {
        if (!Files.exists(path)) return null;
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (JsonParseException exception) {
            throw new JsonSyntaxException("Failed to parse " + file + " from " + path + ": " + exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load " + file + " from " + path, exception);
        }
    }

    /**
     * Returns the data part of a parsed side file.
     *
     * @param root    the parsed file, {@code null} if it does not exist
     * @param dataKey the key the data sits under in the new layout
     * @return the data, or {@code null} if there is none
     */
    public static @Nullable JsonElement data(@Nullable JsonElement root, String dataKey) {
        if (root == null || root.isJsonNull()) return null;
        // The old layout is the bare list, the new one keeps it under its key next to the settings.
        JsonElement data = root.isJsonObject() ? root.getAsJsonObject().get(dataKey) : root;
        return data == null || data.isJsonNull() ? null : data;
    }

    /**
     * Reads the settings of a parsed side file. A key the file leaves out takes its default.
     * <p>
     * Logs one line if the file has no settings at all, and warns about keys the settings do not
     * know, since such a key is most likely a typo and its value is never used. A missing file logs nothing.
     * </p>
     *
     * @param root     the parsed file, {@code null} if it does not exist
     * @param path     the file, named in messages
     * @param type     the settings record
     * @param defaults the settings without a file or without a settings block
     * @param <S>      the settings record
     * @return the settings
     * @throws IllegalArgumentException if a value is out of range or values contradict each other, naming the file
     * @throws JsonSyntaxException      if a value cannot be read, naming the file
     */
    public static <S extends Record> S settings(@Nullable JsonElement root, Path path, Class<S> type, S defaults) {
        if (root == null || root.isJsonNull()) return defaults;
        JsonElement block = root.isJsonObject() ? root.getAsJsonObject().get(SETTINGS_KEY) : null;
        if (block == null || block.isJsonNull()) {
            LOGGER.info("{} has no settings, the map uses the defaults", path);
            return defaults;
        }
        if (!block.isJsonObject()) {
            throw new JsonSyntaxException("The settings in " + path + " have to be an object");
        }
        S settings;
        try {
            settings = GsonHelper.GSON.fromJson(block, type);
        } catch (JsonParseException exception) {
            throw new JsonSyntaxException("Invalid settings in " + path + ": " + exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid settings in " + path + ": " + exception.getMessage(), exception);
        }
        List<String> unknown = unknownKeys(block.getAsJsonObject(), type);
        if (!unknown.isEmpty()) {
            LOGGER.warn("{} has unknown settings {}, they are ignored", path, String.join(", ", unknown));
        }
        return settings;
    }

    /**
     * Reads the settings of a parsed side file like {@link #settings}, but falls back to the
     * defaults if they cannot be used. Meant for the setup, which has to open a broken map so it
     * can be fixed.
     *
     * @param root      the parsed file, {@code null} if it does not exist
     * @param path      the file, named in messages
     * @param type      the settings record
     * @param defaults  the settings without a file, without a settings block or with broken settings
     * @param onInvalid receives the reason if the settings were broken and the defaults are used
     * @param <S>       the settings record
     * @return the settings, or the defaults if they are broken
     */
    public static <S extends Record> S settingsOrDefaults(@Nullable JsonElement root, Path path, Class<S> type, S defaults,
                                                          Consumer<String> onInvalid) {
        try {
            return settings(root, path, type, defaults);
        } catch (IllegalArgumentException | JsonSyntaxException exception) {
            LOGGER.warn("{}, the map uses the defaults", exception.getMessage());
            onInvalid.accept(exception.getMessage());
            return defaults;
        }
    }

    /**
     * Writes a side file in the new layout.
     *
     * @param file     what the file holds, for error messages, e.g. {@code "page faces"}
     * @param path     the file
     * @param dataKey  the key the data is written under
     * @param data     the data
     * @param settings the settings the map sets for itself
     */
    public static void write(String file, Path path, String dataKey, JsonElement data, Record settings) {
        JsonObject root = new JsonObject();
        root.add(dataKey, data);
        root.add(SETTINGS_KEY, GsonHelper.GSON.toJsonTree(settings));
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GsonHelper.GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save " + file + " to " + path, exception);
        }
    }

    /**
     * Returns the keys of a settings block that the settings record has no value for.
     *
     * @param block the settings block
     * @param type  the settings record
     * @return the unknown keys, in block order
     */
    public static List<String> unknownKeys(JsonObject block, Class<? extends Record> type) {
        Set<String> known = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        return block.keySet().stream()
                .filter(key -> !known.contains(key))
                .toList();
    }
}
