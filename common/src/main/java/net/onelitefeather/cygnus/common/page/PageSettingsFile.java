package net.onelitefeather.cygnus.common.page;

import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapSideFile;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Reads and writes the {@code pages.json} file that sits next to a map's {@code map.json} and holds
 * the page values the map sets for itself under {@code settings}. They apply to every page pool of
 * the map; the pools themselves live in the {@code pages} folder (see {@link PagePoolFiles}).
 *
 * <p>A file in an older layout may still hold pages, under {@code pages} or as the bare list. Those
 * are ignored; a bare list has no settings and leaves every value at its default.</p>
 */
public final class PageSettingsFile {

    private static final String FILE_NAME = "pages.json";
    private static final String WHAT = "page settings";

    private PageSettingsFile() {
        // Nothing to do here
    }

    /**
     * Returns the {@code pages.json} path that belongs to the given map file.
     *
     * @param mapFile the map file the settings belong to
     * @return the sibling path, regardless of whether it exists
     */
    public static Path resolve(Path mapFile) {
        return mapFile.resolveSibling(FILE_NAME);
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
     * Loads the page values like {@link #loadSettings(Path)}, but falls back to
     * {@link PageSettings#DEFAULT} if they are broken. Meant for the setup, which has to open a
     * broken map so it can be fixed.
     *
     * @param mapFile   the map file the settings belong to
     * @param onInvalid receives the reason if the settings were broken and the defaults are used
     * @return the settings, or the defaults if they are broken
     */
    public static PageSettings loadSettingsOrDefaults(Path mapFile, Consumer<String> onInvalid) {
        Path pagesFile = resolve(mapFile);
        return MapSideFile.settingsOrDefaults(MapSideFile.parse(WHAT, pagesFile), pagesFile, PageSettings.class,
                PageSettings.DEFAULT, onInvalid);
    }

    /**
     * Writes the page settings to the sibling {@code pages.json}, creating parent directories as needed.
     *
     * @param mapFile  the map file the settings belong to
     * @param settings the page values the map sets for itself
     */
    public static void save(Path mapFile, PageSettings settings) {
        MapSideFile.writeSettings(WHAT, resolve(mapFile), settings);
    }

    /**
     * Returns a copy of the given map carrying the page settings from the sibling {@code pages.json}.
     *
     * @param mapFile the map file {@code map} was loaded from
     * @param map     the map to put the settings into
     * @return a new {@link GameMap} carrying the page settings
     */
    public static GameMap loadInto(Path mapFile, GameMap map) {
        return map.withPageSettings(loadSettings(mapFile));
    }

    /**
     * Works like {@link #loadInto(Path, GameMap)}, but falls back to {@link PageSettings#DEFAULT}
     * if the page settings are broken. Meant for the setup, which has to open a broken map so it
     * can be fixed.
     *
     * @param mapFile   the map file {@code map} was loaded from
     * @param map       the map to put the settings into
     * @param onInvalid receives the reason if the settings were broken and the defaults are used
     * @return a new {@link GameMap} carrying the page settings
     */
    public static GameMap loadInto(Path mapFile, GameMap map, Consumer<String> onInvalid) {
        return map.withPageSettings(loadSettingsOrDefaults(mapFile, onInvalid));
    }
}
