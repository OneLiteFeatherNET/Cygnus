package net.onelitefeather.cygnus.common.page;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.GsonHelper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads and writes the {@code pages.json} file that sits next to a map's {@code map.json} and
 * holds every {@link PageResource} for that map, so the map file itself stays free of the
 * (potentially very long) page list.
 */
public final class PageFacesFile {

    private static final String FILE_NAME = "pages.json";
    private static final Type PAGE_FACES_TYPE = TypeToken.getParameterized(Set.class, PageResource.class).getType();

    private PageFacesFile() {
        // Nothing to do here
    }

    /**
     * Returns the {@code pages.json} path that belongs to the given map file.
     *
     * @param mapFile the map file the page faces belong to
     * @return the sibling path, regardless of whether it exists
     */
    public static Path resolve(Path mapFile) {
        return mapFile.resolveSibling(FILE_NAME);
    }

    /**
     * Loads the page faces from the sibling {@code pages.json}, if it exists.
     *
     * @param mapFile the map file the page faces belong to
     * @return the loaded set, or an empty set if the sibling file does not exist
     */
    public static Set<PageResource> load(Path mapFile) {
        Path pagesFile = resolve(mapFile);
        if (!Files.exists(pagesFile)) {
            return Set.of();
        }

        try (var reader = Files.newBufferedReader(pagesFile, StandardCharsets.UTF_8)) {
            Set<PageResource> pageFaces;
            try {
                pageFaces = GsonHelper.GSON.fromJson(reader, PAGE_FACES_TYPE);
            } catch (JsonSyntaxException exception) {
                throw new JsonSyntaxException("Failed to parse page faces from " + pagesFile + ": " + exception.getMessage(), exception);
            }
            return pageFaces != null ? pageFaces : Set.of();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load page faces from " + pagesFile, exception);
        }
    }

    /**
     * Writes the given page faces to the sibling {@code pages.json}, creating parent directories
     * as needed.
     *
     * @param mapFile   the map file the page faces belong to
     * @param pageFaces the page faces to persist
     */
    public static void save(Path mapFile, Set<PageResource> pageFaces) {
        Path pagesFile = resolve(mapFile);
        try {
            if (pagesFile.getParent() != null) {
                Files.createDirectories(pagesFile.getParent());
            }
            try (var writer = Files.newBufferedWriter(pagesFile, StandardCharsets.UTF_8)) {
                GsonHelper.GSON.toJson(pageFaces, PAGE_FACES_TYPE, writer);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save page faces to " + pagesFile, exception);
        }
    }

    /**
     * Returns a copy of the given map whose page faces are the union of its own page faces
     * (e.g. read from an old-style {@code map.json} that still inlines them) and whatever the
     * sibling {@code pages.json} holds.
     *
     * @param mapFile the map file {@code map} was loaded from
     * @param map     the map to merge page faces into
     * @return a new {@link GameMap} carrying the merged page faces
     */
    public static GameMap loadInto(Path mapFile, GameMap map) {
        Set<PageResource> merged = new HashSet<>(map.getPageFaces());
        merged.addAll(load(mapFile));

        return new GameMap(
                map.name(),
                map.spawn(),
                map.getSlenderSpawn(),
                merged,
                map.getSurvivorSpawns(),
                map.builders(),
                map.getAtmosphere(),
                map.getCreekRoutes()
        );
    }
}
