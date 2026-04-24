package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
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
        GameMapBuilder mapBuilder = new GameMapBuilder();
        assertNotNull(mapBuilder);

        Pos testPos = new Pos(1, 1, 1);

        assertTrue(mapBuilder.getSurvivorSpawns().isEmpty());
        assertTrue(mapBuilder.addSurvivorSpawn(testPos));
        assertFalse(mapBuilder.getSurvivorSpawns().isEmpty());
        assertEquals(1, mapBuilder.getSurvivorSpawns().size());
        assertEquals(testPos, mapBuilder.getSurvivorSpawns().iterator().next());

        assertTrue(mapBuilder.removeSurvivorSpawn(testPos));
        assertTrue(mapBuilder.getSurvivorSpawns().isEmpty());
        assertFalse(mapBuilder.removeSurvivorSpawn(testPos));
    }

    @Disabled("The function will be reimplemented in a future update")
    @Test
    void testHasEnoughSurvivorSpawns() {
        GameMapBuilder mapBuilder = new GameMapBuilder();
        assertNotNull(mapBuilder);
        assertFalse(mapBuilder.hasEnoughSurvivorSpawns());
        for (int i = 0; i <= 12; i++) {
            mapBuilder.addSurvivorSpawn(new Pos(i, i, i));
        }
        assertTrue(mapBuilder.hasEnoughSurvivorSpawns());
    }

    @Test
    void testSlenderSpawnHandling() {
        GameMapBuilder mapBuilder = new GameMapBuilder();
        assertNotNull(mapBuilder);
        mapBuilder.setSlenderSpawn(Pos.ZERO);
        assertEquals(Pos.ZERO, mapBuilder.getSlenderSpawn());
        mapBuilder.setSlenderSpawn(null);
        assertNull(mapBuilder.getSlenderSpawn());
    }

    @Test
    void testPageFaceHandling() {
        GameMapBuilder mapBuilder = new GameMapBuilder();
        assertNotNull(mapBuilder);
        assertTrue(mapBuilder.getPageFaces().isEmpty());

        mapBuilder.addPage(Vec.ZERO, Direction.NORTH);

        assertFalse(mapBuilder.getPageFaces().isEmpty());

        PageResource pageResource = mapBuilder.getPageFaces().iterator().next();

        assertEquals(Vec.ZERO, pageResource.position());
        assertEquals(Direction.NORTH, pageResource.face());
    }
}
