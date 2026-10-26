package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagePoolFilesTest {

    private static final PageResource NORTH_PAGE = new PageResource(new Vec(1, 2, 3), Direction.NORTH);
    private static final PageResource SOUTH_PAGE = new PageResource(new Vec(4, 5, 6), Direction.SOUTH);

    @Test
    void resolvesToASiblingPagesDirectory(@TempDir Path root) {
        assertEquals(root.resolve("pages"), PagePoolFiles.resolveDirectory(root.resolve("map.json")));
    }

    @Test
    void normalizeNameLowercasesAndTrims() {
        assertEquals("nord_1", PagePoolFiles.normalizeName("  Nord_1 "));
        assertEquals("keller-2", PagePoolFiles.normalizeName("keller-2"));
    }

    @Test
    void normalizeNameRejectsInvalidNames() {
        for (String name : List.of("", "   ", "a b", "ä", "../x", "default", "DEFAULT")) {
            assertNull(PagePoolFiles.normalizeName(name), name);
        }
    }

    @Test
    void loadReturnsNothingWithoutDirectory(@TempDir Path root) {
        assertTrue(PagePoolFiles.load(root.resolve("map.json"), (_, _) -> {}).isEmpty());
    }

    @Test
    void savedPoolsCanBeLoadedBack(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", Set.of(NORTH_PAGE));
        PagePoolFiles.save(mapFile, "keller", Set.of(SOUTH_PAGE));

        Map<String, Set<PageResource>> pools = PagePoolFiles.load(mapFile, (_, _) -> {});

        assertEquals(Map.of("keller", Set.of(SOUTH_PAGE), "nord", Set.of(NORTH_PAGE)), pools);
        assertEquals(List.of("keller", "nord"), new ArrayList<>(pools.keySet()));
    }

    @Test
    void savedPoolFileHasNoSettings(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", Set.of(NORTH_PAGE));

        String content = Files.readString(root.resolve("pages").resolve("nord.json"));

        assertTrue(content.contains("\"pages\""));
        assertFalse(content.contains("settings"));
    }

    @Test
    void brokenPoolIsSkippedAndReported(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", Set.of(NORTH_PAGE));
        Path broken = root.resolve("pages").resolve("kaputt.json");
        Files.writeString(broken, "{ not valid json ]");
        List<String> reported = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        Map<String, Set<PageResource>> pools = PagePoolFiles.load(mapFile, (name, reason) -> {
            reported.add(name);
            reasons.add(reason);
        });

        assertEquals(Set.of("nord"), pools.keySet());
        assertEquals(List.of("kaputt"), reported);
        assertTrue(reasons.getFirst().contains(broken.toString()));
    }

    @Test
    void poolWithBrokenValuesIsSkippedAndReported(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", Set.of(NORTH_PAGE));
        Path directory = root.resolve("pages");
        Files.writeString(directory.resolve("ohne_seite.json"),
                "{\"pages\": [{\"position\": {\"x\": 1, \"y\": 2, \"z\": 3}, \"face\": null}]}");
        Files.writeString(directory.resolve("text_statt_zahl.json"),
                "{\"pages\": [{\"position\": {\"x\": \"abc\", \"y\": 2, \"z\": 3}, \"face\": \"north\"}]}");
        List<String> reported = new ArrayList<>();

        Map<String, Set<PageResource>> pools = PagePoolFiles.load(mapFile, (name, _) -> reported.add(name));

        assertEquals(Set.of("nord"), pools.keySet());
        assertEquals(List.of("ohne_seite", "text_statt_zahl"), reported);
    }

    @Test
    void defaultPoolFileIsLoaded(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, PagePoolFiles.DEFAULT_POOL, Set.of(NORTH_PAGE));

        assertEquals(Map.of("default", Set.of(NORTH_PAGE)), PagePoolFiles.load(mapFile, (_, _) -> {}));
    }

    @Test
    void fileWithInvalidNameIsSkippedAndReported(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Path directory = root.resolve("pages");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("Default.json"), "{ \"pages\": [] }");
        Files.writeString(directory.resolve("a b.json"), "{ \"pages\": [] }");
        List<String> reported = new ArrayList<>();

        Map<String, Set<PageResource>> pools = PagePoolFiles.load(mapFile, (name, _) -> reported.add(name));

        assertTrue(pools.isEmpty());
        assertEquals(Set.of("Default", "a b"), Set.copyOf(reported));
    }

    @Test
    void nonJsonFilesAreIgnored(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Path directory = root.resolve("pages");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("notes.txt"), "not a pool");
        List<String> reported = new ArrayList<>();

        assertTrue(PagePoolFiles.load(mapFile, (name, _) -> reported.add(name)).isEmpty());
        assertTrue(reported.isEmpty());
    }

    @Test
    void deleteRemovesThePoolFile(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PagePoolFiles.save(mapFile, "nord", Set.of(NORTH_PAGE));

        PagePoolFiles.delete(mapFile, "nord");

        assertFalse(Files.exists(root.resolve("pages").resolve("nord.json")));
    }

    @Test
    void deleteOfAMissingPoolDoesNothing(@TempDir Path root) {
        PagePoolFiles.delete(root.resolve("map.json"), "nord");

        assertFalse(Files.exists(root.resolve("pages")));
    }
}
