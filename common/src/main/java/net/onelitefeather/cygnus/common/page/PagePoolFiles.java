package net.onelitefeather.cygnus.common.page;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.onelitefeather.cygnus.common.map.MapSideFile;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads and writes the page pools in the {@code pages} folder next to a map's {@code map.json}.
 * Each {@code pages/<name>.json} holds one pool, a set of {@link PageResource}s under {@code pages}.
 *
 * <p>The default pool has no file in that folder. It still lives in the old {@code pages.json}
 * (see {@link PageFacesFile}), which the game keeps reading until it picks pools itself.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PagePoolFiles {

    /** The name of the pool that lives in {@code pages.json}. */
    public static final String DEFAULT_POOL = "default";

    private static final String DIRECTORY_NAME = "pages";
    private static final String EXTENSION = ".json";
    private static final String WHAT = "page pool";
    private static final String PAGES_KEY = "pages";
    // The name is the file name, so it may only hold characters that are safe in one
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9_-]+");
    private static final Type PAGES_TYPE = TypeToken.getParameterized(Set.class, PageResource.class).getType();

    private PagePoolFiles() {
        // Nothing to do here
    }

    /**
     * Returns the {@code pages} folder that belongs to the given map file.
     *
     * @param mapFile the map file the pools belong to
     * @return the sibling folder, regardless of whether it exists
     */
    public static Path resolveDirectory(Path mapFile) {
        return mapFile.resolveSibling(DIRECTORY_NAME);
    }

    /**
     * Turns a name a builder typed into a pool name.
     *
     * @param raw the typed name
     * @return the trimmed name in lower case, or {@code null} if it is empty, holds other characters
     *         than {@code a-z}, {@code 0-9}, {@code _} and {@code -}, or is the {@link #DEFAULT_POOL}
     */
    public static @Nullable String normalizeName(String raw) {
        String name = raw.trim().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(name).matches() || DEFAULT_POOL.equals(name)) return null;
        return name;
    }

    /**
     * Loads every pool from the {@code pages} folder. A file that cannot be read or whose name is
     * no valid pool name is skipped, so the rest of the map still opens.
     *
     * @param mapFile  the map file the pools belong to
     * @param onBroken receives the file name without {@code .json} and the reason for every skipped file
     * @return the pools sorted by name, empty if the folder does not exist
     */
    public static Map<String, Set<PageResource>> load(Path mapFile, BiConsumer<String, String> onBroken) {
        Map<String, Set<PageResource>> pools = new TreeMap<>();
        Path directory = resolveDirectory(mapFile);
        if (!Files.isDirectory(directory)) return pools;
        for (Path file : listPoolFiles(directory)) {
            String fileName = file.getFileName().toString();
            String name = fileName.substring(0, fileName.length() - EXTENSION.length());
            if (!name.equals(normalizeName(name))) {
                onBroken.accept(name, "Invalid pool name in " + file);
                continue;
            }
            try {
                JsonElement pages = MapSideFile.data(MapSideFile.parse(WHAT, file), PAGES_KEY);
                Set<PageResource> loaded = pages == null ? null : GsonHelper.GSON.fromJson(pages, PAGES_TYPE);
                pools.put(name, loaded != null ? loaded : Set.of());
            } catch (RuntimeException exception) {
                // Not only JsonParseException: Gson lets a null face or a text coordinate through as
                // UnsupportedOperationException or NumberFormatException, and one bad file must not stop the map
                onBroken.accept(name, "Failed to load " + WHAT + " from " + file + ": " + exception.getMessage());
            }
        }
        return pools;
    }

    /**
     * Writes a pool to {@code pages/<name>.json}, creating the folder as needed.
     *
     * @param mapFile the map file the pool belongs to
     * @param name    the name of the pool
     * @param pages   the pages of the pool
     */
    public static void save(Path mapFile, String name, Set<PageResource> pages) {
        Path file = resolveFile(mapFile, name);
        JsonObject root = new JsonObject();
        root.add(PAGES_KEY, GsonHelper.GSON.toJsonTree(pages, PAGES_TYPE));
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GsonHelper.GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save " + WHAT + " to " + file, exception);
        }
    }

    /**
     * Deletes the file of a pool, if it exists.
     *
     * @param mapFile the map file the pool belongs to
     * @param name    the name of the pool
     */
    public static void delete(Path mapFile, String name) {
        Path file = resolveFile(mapFile, name);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete " + WHAT + " " + file, exception);
        }
    }

    private static Path resolveFile(Path mapFile, String name) {
        return resolveDirectory(mapFile).resolve(name + EXTENSION);
    }

    private static List<Path> listPoolFiles(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(EXTENSION))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to list the " + WHAT + "s in " + directory, exception);
        }
    }
}
