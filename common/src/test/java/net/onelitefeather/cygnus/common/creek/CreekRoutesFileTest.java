package net.onelitefeather.cygnus.common.creek;

import com.google.gson.JsonSyntaxException;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekRoutesFileTest {

    private static final CreekRoute NORTH = CreekRoute.ofPositions("Waldweg Nord",
            List.of(new Vec(12.5, 80, 4.5), new Vec(20.5, 80, 9.5), new Vec(31.5, 81, 15.5)));
    private static final CreekRoute SOUTH = CreekRoute.ofPositions("Waldweg Süd",
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
                  { "name": " ", "points": [ { "position": { "x": 0, "y": 80, "z": 0 } }, { "position": { "x": 5, "y": 80, "z": 0 } } ] },
                  { "name": "Stub", "points": [ { "position": { "x": 0, "y": 80, "z": 0 } } ] },
                  { "name": "Waldweg Süd", "points": [ { "position": { "x": 0.5, "y": 80, "z": 0.5 } }, { "position": { "x": 0.5, "y": 80, "z": 10.5 } } ] }
                ]
                """, StandardCharsets.UTF_8);

        assertEquals(List.of(SOUTH), CreekRoutesFile.load(mapFile(root)));
    }

    @Test
    @DisplayName("Only invalid routes leave nothing to walk")
    void onlyInvalidRoutesMeansNoRoutes(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), """
                [ { "name": "Stub", "points": [ { "position": { "x": 0, "y": 80, "z": 0 } } ] } ]
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
        CreekRoute stub = CreekRoute.ofPositions("Stub", List.of(new Vec(0, 80, 0)));
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

    @Test
    @DisplayName("A point without a pause loads with no pause")
    void oldFileHasNoPauses(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), """
                [ { "name": "Alt", "points": [ { "position": { "x": 0.5, "y": 80, "z": 0.5 } }, { "position": { "x": 0.5, "y": 80, "z": 10.5 } } ] } ]
                """, StandardCharsets.UTF_8);

        List<CreekRoute> routes = CreekRoutesFile.load(mapFile(root));

        assertEquals(1, routes.size());
        assertTrue(routes.getFirst().points().stream().allMatch(point -> point.pauseMillis() == 0));
    }

    @Test
    @DisplayName("A route with a point from before positions were nested is skipped, not a crash")
    void flatPointIsSkipped(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), """
                [
                  { "name": "Flach", "points": [ { "x": 0.5, "y": 80, "z": 0.5 }, { "x": 0.5, "y": 80, "z": 10.5 } ] },
                  { "name": "Waldweg Süd", "points": [ { "position": { "x": 0.5, "y": 80, "z": 0.5 } }, { "position": { "x": 0.5, "y": 80, "z": 10.5 } } ] }
                ]
                """, StandardCharsets.UTF_8);

        assertEquals(List.of(SOUTH), CreekRoutesFile.load(mapFile(root)));
    }

    @Test
    @DisplayName("Pauses survive saving and loading")
    void pausesAreKept(@TempDir Path root) {
        CreekRoute route = new CreekRoute("Pausen", List.of(
                CreekWaypoint.of(new Vec(0.5, 80, 0.5)).withPause(2000),
                CreekWaypoint.of(new Vec(5.5, 80, 0.5)),
                CreekWaypoint.of(new Vec(9.5, 80, 0.5)).withPause(4000)));
        CreekRoutesFile.save(mapFile(root), List.of(route));

        assertEquals(List.of(route), CreekRoutesFile.load(mapFile(root)));
    }

    @Test
    @DisplayName("A route with a negative pause is skipped")
    void negativePauseIsSkipped(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), """
                [
                  { "name": "Kaputt", "points": [ { "position": { "x": 0, "y": 80, "z": 0 }, "pauseMillis": -5 }, { "position": { "x": 5, "y": 80, "z": 0 } } ] },
                  { "name": "Waldweg Süd", "points": [ { "position": { "x": 0.5, "y": 80, "z": 0.5 } }, { "position": { "x": 0.5, "y": 80, "z": 10.5 } } ] }
                ]
                """, StandardCharsets.UTF_8);

        assertEquals(List.of(SOUTH), CreekRoutesFile.load(mapFile(root)));
    }

    @Test
    @DisplayName("A waypoint knows its position")
    void waypointPosition() {
        CreekWaypoint point = CreekWaypoint.of(new Vec(1.5, 80, -2.5));

        assertEquals(new Vec(1.5, 80, -2.5), point.position());
        assertEquals(0, point.pauseMillis());
        assertEquals(3000, point.withPause(3000).pauseMillis());
    }

    @Test
    @DisplayName("An old file is the bare list: its routes load, the settings are the defaults")
    void oldArrayFileUsesTheDefaults(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"), """
                [ { "name": "A", "points": [ { "position": { "x": 0.5, "y": 80, "z": 0.5 } }, { "position": { "x": 0.5, "y": 80, "z": 9.5 } } ] } ]
                """, StandardCharsets.UTF_8);

        assertEquals(1, CreekRoutesFile.load(mapFile(root)).size());
        assertEquals(CreekSettings.DEFAULT, CreekRoutesFile.loadSettings(mapFile(root)));
    }

    @Test
    @DisplayName("Saved settings come back next to the routes")
    void savedSettingsComeBack(@TempDir Path root) {
        CreekSettings settings = new CreekSettings(30, 18, 28, 20, 2.0D, 10, 0.0D);

        CreekRoutesFile.save(mapFile(root), List.of(NORTH, SOUTH), settings);

        assertEquals(List.of(NORTH, SOUTH), CreekRoutesFile.load(mapFile(root)));
        assertEquals(settings, CreekRoutesFile.loadSettings(mapFile(root)));
    }

    @Test
    @DisplayName("A launch height of 0 stays 0 and does not fall back to 5")
    void zeroLaunchHeightStays(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"),
                "{ \"routes\": [], \"settings\": { \"launchHeight\": 0 } }", StandardCharsets.UTF_8);

        assertEquals(0.0D, CreekRoutesFile.loadSettings(mapFile(root)).launchHeight());
    }

    @Test
    @DisplayName("Keys the file leaves out take their default")
    void missingKeysTakeTheirDefault(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"),
                "{ \"routes\": [], \"settings\": { \"sightRange\": 30 } }", StandardCharsets.UTF_8);

        assertEquals(new CreekSettings(30, 20, 35, 30, 3.0D, 15, 5.0D), CreekRoutesFile.loadSettings(mapFile(root)));
    }

    @Test
    @DisplayName("Settings that contradict each other name the file and the field")
    void contradictionNamesFileAndField(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"),
                "{ \"routes\": [], \"settings\": { \"personalSpace\": 25 } }", StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CreekRoutesFile.loadSettings(mapFile(root)));
        assertTrue(exception.getMessage().contains("creek.json"), exception.getMessage());
        assertTrue(exception.getMessage().contains("personalSpace"), exception.getMessage());
    }

    @Test
    @DisplayName("For the setup, broken settings fall back to the defaults and name the reason")
    void brokenSettingsFallBackForTheSetup(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("creek.json"),
                "{ \"routes\": [], \"settings\": { \"personalSpace\": 25 } }", StandardCharsets.UTF_8);
        List<String> reasons = new ArrayList<>();

        assertEquals(CreekSettings.DEFAULT, CreekRoutesFile.loadSettingsOrDefaults(mapFile(root), reasons::add));
        assertTrue(reasons.getFirst().contains("personalSpace"), reasons.getFirst());
    }

    @Test
    @DisplayName("loadInto sets routes and settings")
    void loadIntoSetsTheSettings(@TempDir Path root) {
        CreekSettings settings = new CreekSettings(30, 18, 28, 20, 2.0D, 10, 0.0D);
        CreekRoutesFile.save(mapFile(root), List.of(NORTH), settings);
        GameMap map = new GameMap("Test", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);

        GameMap loaded = CreekRoutesFile.loadInto(mapFile(root), map);

        assertEquals(List.of(NORTH), loaded.getCreekRoutes());
        assertEquals(settings, loaded.getCreekSettings());
    }
}
