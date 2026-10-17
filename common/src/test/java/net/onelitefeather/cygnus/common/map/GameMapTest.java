package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
        GameMap gameMap = new GameMap(name, spawnPos, slenderPos, pages, survivorPos, List.of("Test"), null);

        assertNotNull(gameMap);
        assertEquals(name, gameMap.name());
        assertEquals(spawnPos, gameMap.spawn());
        assertEquals(slenderPos, gameMap.getSlenderSpawn());
        assertTrue(gameMap.getPageFaces().isEmpty());
        assertEquals(survivorPos, gameMap.getSurvivorSpawns());
        assertEquals(List.of("Test"), gameMap.builders());
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

        assertTrue(mapBuilder.addPage(Vec.ZERO, Direction.NORTH));

        assertFalse(mapBuilder.getPageFaces().isEmpty());

        PageResource pageResource = mapBuilder.getPageFaces().iterator().next();

        assertEquals(Vec.ZERO, pageResource.position());
        assertEquals(Direction.NORTH, pageResource.face());
    }

    @Test
    void testDuplicatePageHandling() {
        GameMapBuilder mapBuilder = new GameMapBuilder();
        assertTrue(mapBuilder.addPage(Vec.ZERO, Direction.NORTH));
        assertFalse(mapBuilder.addPage(Vec.ZERO, Direction.NORTH));
        assertTrue(mapBuilder.addPage(Vec.ZERO, Direction.EAST));
        assertEquals(2, mapBuilder.getPageFaces().size());
    }

    @Test
    void testGameMapCopy() {
        Pos slenderPos = new Pos(10, 10, 10);
        Pos survivorPos = new Pos(5, 5, 5);
        PageResource pageResource = new PageResource(Vec.ZERO, Direction.SOUTH);

        GameMap originalMap = new GameMap("Map", Pos.ZERO, slenderPos, Set.of(pageResource), Set.of(survivorPos), List.of("Author"), null);
        GameMapBuilder copyBuilder = new GameMapBuilder(originalMap);

        assertEquals(1, copyBuilder.getSurvivorSpawns().size());
        assertTrue(copyBuilder.getSurvivorSpawns().contains(survivorPos));
        assertEquals(1, copyBuilder.getPageFaces().size());
        assertTrue(copyBuilder.getPageFaces().contains(pageResource));

        GameMap rebuiltMap = copyBuilder.build();
        assertEquals(originalMap.getSurvivorSpawns(), rebuiltMap.getSurvivorSpawns());
        assertEquals(originalMap.getPageFaces(), rebuiltMap.getPageFaces());
    }

    @Test
    void testGameMapCopyWithEmptyCollections() {
        GameMap mapWithEmptyCollections = new GameMap("Map", Pos.ZERO, null, Set.of(), Set.of(), List.of(), null);
        GameMapBuilder builder = new GameMapBuilder(mapWithEmptyCollections);

        assertNotNull(builder.getSurvivorSpawns());
        assertTrue(builder.getSurvivorSpawns().isEmpty());
        assertNotNull(builder.getPageFaces());
        assertTrue(builder.getPageFaces().isEmpty());
    }

    @Test
    void testWithPageFacesKeepsTheRest() {
        GameMap map = new GameMap("Forest", Pos.ZERO, new Pos(1, 2, 3), Set.of(), Set.of(new Pos(4, 5, 6)), List.of("Builder"), null);
        PageResource page = new PageResource(Vec.ZERO, Direction.NORTH);

        GameMap copy = map.withPageFaces(Set.of(page));

        assertEquals(Set.of(page), copy.getPageFaces());
        assertEquals("Forest", copy.name());
        assertEquals(new Pos(1, 2, 3), copy.getSlenderSpawn());
        assertEquals(Set.of(new Pos(4, 5, 6)), copy.getSurvivorSpawns());
        assertEquals(List.of("Builder"), copy.builders());
    }

    @Test
    void testSettingsDefaultWithoutAFileOfTheirOwn() {
        GameMap map = new GameMap("Test", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);

        assertEquals(PageSettings.DEFAULT, map.getPageSettings());
        assertEquals(CreekSettings.DEFAULT, map.getCreekSettings());
    }

    @Test
    void testWithSettingsKeepsTheRest() {
        PageSettings pages = new PageSettings(30, 5, 90, 20);
        CreekSettings creek = new CreekSettings(30, 18, 28, 20, 2.0D, 10, 0.0D);
        GameMap map = new GameMap("Test", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null)
                .withPageSettings(pages)
                .withCreekSettings(creek)
                .withPageFaces(Set.of())
                .withCreekRoutes(List.of());

        assertEquals(pages, map.getPageSettings(), "withPageFaces and withCreekRoutes keep the settings");
        assertEquals(creek, map.getCreekSettings());
        assertEquals("Test", map.name());
    }

    @Test
    void testBuilderCarriesTheSettingsBothWays() {
        PageSettings pages = new PageSettings(30, 5, 90, 20);
        CreekSettings creek = new CreekSettings(30, 18, 28, 20, 2.0D, 10, 0.0D);
        GameMap map = new GameMap("Test", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null)
                .withPageSettings(pages)
                .withCreekSettings(creek);

        GameMapBuilder builder = new GameMapBuilder(map);
        assertEquals(pages, builder.getPageSettings());
        assertEquals(creek, builder.getCreekSettings());

        builder.setCreekSettings(CreekSettings.DEFAULT);
        GameMap built = builder.build();
        assertEquals(pages, built.getPageSettings());
        assertEquals(CreekSettings.DEFAULT, built.getCreekSettings());
        assertEquals(PageSettings.DEFAULT, new GameMapBuilder().getPageSettings());
    }
}
