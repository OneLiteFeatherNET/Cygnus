package net.onelitefeather.cygnus.common.page;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageFacesFileTest {

    @Test
    void resolvesToASiblingPagesJson(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");

        assertEquals(root.resolve("pages.json"), PageFacesFile.resolve(mapFile));
    }

    @Test
    void loadReturnsEmptySetWhenTheSiblingFileIsMissing(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");

        assertTrue(PageFacesFile.load(mapFile).isEmpty());
    }

    @Test
    void loadNamesTheFileWhenItsJsonIsMalformed(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Path pagesFile = PageFacesFile.resolve(mapFile);
        Files.writeString(pagesFile, "{ not valid json ]");

        JsonSyntaxException exception = assertThrows(JsonSyntaxException.class, () -> PageFacesFile.load(mapFile));

        assertTrue(exception.getMessage().contains(pagesFile.toString()));
    }

    @Test
    void savedPageFacesCanBeLoadedBack(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PageResource page = new PageResource(new Vec(1, 2, 3), Direction.NORTH);

        PageFacesFile.save(mapFile, Set.of(page));

        assertTrue(Files.exists(root.resolve("pages.json")));
        assertEquals(Set.of(page), PageFacesFile.load(mapFile));
    }

    @Test
    void loadIntoMergesTheFileContentsIntoTheGivenMap(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PageResource filePage = new PageResource(new Vec(4, 5, 6), Direction.SOUTH);
        PageResource inlinePage = new PageResource(new Vec(7, 8, 9), Direction.EAST);
        PageFacesFile.save(mapFile, Set.of(filePage));
        GameMap map = new GameMap("Forest", Pos.ZERO, Pos.ZERO, Set.of(inlinePage), Set.of(), List.of(), null);

        GameMap merged = PageFacesFile.loadInto(mapFile, map);

        assertEquals(Set.of(filePage, inlinePage), merged.getPageFaces());
        assertEquals("Forest", merged.name());
    }

    @Test
    void loadIntoLeavesTheMapUnchangedWhenNoSiblingFileExists(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PageResource inlinePage = new PageResource(new Vec(1, 1, 1), Direction.UP);
        GameMap map = new GameMap("Forest", Pos.ZERO, Pos.ZERO, Set.of(inlinePage), Set.of(), List.of(), null);

        GameMap merged = PageFacesFile.loadInto(mapFile, map);

        assertEquals(Set.of(inlinePage), merged.getPageFaces());
        assertFalse(Files.exists(root.resolve("pages.json")));
    }

    @Test
    void oldArrayFileLoadsItsPagesAndTheDefaults(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Set<PageResource> faces = Set.of(new PageResource(new Vec(1, 2, 3), Direction.NORTH));
        // The layout every map had before settings: the bare list.
        String oldLayout = GsonHelper.GSON.toJson(faces, TypeToken.getParameterized(Set.class, PageResource.class).getType());
        Files.writeString(PageFacesFile.resolve(mapFile), oldLayout, StandardCharsets.UTF_8);

        assertEquals(faces, PageFacesFile.load(mapFile));
        assertEquals(PageSettings.DEFAULT, PageFacesFile.loadSettings(mapFile));
    }

    @Test
    void savedSettingsComeBack(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PageSettings settings = new PageSettings(30, 6, 90, 20);
        Set<PageResource> faces = Set.of(new PageResource(new Vec(1, 2, 3), Direction.NORTH));

        PageFacesFile.save(mapFile, faces, settings);

        assertEquals(faces, PageFacesFile.load(mapFile));
        assertEquals(settings, PageFacesFile.loadSettings(mapFile));
    }

    @Test
    void missingKeysTakeTheirDefault(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Files.writeString(PageFacesFile.resolve(mapFile),
                "{ \"pages\": [], \"settings\": { \"proximityRange\": 12 } }", StandardCharsets.UTF_8);

        assertEquals(new PageSettings(12, 4, 60, 15), PageFacesFile.loadSettings(mapFile));
    }

    @Test
    void objectWithoutSettingsUsesTheDefaults(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Files.writeString(PageFacesFile.resolve(mapFile), "{ \"pages\": [] }", StandardCharsets.UTF_8);

        assertEquals(Set.of(), PageFacesFile.load(mapFile));
        assertEquals(PageSettings.DEFAULT, PageFacesFile.loadSettings(mapFile));
    }

    @Test
    void invalidSettingsNameFileAndField(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Files.writeString(PageFacesFile.resolve(mapFile),
                "{ \"pages\": [], \"settings\": { \"ttlSeconds\": 5 } }", StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> PageFacesFile.loadSettings(mapFile));
        assertTrue(exception.getMessage().contains("pages.json"), exception.getMessage());
        assertTrue(exception.getMessage().contains("ttlSeconds"), exception.getMessage());
    }

    @Test
    void invalidSettingsFallBackToTheDefaultsForTheSetup(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Set<PageResource> faces = Set.of(new PageResource(new Vec(1, 2, 3), Direction.NORTH));
        String pages = GsonHelper.GSON.toJson(faces, TypeToken.getParameterized(Set.class, PageResource.class).getType());
        Files.writeString(PageFacesFile.resolve(mapFile),
                "{ \"pages\": " + pages + ", \"settings\": { \"ttlSeconds\": 5 } }", StandardCharsets.UTF_8);
        GameMap map = new GameMap("Forest", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);
        List<String> reasons = new ArrayList<>();

        GameMap loaded = PageFacesFile.loadInto(mapFile, map, reasons::add);

        assertEquals(faces, loaded.getPageFaces());
        assertEquals(PageSettings.DEFAULT, loaded.getPageSettings());
        assertTrue(reasons.getFirst().contains("ttlSeconds"), reasons.getFirst());
        assertThrows(IllegalArgumentException.class, () -> PageFacesFile.loadInto(mapFile, map),
                "the game still refuses broken settings");
    }

    @Test
    void missingFileMeansDefaultSettings(@TempDir Path root) {
        assertEquals(PageSettings.DEFAULT, PageFacesFile.loadSettings(root.resolve("map.json")));
    }

    @Test
    void loadIntoSetsTheSettings(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PageSettings settings = new PageSettings(30, 6, 90, 20);
        PageFacesFile.save(mapFile, Set.of(), settings);
        GameMap map = new GameMap("Test", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);

        assertEquals(settings, PageFacesFile.loadInto(mapFile, map).getPageSettings());
    }
}
