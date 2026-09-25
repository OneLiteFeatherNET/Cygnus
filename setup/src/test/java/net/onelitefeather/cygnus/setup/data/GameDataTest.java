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
import net.onelitefeather.cygnus.setup.item.SetupItemId;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class GameDataTest {

    @Test
    void testGameDataCreation(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(Paths.get(""));

        GameData gameData = new GameData(player, mapEntry);

        assertNotNull(gameData);

        env.destroyInstance(instance, true);
    }

    @Test
    void testAddAndRemovePage(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(Paths.get(""));

        GameData gameData = new GameData(player, mapEntry);

        assertTrue(gameData.addPage(Vec.ZERO, Direction.NORTH));
        assertFalse(gameData.addPage(Vec.ZERO, Direction.NORTH));
        assertTrue(gameData.addPage(Vec.ZERO, Direction.SOUTH));

        env.destroyInstance(instance, true);
    }

    @Test
    void testSwapSurvivorMode(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(Paths.get(""));

        GameData gameData = new GameData(player, mapEntry);

        assertFalse(gameData.hasSurvivorMode());
        gameData.swapSurvivorMode();
        assertTrue(gameData.hasSurvivorMode());
        gameData.swapSurvivorMode();
        assertFalse(gameData.hasSurvivorMode());

        env.destroyInstance(instance, true);
    }

    @Test
    void testSetSurvivorPosition(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(Paths.get(""));

        GameData gameData = new GameData(player, mapEntry);

        Pos playerPosLookingDown = new Pos(10.5, 64.0, -15.5, 90.0f, 45.0f);
        player.teleport(playerPosLookingDown);

        gameData.setPosition(MapDataCategory.SURVIVOR, player);
        GameMap map = (GameMap) gameData.getMapBuilder().build();

        Pos expectedSpawn = new Pos(10.5, 64.0, -15.5, 90.0f, 0.0f);
        assertEquals(1, map.getSurvivorSpawns().size());
        assertTrue(map.getSurvivorSpawns().contains(expectedSpawn));

        env.destroyInstance(instance, true);
    }

    @Test
    void testRemoveSurvivorPosition(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(Paths.get(""));

        GameData gameData = new GameData(player, mapEntry);

        Pos playerPos = new Pos(10.5, 64.0, -15.5, 90.0f, 0.0f);
        player.teleport(playerPos);

        gameData.setPosition(MapDataCategory.SURVIVOR, player);
        GameMap mapWithSpawn = (GameMap) gameData.getMapBuilder().build();
        assertEquals(1, mapWithSpawn.getSurvivorSpawns().size());

        gameData.handleDataContextDelete(MapDataCategory.SURVIVOR, playerPos);
        GameMap mapAfterDelete = (GameMap) gameData.getMapBuilder().build();
        assertEquals(0, mapAfterDelete.getSurvivorSpawns().size());

        env.destroyInstance(instance, true);
    }

    @Test
    void saveWritesPageFacesToTheSiblingFileAndLeavesThemOutOfMapJson(Env env, @TempDir Path root) throws IOException {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(root);

        GameData gameData = new GameData(player, mapEntry);
        gameData.addPage(Vec.ZERO, Direction.NORTH);
        gameData.save();

        String mapJson = Files.readString(root.resolve("map.json"), StandardCharsets.UTF_8);
        assertFalse(mapJson.contains("pageFaces"));
        assertTrue(Files.exists(root.resolve("pages.json")));

        env.destroyInstance(instance, true);
    }

    @Test
    void loadDataReadsPageFacesBackFromTheSiblingFile(Env env, @TempDir Path root) throws IOException {
        Files.writeString(root.resolve("map.json"), "{ \"name\": \"Test\" }", StandardCharsets.UTF_8);
        Files.writeString(
                root.resolve("pages.json"),
                "[ { \"face\": \"NORTH\", \"position\": { \"x\": 1.0, \"y\": 2.0, \"z\": 3.0 } } ]",
                StandardCharsets.UTF_8
        );
        MapEntry mapEntry = MapEntry.of(root);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        GameData gameData = new GameData(player, mapEntry);

        GameMapBuilder builder = (GameMapBuilder) gameData.getMapBuilder();
        assertEquals(1, builder.getPageFaces().size());

        env.destroyInstance(instance, true);
    }

    @Test
    void testCreekRouteManagement(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));

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

        env.destroyInstance(instance, true);
    }

    @Test
    void testSwapCreekRouteMode(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));

        assertFalse(gameData.hasCreekRouteMode());
        gameData.swapCreekRouteMode();
        assertTrue(gameData.hasCreekRouteMode());

        env.destroyInstance(instance, true);
    }

    @Test
    void saveAndLoadKeepAnUnfinishedCreekRoute(Env env, @TempDir Path root) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(root);
        GameData gameData = new GameData(player, mapEntry);
        gameData.createCreekRoute("Stub");
        gameData.addCreekPoint(new Vec(1, 80, 1));

        gameData.save();
        GameData reloaded = new GameData(player, mapEntry);

        assertEquals(List.of(CreekRoute.ofPositions("Stub", List.of(new Vec(1, 80, 1)))),
                ((GameMapBuilder) reloaded.getMapBuilder()).getCreekRoutes());
        assertTrue(Files.exists(CreekRoutesFile.resolve(root.resolve("map.json"))));

        env.destroyInstance(instance, true);
    }

    @Test
    void testCreekRouteItemTogglesTheMode(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));

        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);
        assertTrue(gameData.hasCreekRouteMode());
        gameData.handleItemInteraction(player, SetupItemId.CREEK_LEAVE);
        assertFalse(gameData.hasCreekRouteMode());

        env.destroyInstance(instance, true);
    }

    @Test
    void testCreekRouteModeStaysOffWhileAnotherModeIsActive(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));
        gameData.swapPageMode();

        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);

        assertFalse(gameData.hasCreekRouteMode());
        env.destroyInstance(instance, true);
    }

    @Test
    void testCreekRoutePauses(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));

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

        env.destroyInstance(instance, true);
    }

    @Test
    void testFinishCreekRoute(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));

        assertFalse(gameData.finishCreekRoute(), "nothing to finish yet");
        gameData.createCreekRoute("Waldweg");
        gameData.addCreekPoint(new Vec(1, 80, 1));

        assertTrue(gameData.finishCreekRoute());
        assertNull(gameData.activeCreekRoute());
        assertTrue(((GameMapBuilder) gameData.getMapBuilder()).hasCreekRoute("Waldweg"), "finishing keeps the route");

        env.destroyInstance(instance, true);
    }

    @Test
    void testFinishItemSwitchesBackToTheOverview(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));
        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);
        gameData.createCreekRoute("Waldweg");

        gameData.handleItemInteraction(player, SetupItemId.CREEK_FINISH);

        assertNull(gameData.activeCreekRoute());
        assertEquals(SetupItemId.CREEK_NEW, player.getInventory().getItemStack(0).getTag(Tags.ITEM_TAG).byteValue());

        env.destroyInstance(instance, true);
    }

    @Test
    void testLeavingTheCreekRouteModeFinishesTheRoute(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));
        gameData.handleItemInteraction(player, SetupItemId.CREEK_ROUTES);
        gameData.createCreekRoute("Waldweg");

        gameData.handleItemInteraction(player, SetupItemId.CREEK_LEAVE);

        assertFalse(gameData.hasCreekRouteMode());
        assertNull(gameData.activeCreekRoute());

        env.destroyInstance(instance, true);
    }

    @Test
    void testRemoveCreekPointAt(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameData gameData = new GameData(player, MapEntry.of(Paths.get("")));

        assertEquals(-1, gameData.removeCreekPointAt(new Vec(5, 80, 1)), "no active route");
        gameData.createCreekRoute("Waldweg");
        gameData.addCreekPoint(new Vec(1, 80, 1));
        gameData.addCreekPoint(new Vec(5, 80, 1));
        gameData.addCreekPoint(new Vec(9, 80, 1));

        assertEquals(2, gameData.removeCreekPointAt(new Vec(5, 80, 1)));
        assertEquals(2, gameData.activeCreekPointCount());
        assertEquals(-1, gameData.removeCreekPointAt(new Vec(5, 80, 1)), "already gone");

        env.destroyInstance(instance, true);
    }
}
