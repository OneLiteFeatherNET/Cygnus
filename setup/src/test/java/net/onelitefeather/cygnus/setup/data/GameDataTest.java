package net.onelitefeather.cygnus.setup.data;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
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
}
