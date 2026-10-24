package net.onelitefeather.cygnus.setup.data;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekRoutesFile;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import net.onelitefeather.cygnus.common.page.PageFacesFile;
import net.onelitefeather.cygnus.setup.item.SetupItemId;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class GameDataTest {

    private Instance instance;
    private Player player;
    private GameData gameData;

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
        this.player = env.createPlayer(this.instance);
        this.gameData = new GameData(this.player, MapEntry.of(Paths.get("")));
    }

    @AfterEach
    void tearDown(Env env) {
        env.destroyInstance(this.instance, true);
    }

    @Test
    void testAddAndRemovePage() {
        assertTrue(gameData.addPage(Vec.ZERO, Direction.NORTH));
        assertFalse(gameData.addPage(Vec.ZERO, Direction.NORTH));
        assertTrue(gameData.addPage(Vec.ZERO, Direction.SOUTH));
    }

    @Test
    void testSwapSurvivorMode() {
        assertFalse(gameData.hasSurvivorMode());
        gameData.swapSurvivorMode();
        assertTrue(gameData.hasSurvivorMode());
        gameData.swapSurvivorMode();
        assertFalse(gameData.hasSurvivorMode());
    }

    @Test
    void testSetAndRemoveSurvivorPosition() {
        player.teleport(new Pos(10.5, 64.0, -15.5, 90.0f, 45.0f));
        gameData.setPosition(MapDataCategory.SURVIVOR, player);

        Pos expectedSpawn = new Pos(10.5, 64.0, -15.5, 90.0f, 0.0f);
        GameMap mapWithSpawn = (GameMap) gameData.getMapBuilder().build();
        assertEquals(List.of(expectedSpawn), List.copyOf(mapWithSpawn.getSurvivorSpawns()), "the spawn keeps the yaw but looks straight ahead");

        gameData.handleDataContextDelete(MapDataCategory.SURVIVOR, expectedSpawn);
        GameMap mapAfterDelete = (GameMap) gameData.getMapBuilder().build();
        assertTrue(mapAfterDelete.getSurvivorSpawns().isEmpty());
    }

    @Test
    void saveWritesPageFacesToTheSiblingFileAndLeavesThemOutOfMapJson(@TempDir Path root) throws IOException {
        GameData data = new GameData(player, MapEntry.of(root));
        data.addPage(Vec.ZERO, Direction.NORTH);
        data.save();

        String mapJson = Files.readString(root.resolve("map.json"), StandardCharsets.UTF_8);
        assertFalse(mapJson.contains("pageFaces"));
        assertTrue(Files.exists(root.resolve("pages.json")));
    }

    @Test
    void loadDataOpensAMapWithBrokenSettingsOnTheDefaults(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("map.json"), "{ \"name\": \"Test\" }", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("pages.json"),
                "{ \"pages\": [], \"settings\": { \"ttlSeconds\": 5 } }", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("creek.json"),
                "{ \"routes\": [], \"settings\": { \"sightRange\": 200 } }", StandardCharsets.UTF_8);

        GameMapBuilder builder = (GameMapBuilder) new GameData(player, MapEntry.of(root)).getMapBuilder();

        assertEquals(PageSettings.DEFAULT, builder.getPageSettings());
        assertEquals(CreekSettings.DEFAULT, builder.getCreekSettings());
    }

    @Test
    void loadDataReadsPageFacesBackFromTheSiblingFile(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("map.json"), "{ \"name\": \"Test\" }", StandardCharsets.UTF_8);
        Files.writeString(
                root.resolve("pages.json"),
                "[ { \"face\": \"NORTH\", \"position\": { \"x\": 1.0, \"y\": 2.0, \"z\": 3.0 } } ]",
                StandardCharsets.UTF_8
        );

        GameData data = new GameData(player, MapEntry.of(root));
        assertEquals(1, ((GameMapBuilder) data.getMapBuilder()).getPageFaces().size());
    }

    @Test
    void testCreekRouteManagement() {
        assertFalse(gameData.addCreekPoint(Vec.ZERO), "no active route yet");
        assertTrue(gameData.createCreekRoute("Waldweg"));
        assertFalse(gameData.createCreekRoute("Waldweg"));
        assertEquals("Waldweg", gameData.activeCreekRoute());

        assertTrue(gameData.addCreekPoint(new Vec(1, 80, 1)));
        assertTrue(gameData.addCreekPoint(new Vec(5, 80, 1)));
        assertEquals(2, gameData.activeCreekPointCount());
        assertTrue(gameData.removeLastCreekPoint());
        assertEquals(1, gameData.activeCreekPointCount());

        assertTrue(gameData.createCreekRoute("Lichtung"));
        assertTrue(gameData.selectCreekRoute("Waldweg"));
        assertFalse(gameData.selectCreekRoute("Unknown"));
        assertTrue(gameData.deleteCreekRoute("Waldweg"));
        assertNull(gameData.activeCreekRoute(), "deleting the active route clears the selection");
    }

    @Test
    void saveAndLoadKeepAnUnfinishedCreekRoute(@TempDir Path root) {
        MapEntry mapEntry = MapEntry.of(root);
        GameData data = new GameData(player, mapEntry);
        data.createCreekRoute("Stub");
        data.addCreekPoint(new Vec(1, 80, 1));
        data.save();

        GameData reloaded = new GameData(player, mapEntry);
        assertEquals(List.of(CreekRoute.ofPositions("Stub", List.of(new Vec(1, 80, 1)))),
                ((GameMapBuilder) reloaded.getMapBuilder()).getCreekRoutes());
        assertTrue(Files.exists(CreekRoutesFile.resolve(root.resolve("map.json"))));
    }

    @Test
    void testCreekRouteItemTogglesTheMode() {
        assertFalse(gameData.hasCreekRouteMode());
        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);
        assertTrue(gameData.hasCreekRouteMode());
        gameData.handleItemInteraction(player, SetupItemId.CREEK_LEAVE);
        assertFalse(gameData.hasCreekRouteMode());
    }

    @Test
    void testCreekRouteModeStaysOffWhileAnotherModeIsActive() {
        gameData.swapPageMode();
        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);
        assertFalse(gameData.hasCreekRouteMode());
    }

    @Test
    void testCreekRoutePauses() {
        assertFalse(gameData.setCreekStartPause(1000), "no active route");
        gameData.createCreekRoute("Waldweg");
        assertFalse(gameData.setCreekStartPause(1000), "no point yet");
        gameData.addCreekPoint(new Vec(1, 80, 1));
        assertTrue(gameData.setCreekStartPause(1000));
        assertFalse(gameData.setCreekEndPause(2000), "the end needs two points");
        gameData.addCreekPoint(new Vec(5, 80, 1));
        assertTrue(gameData.setCreekEndPause(2000));

        List<CreekWaypoint> points = ((GameMapBuilder) gameData.getMapBuilder()).getCreekRoutePoints("Waldweg");
        assertEquals(1000, points.getFirst().pauseMillis());
        assertEquals(2000, points.getLast().pauseMillis());
    }

    @Test
    void testFinishCreekRoute() {
        assertFalse(gameData.finishCreekRoute(), "nothing to finish yet");
        gameData.createCreekRoute("Waldweg");
        gameData.addCreekPoint(new Vec(1, 80, 1));
        assertTrue(gameData.finishCreekRoute());
        assertNull(gameData.activeCreekRoute());
        assertTrue(((GameMapBuilder) gameData.getMapBuilder()).hasCreekRoute("Waldweg"), "finishing keeps the route");
    }

    @Test
    void testFinishItemSwitchesBackToTheOverview() {
        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);
        gameData.createCreekRoute("Waldweg");
        gameData.handleItemInteraction(player, SetupItemId.CREEK_FINISH);
        assertNull(gameData.activeCreekRoute());
        assertEquals(SetupItemId.CREEK_NEW, player.getInventory().getItemStack(0).getTag(Tags.ITEM_TAG).byteValue());
    }

    @Test
    void testLeavingTheCreekRouteModeFinishesTheRoute() {
        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);
        gameData.createCreekRoute("Waldweg");
        gameData.handleItemInteraction(player, SetupItemId.CREEK_LEAVE);
        assertFalse(gameData.hasCreekRouteMode());
        assertNull(gameData.activeCreekRoute());
    }

    @Test
    void testRemoveCreekPointAt() {
        assertEquals(-1, gameData.removeCreekPointAt(new Vec(5, 80, 1)), "no active route");
        gameData.createCreekRoute("Waldweg");
        gameData.addCreekPoint(new Vec(1, 80, 1));
        gameData.addCreekPoint(new Vec(5, 80, 1));
        gameData.addCreekPoint(new Vec(9, 80, 1));
        assertEquals(2, gameData.removeCreekPointAt(new Vec(5, 80, 1)));
        assertEquals(2, gameData.activeCreekPointCount());
        assertEquals(-1, gameData.removeCreekPointAt(new Vec(5, 80, 1)), "already gone");
    }

    @Test
    void saveAndLoadKeepTheMapSettings(@TempDir Path root) {
        MapEntry mapEntry = MapEntry.of(root);
        GameData data = new GameData(player, mapEntry);
        PageSettings pages = new PageSettings(30, 6, 90, 20);
        CreekSettings creek = new CreekSettings(30, 18, 28, 20, 2.0D, 10, 0.0D);
        GameMapBuilder builder = (GameMapBuilder) data.getMapBuilder();
        builder.setPageSettings(pages);
        builder.setCreekSettings(creek);
        data.save();

        GameMapBuilder reloaded = (GameMapBuilder) new GameData(player, mapEntry).getMapBuilder();
        assertEquals(pages, reloaded.getPageSettings());
        assertEquals(creek, reloaded.getCreekSettings());
    }

    @Test
    void savingWithoutTouchingTheSettingsKeepsThem(@TempDir Path root) {
        MapEntry mapEntry = MapEntry.of(root);
        Path mapFile = root.resolve("map.json");
        new GameData(player, mapEntry).save();
        PageSettings pages = new PageSettings(30, 6, 90, 20);
        CreekSettings creek = new CreekSettings(30, 18, 28, 20, 2.0D, 10, 0.0D);
        PageFacesFile.save(mapFile, Set.of(), pages);
        CreekRoutesFile.save(mapFile, List.of(), creek);

        GameData second = new GameData(player, mapEntry);
        second.addPage(Vec.ZERO, Direction.NORTH);
        second.save();

        assertEquals(pages, PageFacesFile.loadSettings(mapFile), "saving a page must not reset the map's page settings");
        assertEquals(creek, CreekRoutesFile.loadSettings(mapFile));
    }
}
