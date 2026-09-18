package net.onelitefeather.cygnus.common.page;

import com.google.gson.JsonSyntaxException;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.map.GameMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void savedPageFacesCanBeLoadedBack(@TempDir Path root) throws IOException {
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
}
