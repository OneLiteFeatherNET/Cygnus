package net.onelitefeather.cygnus.common.map.filter;

import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that both world layouts are recognized as maps: the one 26.2 writes, where the region
 * files live below {@code dimensions/minecraft/overworld}, and the legacy one which keeps them in a
 * {@code region} directory next to {@code level.dat}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
class MapFiltersTest {

    @Test
    void testGameFilterAcceptsDimensionLayout(@TempDir Path maps) throws IOException {
        Path arena = writeDimensionLayoutMap(maps.resolve("arena"));
        Files.writeString(arena.resolve("map.json"), "{}", StandardCharsets.UTF_8);

        List<MapEntry> entries = filterForGame(maps);

        assertEquals(1, entries.size());
        assertEquals(arena, entries.getFirst().getDirectoryRoot());
        assertTrue(entries.getFirst().hasMapFile());
    }

    @Test
    void testGameFilterAcceptsLegacyLayout(@TempDir Path maps) throws IOException {
        Path arena = writeLegacyLayoutMap(maps.resolve("arena"));
        Files.writeString(arena.resolve("map.json"), "{}", StandardCharsets.UTF_8);

        List<MapEntry> entries = filterForGame(maps);

        assertEquals(1, entries.size());
        assertEquals(arena, entries.getFirst().getDirectoryRoot());
    }

    @Test
    void testGameFilterSkipsMapWithoutMapFile(@TempDir Path maps) throws IOException {
        writeDimensionLayoutMap(maps.resolve("arena"));

        assertTrue(filterForGame(maps).isEmpty());
    }

    @Test
    void testGameFilterSkipsDirectoryWithoutRegions(@TempDir Path maps) throws IOException {
        Path arena = Files.createDirectories(maps.resolve("arena"));
        Files.writeString(arena.resolve("map.json"), "{}", StandardCharsets.UTF_8);

        assertTrue(filterForGame(maps).isEmpty());
    }

    @Test
    void testSetupFilterAcceptsBothLayouts(@TempDir Path maps) throws IOException {
        writeDimensionLayoutMap(maps.resolve("arena"));
        writeLegacyLayoutMap(maps.resolve("lobby"));

        assertEquals(2, filterForSetup(maps).size());
    }

    @Test
    void testSetupFilterSkipsDirectoryWithoutRegions(@TempDir Path maps) throws IOException {
        Files.createDirectories(maps.resolve("arena"));

        assertTrue(filterForSetup(maps).isEmpty());
    }

    /**
     * Runs the game filter over the direct children of the given directory.
     *
     * @param maps the directory which holds the world directories
     * @return the entries the filter accepted
     * @throws IOException if the directory cannot be listed
     */
    private List<MapEntry> filterForGame(Path maps) throws IOException {
        try (Stream<Path> stream = Files.list(maps)) {
            return MapFilters.filterMapsForGame(stream);
        }
    }

    /**
     * Runs the setup filter over the direct children of the given directory.
     *
     * @param maps the directory which holds the world directories
     * @return the entries the filter accepted
     * @throws IOException if the directory cannot be listed
     */
    private List<MapEntry> filterForSetup(Path maps) throws IOException {
        try (Stream<Path> stream = Files.list(maps)) {
            return MapFilters.filterMapsForSetup(stream);
        }
    }

    /**
     * Writes a world directory in the layout 26.2 uses.
     *
     * @param worldRoot the root directory of the world
     * @return the written world root
     * @throws IOException if the layout cannot be written
     */
    private Path writeDimensionLayoutMap(Path worldRoot) throws IOException {
        Files.createDirectories(worldRoot.resolve("dimensions").resolve("minecraft").resolve("overworld").resolve("region"));
        return worldRoot;
    }

    /**
     * Writes a world directory in the layout used before 26.2.
     *
     * @param worldRoot the root directory of the world
     * @return the written world root
     * @throws IOException if the layout cannot be written
     */
    private Path writeLegacyLayoutMap(Path worldRoot) throws IOException {
        Files.createDirectories(worldRoot.resolve("region"));
        return worldRoot;
    }
}
