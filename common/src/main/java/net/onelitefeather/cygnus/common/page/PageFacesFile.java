package net.onelitefeather.cygnus.common.page;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapSideFile;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import net.onelitefeather.cygnus.common.util.GsonHelper;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads and writes the {@code pages.json} file that sits next to a map's {@code map.json} and
 * holds every {@link PageResource} for that map, so the map file itself stays free of the
 * (potentially very long) page list.
 *
 * <p>The file holds the list under {@code pages} and the page values the map sets for itself under
 * {@code settings}. A file in the old layout is the bare list and leaves every setting at its default.</p>
 */
public final class PageFacesFile {

    private static final String FILE_NAME = "pages.json";
    private static final String WHAT = "page faces";
    private static final String PAGES_KEY = "pages";
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
        JsonElement pages = MapSideFile.data(MapSideFile.parse(WHAT, pagesFile), PAGES_KEY);
        if (pages == null) return Set.of();
        try {
            Set<PageResource> pageFaces = GsonHelper.GSON.fromJson(pages, PAGE_FACES_TYPE);
            return pageFaces != null ? pageFaces : Set.of();
        } catch (JsonSyntaxException exception) {
            throw new JsonSyntaxException("Failed to parse " + WHAT + " from " + pagesFile + ": " + exception.getMessage(), exception);
        }
    }

    /**
     * Loads the page values the map sets for itself from the sibling {@code pages.json}.
     *
     * @param mapFile the map file the settings belong to
     * @return the settings, {@link PageSettings#DEFAULT} for a missing file and filled up with
     *         defaults for every key the file leaves out
     * @throws IllegalArgumentException if a value is out of range, naming the file
     */
    public static PageSettings loadSettings(Path mapFile) {
        Path pagesFile = resolve(mapFile);
        return MapSideFile.settings(MapSideFile.parse(WHAT, pagesFile), pagesFile, PageSettings.class, PageSettings.DEFAULT);
    }

    /**
     * Writes the given page faces to the sibling {@code pages.json} with the default page settings.
     *
     * @param mapFile   the map file the page faces belong to
     * @param pageFaces the page faces to persist
     */
    public static void save(Path mapFile, Set<PageResource> pageFaces) {
        save(mapFile, pageFaces, PageSettings.DEFAULT);
    }

    /**
     * Writes the page faces and the page settings to the sibling {@code pages.json}, creating
     * parent directories as needed.
     *
     * @param mapFile   the map file the page faces belong to
     * @param pageFaces the page faces to persist
     * @param settings  the page values the map sets for itself
     */
    public static void save(Path mapFile, Set<PageResource> pageFaces, PageSettings settings) {
        MapSideFile.write(WHAT, resolve(mapFile), PAGES_KEY, GsonHelper.GSON.toJsonTree(pageFaces, PAGE_FACES_TYPE), settings);
    }

    /**
     * Returns a copy of the given map whose page faces are the union of its own page faces
     * (e.g. read from an old-style {@code map.json} that still inlines them) and whatever the
     * sibling {@code pages.json} holds, together with the page settings from that file.
     *
     * @param mapFile the map file {@code map} was loaded from
     * @param map     the map to merge page faces into
     * @return a new {@link GameMap} carrying the merged page faces and the page settings
     */
    public static GameMap loadInto(Path mapFile, GameMap map) {
        Set<PageResource> merged = new HashSet<>(map.getPageFaces());
        merged.addAll(load(mapFile));

        return map.withPageFaces(merged).withPageSettings(loadSettings(mapFile));
    }
}
