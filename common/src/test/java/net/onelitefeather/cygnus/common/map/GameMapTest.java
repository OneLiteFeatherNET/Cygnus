package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapTest {

    @Test
    void testMapCreationWithGivenData() {
        String name = "TestMap";
        Pos spawnPos = Pos.ZERO;
        Pos slenderPos = new Pos(1, 1, 1);
        Set<PageResource> pages = Set.of();
        Set<Pos> survivorPos = Set.of(new Pos(2, 2, 2), new Pos(3, 3, 3));
        GameMap gameMap = new GameMap(name, spawnPos, slenderPos, pages, survivorPos, "Test");

        assertNotNull(gameMap);
        assertEquals(name, gameMap.getName());
        assertEquals(spawnPos, gameMap.getSpawn());
        assertEquals(slenderPos, gameMap.getSlenderSpawn());
        assertTrue(gameMap.getPageFaces().isEmpty());
        assertEquals(survivorPos, gameMap.getSurvivorSpawns());
        assertArrayEquals(new String[]{"Test"}, gameMap.getBuilders());
    }

    @Test
    void testSurvivorSpawnHandling() {
        GameMap gameMap = new GameMap();
        assertNotNull(gameMap);

        Pos testPos = new Pos(1, 1, 1);

        assertTrue(gameMap.getSurvivorSpawns().isEmpty());
        assertTrue(gameMap.addSurvivorSpawn(testPos));
        assertFalse(gameMap.getSurvivorSpawns().isEmpty());
        assertEquals(1, gameMap.getSurvivorSpawns().size());
        assertEquals(testPos, gameMap.getSurvivorSpawns().iterator().next());

        assertTrue(gameMap.removeSurvivorSpawn(testPos));
        assertTrue(gameMap.getSurvivorSpawns().isEmpty());
        assertFalse(gameMap.removeSurvivorSpawn(testPos));
    }

    @Disabled("The function will be reimplemented in a future update")
    @Test
    void testHasEnoughSurvivorSpawns() {
        GameMap gameMap = new GameMap();
        assertNotNull(gameMap);
        assertFalse(gameMap.hasEnoughSurvivorSpawns());
        for (int i = 0; i <= 12; i++) {
            gameMap.addSurvivorSpawn(new Pos(i, i, i));
        }
        assertTrue(gameMap.hasEnoughSurvivorSpawns());
    }

    @Test
    void testSlenderSpawnHandling() {
        GameMap gameMap = new GameMap();
        assertNotNull(gameMap);
        gameMap.setSlenderSpawn(Pos.ZERO);
        assertEquals(Pos.ZERO, gameMap.getSlenderSpawn());
        gameMap.removeSlenderSpawn();
        assertNull(gameMap.getSlenderSpawn());
    }

    @Test
    void testPageFaceHandling() {
        GameMap gameMap = new GameMap();
        assertNotNull(gameMap);
        assertTrue(gameMap.getPageFaces().isEmpty());

        gameMap.addPage(Vec.ZERO, Direction.NORTH);

        assertFalse(gameMap.getPageFaces().isEmpty());

        PageResource pageResource = gameMap.getPageFaces().iterator().next();

        assertEquals(Vec.ZERO, pageResource.position());
        assertEquals(Direction.NORTH, pageResource.face());
    }
}
