package net.onelitefeather.cygnus.common.creek;

import com.google.gson.JsonSyntaxException;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.map.GameMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekRoutesFileTest {

    private static final CreekRoute NORTH = new CreekRoute("Waldweg Nord",
            List.of(new Vec(12.5, 80, 4.5), new Vec(20.5, 80, 9.5), new Vec(31.5, 81, 15.5)));
    private static final CreekRoute SOUTH = new CreekRoute("Waldweg Süd",
            List.of(new Vec(0.5, 80, 0.5), new Vec(0.5, 80, 10.5)));

    private static Path mapFile(Path root) {
        return root.resolve("map.json");
    }

    @Test
    @DisplayName("The file sits next to map.json")
    void resolvesToASiblingCreekJson(@TempDir Path root) {
        assertEquals(root.resolve("creek.json"), CreekRoutesFile.resolve(mapFile(root)));
    }

    @Test
    @DisplayName("Without a file there are no routes")
    void missingFileMeansNoRoutes(@TempDir Path root) {
        assertTrue(CreekRoutesFile.load(mapFile(root)).isEmpty());
    }

    @Test
    @DisplayName("Broken JSON names the file")
    void brokenJsonNamesTheFile(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), "[ { \"name\": ", StandardCharsets.UTF_8);

        JsonSyntaxException exception = assertThrows(JsonSyntaxException.class, () -> CreekRoutesFile.load(mapFile(root)));
        assertTrue(exception.getMessage().contains("creek.json"));
    }

    @Test
    @DisplayName("Saved routes come back in the same order")
    void savedRoutesCanBeLoadedBack(@TempDir Path root) {
        CreekRoutesFile.save(mapFile(root), List.of(NORTH, SOUTH));

        assertEquals(List.of(NORTH, SOUTH), CreekRoutesFile.load(mapFile(root)));
    }

    @Test
    @DisplayName("Routes without a name or with too few points are skipped")
    void invalidRoutesAreSkipped(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), """
                [
                  { "name": " ", "points": [ { "x": 0, "y": 80, "z": 0 }, { "x": 5, "y": 80, "z": 0 } ] },
                  { "name": "Stub", "points": [ { "x": 0, "y": 80, "z": 0 } ] },
                  { "name": "Waldweg Süd", "points": [ { "x": 0.5, "y": 80, "z": 0.5 }, { "x": 0.5, "y": 80, "z": 10.5 } ] }
                ]
                """, StandardCharsets.UTF_8);

        assertEquals(List.of(SOUTH), CreekRoutesFile.load(mapFile(root)));
    }

    @Test
    @DisplayName("Only invalid routes leave nothing to walk")
    void onlyInvalidRoutesMeansNoRoutes(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), """
                [ { "name": "Stub", "points": [ { "x": 0, "y": 80, "z": 0 } ] } ]
                """, StandardCharsets.UTF_8);

        assertTrue(CreekRoutesFile.load(mapFile(root)).isEmpty());
    }

    @Test
    @DisplayName("Of two routes with the same name only the first one stays")
    void duplicateNamesKeepTheFirst(@TempDir Path root) {
        CreekRoute copy = new CreekRoute("Waldweg Nord", SOUTH.points());
        CreekRoutesFile.save(mapFile(root), List.of(NORTH, copy));

        assertEquals(List.of(NORTH), CreekRoutesFile.load(mapFile(root)));
    }

    @Test
    @DisplayName("The setup still gets routes that are not finished yet")
    void uncheckedLoadKeepsIncompleteRoutes(@TempDir Path root) {
        CreekRoute stub = new CreekRoute("Stub", List.of(new Vec(0, 80, 0)));
        CreekRoutesFile.save(mapFile(root), List.of(stub));

        assertEquals(List.of(stub), CreekRoutesFile.loadUnchecked(mapFile(root)));
    }

    @Test
    @DisplayName("Names are trimmed")
    void namesAreTrimmed() {
        assertEquals("Waldweg", new CreekRoute("  Waldweg ", SOUTH.points()).name());
    }

    @Test
    @DisplayName("loadInto puts the routes into the map")
    void loadIntoSetsTheRoutes(@TempDir Path root) {
        CreekRoutesFile.save(mapFile(root), List.of(NORTH));
        GameMap map = new GameMap("Forest", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);

        assertEquals(List.of(NORTH), CreekRoutesFile.loadInto(mapFile(root), map).getCreekRoutes());
    }
}
