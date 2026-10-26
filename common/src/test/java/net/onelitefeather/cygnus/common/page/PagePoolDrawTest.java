package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PagePoolDrawTest {

    @Test
    void drawsOnlyFromPoolsWithEnoughSpots() {
        Map<String, Set<PageResource>> pools = Map.of("nord", spots(4), "keller", spots(2));
        Random random = new Random(1);
        Set<String> drawn = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            drawn.add(PagePoolDraw.draw(spots(4), pools, 4, random).name());
        }
        assertEquals(Set.of("default", "nord"), drawn);
    }

    @Test
    void drawnPoolCarriesItsPages() {
        Set<PageResource> nord = spots(5);
        PagePoolDraw.Drawn drawn = PagePoolDraw.draw(Set.of(), Map.of("nord", nord), 4, new Random(1));
        assertEquals(new PagePoolDraw.Drawn("nord", nord), drawn);
    }

    @Test
    void emptyDefaultIsSkippedWhenAnotherPoolFits() {
        assertEquals("nord", PagePoolDraw.draw(Set.of(), Map.of("nord", spots(4)), 4, new Random(1)).name());
    }

    @Test
    void fallsBackToDefaultWhenNoPoolIsBigEnough() {
        Set<PageResource> defaults = spots(1);
        PagePoolDraw.Drawn drawn = PagePoolDraw.draw(defaults, Map.of("nord", spots(2)), 4, new Random(1));
        assertEquals(new PagePoolDraw.Drawn("default", defaults), drawn);
    }

    @Test
    void fallsBackToEmptyDefaultWithoutAnyPool() {
        assertEquals(new PagePoolDraw.Drawn("default", Set.of()), PagePoolDraw.draw(Set.of(), Map.of(), 4, new Random(1)));
    }

    @Test
    void drawFromWithoutPagesDirectoryUsesDefault(@TempDir Path root) {
        Set<PageResource> defaults = spots(4);
        assertEquals(new PagePoolDraw.Drawn("default", defaults),
                PagePoolDraw.drawFrom(root.resolve("map.json"), defaults, 4, new Random(1)));
    }

    @Test
    void drawFromSkipsBrokenPoolFile(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", spots(4));
        Files.writeString(root.resolve("pages").resolve("kaputt.json"), "{ not json");
        assertEquals("nord", PagePoolDraw.drawFrom(mapFile, Set.of(), 4, new Random(1)).name());
    }

    @Test
    void drawFromSurvivesUnreadablePagesDirectory(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", spots(4));
        Path directory = root.resolve("pages");
        assumeTrue(directory.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Files.setPosixFilePermissions(directory, Set.of());
        try {
            assumeFalse(Files.isReadable(directory), "running as a user that ignores permissions");
            Set<PageResource> defaults = spots(4);
            assertEquals(new PagePoolDraw.Drawn("default", defaults),
                    PagePoolDraw.drawFrom(mapFile, defaults, 4, new Random(1)));
        } finally {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        }
    }

    private static Set<PageResource> spots(int count) {
        Set<PageResource> spots = new HashSet<>();
        for (int i = 0; i < count; i++) {
            spots.add(new PageResource(new Vec(i, 0, 0), Direction.NORTH));
        }
        return spots;
    }
}
