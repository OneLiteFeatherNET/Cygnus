package net.onelitefeather.cygnus.map;

import net.onelitefeather.cygnus.common.creek.CreekRoutesFile;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.filter.MapFilters;
import net.onelitefeather.cygnus.common.page.PageSettingsFile;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.theevilreaper.aves.map.MapEntry;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Holds every map the game found below {@code game/maps}: the lobby on its own and the game maps a
 * round can be played on.
 *
 * <p>A game map is only taken in if its {@code map.json} and its side files ({@code pages.json},
 * {@code creek.json}) can be read. A map that fails is skipped with a warning, so a single broken
 * map does not keep the server from starting as long as another one is left.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class MapCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapCatalog.class);
    private static final String LOBBY_DIRECTORY = "lobby";

    private final MapEntry lobby;
    private final List<MapCandidate> candidates;

    private MapCatalog(MapEntry lobby, List<MapCandidate> candidates) {
        this.lobby = lobby;
        this.candidates = List.copyOf(candidates);
    }

    /**
     * Searches the maps below the given path.
     *
     * @param path the directory holding {@code game/maps}
     * @return the catalog of the found maps
     * @throws IllegalStateException if there is no lobby or no game map that can be read
     * @throws UncheckedIOException  if the map directory cannot be listed
     */
    public static MapCatalog load(Path path) {
        Path mapsDirectory = path.resolve("game").resolve("maps");
        List<MapEntry> entries;
        try (Stream<Path> stream = Files.list(mapsDirectory)) {
            entries = MapFilters.filterMapsForGame(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to list the maps in " + mapsDirectory, exception);
        }

        MapEntry lobby = null;
        List<MapCandidate> candidates = new ArrayList<>();
        for (MapEntry entry : entries) {
            if (isLobby(entry)) {
                lobby = entry;
                continue;
            }
            try {
                candidates.add(new MapCandidate(entry, readGameMap(entry.getMapFile())));
            } catch (RuntimeException exception) {
                LOGGER.warn("Skipping map in {}: {}", entry.getDirectoryRoot(), exception.getMessage());
            }
        }

        if (lobby == null) {
            throw new IllegalStateException("No lobby map found in " + mapsDirectory);
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No game map that can be read found in " + mapsDirectory);
        }
        LOGGER.info("Found {} game map(s): {}", candidates.size(), candidates.stream().map(MapCandidate::name).toList());
        return new MapCatalog(lobby, candidates);
    }

    /**
     * Reads a game map from its map file, together with the page and creek settings of its side files.
     *
     * <p>The pages themselves are only put in once the map is loaded for a round, from the pool
     * picked for it.</p>
     *
     * @param mapFile the {@code map.json} of the map
     * @return the read game map
     * @throws IllegalStateException if the map file cannot be read
     * @throws RuntimeException      if a side file holds broken or invalid values
     */
    static GameMap readGameMap(Path mapFile) {
        GameMap map = GsonHelper.FILE_HANDLER.load(mapFile, GameMap.class)
                .orElseThrow(() -> new IllegalStateException("Failed to load GameMap from file: " + mapFile));
        return CreekRoutesFile.loadInto(mapFile, PageSettingsFile.loadInto(mapFile, map));
    }

    /**
     * Answers whether an entry is the lobby, by the name of its own directory rather than by the
     * path leading to it - a temporary directory or a checkout below a folder called {@code lobby}
     * would otherwise make every map the lobby.
     *
     * @param entry the entry to look at
     * @return {@code true} if the entry is the lobby
     */
    private static boolean isLobby(MapEntry entry) {
        Path directory = entry.getDirectoryRoot().getFileName();
        return directory != null && directory.toString().equalsIgnoreCase(LOBBY_DIRECTORY);
    }

    /**
     * Picks the map the round is played on.
     *
     * @param selector decides between the game maps
     * @return the picked map
     */
    public MapCandidate select(MapSelector selector) {
        MapCandidate selected = selector.select(this.candidates);
        LOGGER.info("Selected map {} out of {}", selected.name(), this.candidates.size());
        return selected;
    }

    /**
     * Returns the lobby map.
     *
     * @return the entry of the lobby
     */
    public MapEntry lobby() {
        return this.lobby;
    }

    /**
     * Returns every game map that can be played on.
     *
     * @return the game maps, in the order they were found
     */
    public @Unmodifiable List<MapCandidate> candidates() {
        return this.candidates;
    }
}
