package net.onelitefeather.cygnus.common.map.filter;

import net.kyori.adventure.key.Key;
import net.minestom.server.world.DimensionType;
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
 * @version 1.2.0
 * @since 1.0.0
 */
public final class MapFilters {

    private static final String REGION_FOLDER = "region";
    private static final String DIMENSIONS_FOLDER = "dimensions";
    private static final String MAP_FILE_NAME = "map.json";
    private static final Key OVERWORLD_KEY = DimensionType.OVERWORLD.key();

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
                .filter(MapFilters::hasRegionFolder)
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
                .filter(MapFilters::hasRegionFolder)
                .map(MapEntry::of)
                .toList();
    }

    /**
     * Checks whether the given world root holds the region files of the overworld.
     *
     * <p>A world written by 26.2 keeps its region files below
     * {@code dimensions/<namespace>/<dimension>/region}, while a world written before that keeps
     * them in a {@code region} directory next to {@code level.dat}. Both chunk loaders the project
     * uses read the current layout and fall back to the legacy one, so a world counts as a map as
     * soon as one of the two directories exists.</p>
     *
     * @param worldRoot the root directory of the world
     * @return {@code true} if the world holds region files in either layout, otherwise {@code false}
     */
    private static boolean hasRegionFolder(Path worldRoot) {
        Path dimensionRegion = worldRoot.resolve(DIMENSIONS_FOLDER)
                .resolve(OVERWORLD_KEY.namespace())
                .resolve(OVERWORLD_KEY.value())
                .resolve(REGION_FOLDER);
        return Files.isDirectory(dimensionRegion) || Files.isDirectory(worldRoot.resolve(REGION_FOLDER));
    }
}
