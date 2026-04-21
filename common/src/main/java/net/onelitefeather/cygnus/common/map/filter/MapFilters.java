package net.onelitefeather.cygnus.common.map.filter;

import net.theevilreaper.aves.map.MapEntry;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * The {@link MapFilters} class contains some filter method to filter maps for different conditions.
 * The game module needs another filter to logic than the setup.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
public final class MapFilters {

    private static final String REGION_FOLDER = "region";
    private static final String MAP_FILE_NAME = "map.json";

    private MapFilters() {

    }

    /**
     * Filters through the given stream of paths and returns a list of maps which are available for the game.
     *
     * @param mapStream a stream of paths
     * @return a list that contains different maps which are available for the game
     */
    public static @Unmodifiable List<MapEntry> filterMapsForGame(Stream<Path> mapStream) {
        return mapStream
                .filter(Files::isDirectory)
                .filter(path -> Files.exists(path.resolve(REGION_FOLDER)))
                .filter(path -> Files.exists(path.resolve(MAP_FILE_NAME)))
                .map(MapEntry::of)
                .toList();
    }

    /**
     * Filters through the given stream of paths and returns a list of maps which are available for the setup.
     *
     * @param mapStream a stream of paths
     * @return a list that contains different maps which are available for the setup
     */
    public static @Unmodifiable List<MapEntry> filterMapsForSetup(Stream<Path> mapStream) {
        return mapStream
                .filter(Files::isDirectory)
                .filter(path -> Files.exists(path.resolve(REGION_FOLDER)))
                .map(MapEntry::of)
                .toList();
    }
}
