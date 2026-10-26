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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PagePoolTest {

    @Test
    void picksOnlyPoolsWithEnoughSpots() {
        List<PagePool> pools = List.of(new PagePool("default", spots(4)), new PagePool("nord", spots(4)),
                new PagePool("keller", spots(2)));
        Set<String> picked = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            picked.add(PagePool.pick(pools, 4).name());
        }
        assertEquals(Set.of("default", "nord"), picked);
    }

    @Test
    void emptyDefaultIsSkippedWhenAnotherPoolFits() {
        Set<PageResource> nord = spots(4);
        PagePool nordPool = new PagePool("nord", nord);
        assertEquals(nordPool, PagePool.pick(List.of(new PagePool("default", Set.of()), nordPool), 4));
    }

    @Test
    void fallsBackToDefaultWhenNoPoolIsBigEnough() {
        PagePool defaultPool = new PagePool("default", spots(1));
        assertEquals(defaultPool, PagePool.pick(List.of(defaultPool, new PagePool("nord", spots(2))), 4));
    }

    @Test
    void fallsBackToAnEmptyDefaultWhenItIsMissing() {
        assertEquals(new PagePool("default", Set.of()), PagePool.pick(List.of(new PagePool("nord", spots(2))), 4));
    }

    @Test
    void forRoundReadsTheDefaultPoolFromThePagesDirectory(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        Set<PageResource> defaults = spots(4);
        PagePoolFiles.save(mapFile, "default", defaults);
        assertEquals(new PagePool("default", defaults), PagePool.forRound(mapFile, 4));
    }

    @Test
    void forRoundWithoutPagesDirectoryFallsBackToAnEmptyDefault(@TempDir Path root) {
        assertEquals(new PagePool("default", Set.of()), PagePool.forRound(root.resolve("map.json"), 4));
    }

    @Test
    void forRoundSurvivesUnreadablePagesDirectory(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", spots(4));
        Path directory = root.resolve("pages");
        assumeTrue(directory.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Files.setPosixFilePermissions(directory, Set.of());
        try {
            assumeFalse(Files.isReadable(directory), "running as a user that ignores permissions");
            assertEquals(new PagePool("default", Set.of()), PagePool.forRound(mapFile, 4));
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
