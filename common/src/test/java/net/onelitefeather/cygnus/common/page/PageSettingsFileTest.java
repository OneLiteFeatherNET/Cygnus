package net.onelitefeather.cygnus.common.page;

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

class PageSettingsFileTest {

    private static final PageResource PAGE = new PageResource(new Vec(1, 2, 3), Direction.NORTH);

    @Test
    void resolvesToASiblingPagesJson(@TempDir Path root) {
        assertEquals(root.resolve("pages.json"), PageSettingsFile.resolve(root.resolve("map.json")));
    }

    @Test
    void missingFileMeansDefaultSettings(@TempDir Path root) {
        assertEquals(PageSettings.DEFAULT, PageSettingsFile.loadSettings(root.resolve("map.json")));
    }

    @Test
    void savedSettingsComeBack(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PageSettings settings = new PageSettings(30, 6, 90, 20);

        PageSettingsFile.save(mapFile, settings);

        assertEquals(settings, PageSettingsFile.loadSettings(mapFile));
    }

    @Test
    void savedFileHoldsOnlyTheSettings(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");

        PageSettingsFile.save(mapFile, PageSettings.DEFAULT);

        String content = Files.readString(PageSettingsFile.resolve(mapFile), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"settings\""), content);
        assertFalse(content.contains("\"pages\""), content);
    }

    @Test
    void oldArrayFileUsesTheDefaults(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        // The layout every map had before settings: the bare list.
        String oldLayout = GsonHelper.GSON.toJson(Set.of(PAGE), TypeToken.getParameterized(Set.class, PageResource.class).getType());
        Files.writeString(PageSettingsFile.resolve(mapFile), oldLayout, StandardCharsets.UTF_8);

        assertEquals(PageSettings.DEFAULT, PageSettingsFile.loadSettings(mapFile));
    }

    @Test
    void oldFileWithPagesKeepsItsSettings(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Files.writeString(PageSettingsFile.resolve(mapFile),
                "{ \"pages\": [ { \"face\": \"NORTH\", \"position\": { \"x\": 1.0, \"y\": 2.0, \"z\": 3.0 } } ],"
                        + " \"settings\": { \"proximityRange\": 12 } }", StandardCharsets.UTF_8);

        assertEquals(new PageSettings(12, 4, 60, 15), PageSettingsFile.loadSettings(mapFile));
    }

    @Test
    void missingKeysTakeTheirDefault(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Files.writeString(PageSettingsFile.resolve(mapFile),
                "{ \"settings\": { \"proximityRange\": 12 } }", StandardCharsets.UTF_8);

        assertEquals(new PageSettings(12, 4, 60, 15), PageSettingsFile.loadSettings(mapFile));
    }

    @Test
    void invalidSettingsNameFileAndField(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Files.writeString(PageSettingsFile.resolve(mapFile),
                "{ \"settings\": { \"ttlSeconds\": 5 } }", StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> PageSettingsFile.loadSettings(mapFile));
        assertTrue(exception.getMessage().contains("pages.json"), exception.getMessage());
        assertTrue(exception.getMessage().contains("ttlSeconds"), exception.getMessage());
    }

    @Test
    void invalidSettingsFallBackToTheDefaultsForTheSetup(@TempDir Path root) throws IOException {
        Path mapFile = root.resolve("map.json");
        Files.writeString(PageSettingsFile.resolve(mapFile),
                "{ \"settings\": { \"ttlSeconds\": 5 } }", StandardCharsets.UTF_8);
        GameMap map = new GameMap("Forest", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);
        List<String> reasons = new ArrayList<>();

        GameMap loaded = PageSettingsFile.loadInto(mapFile, map, reasons::add);

        assertEquals(PageSettings.DEFAULT, loaded.getPageSettings());
        assertTrue(reasons.getFirst().contains("ttlSeconds"), reasons.getFirst());
        assertThrows(IllegalArgumentException.class, () -> PageSettingsFile.loadInto(mapFile, map),
                "the game still refuses broken settings");
    }

    @Test
    void loadIntoSetsTheSettingsAndLeavesThePages(@TempDir Path root) {
        Path mapFile = root.resolve("map.json");
        PageSettings settings = new PageSettings(30, 6, 90, 20);
        PageSettingsFile.save(mapFile, settings);
        GameMap map = new GameMap("Test", Pos.ZERO, Pos.ZERO, Set.of(PAGE), Set.of(), List.of(), null);

        GameMap loaded = PageSettingsFile.loadInto(mapFile, map);

        assertEquals(settings, loaded.getPageSettings());
        assertEquals(Set.of(PAGE), loaded.getPageFaces());
    }
}
