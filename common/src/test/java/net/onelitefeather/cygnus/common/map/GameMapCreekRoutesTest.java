package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import net.onelitefeather.cygnus.common.page.PageFacesFile;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapCreekRoutesTest {

    private static final CreekRoute ROUTE = CreekRoute.ofPositions("Waldweg", List.of(new Vec(0, 80, 0), new Vec(5, 80, 0)));

    private static GameMap map() {
        return new GameMap("Forest", Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);
    }

    @Test
    @DisplayName("A map built the old way has no routes")
    void oldConstructorHasNoRoutes() {
        assertTrue(map().getCreekRoutes().isEmpty());
    }

    @Test
    @DisplayName("Adding routes keeps everything else")
    void withCreekRoutesKeepsTheRest() {
        GameMap map = map().withCreekRoutes(List.of(ROUTE));

        assertEquals(List.of(ROUTE), map.getCreekRoutes());
        assertEquals("Forest", map.name());
    }

    @Test
    @DisplayName("The builder manages routes and their points")
    void builderManagesRoutes() {
        GameMapBuilder builder = new GameMapBuilder();

        assertTrue(builder.addCreekRoute("Waldweg"));
        assertFalse(builder.addCreekRoute("Waldweg"), "names are unique");
        assertFalse(builder.addCreekRoute("  "), "a name must not be empty");
        assertFalse(builder.addCreekPoint("Unknown", Vec.ZERO));
        assertTrue(builder.addCreekPoint("Waldweg", new Vec(0, 80, 0)));
        assertTrue(builder.addCreekPoint("Waldweg", new Vec(5, 80, 0)));
        assertTrue(builder.addCreekPoint("Waldweg", new Vec(9, 80, 0)));
        assertTrue(builder.removeLastCreekPoint("Waldweg"));

        assertEquals(List.of(ROUTE), builder.build().getCreekRoutes());
        assertEquals(2, builder.getCreekRoutePoints("Waldweg").size());

        assertTrue(builder.removeCreekRoute("Waldweg"));
        assertFalse(builder.hasCreekRoute("Waldweg"));
    }

    @Test
    @DisplayName("A builder made from a map copies its routes")
    void builderCopiesRoutes() {
        GameMapBuilder builder = new GameMapBuilder(map().withCreekRoutes(List.of(ROUTE)));

        assertEquals(List.of(ROUTE), builder.getCreekRoutes());
    }

    @Test
    @DisplayName("Merging page faces keeps the routes")
    void pageMergeKeepsRoutes(@TempDir Path root) {
        GameMap map = map().withCreekRoutes(List.of(ROUTE));
        PageFacesFile.save(root.resolve("map.json"), Set.of(new PageResource(Vec.ZERO, Direction.NORTH)));

        GameMap merged = PageFacesFile.loadInto(root.resolve("map.json"), map);

        assertEquals(List.of(ROUTE), merged.getCreekRoutes());
    }

    @Test
    @DisplayName("Pauses can be set on the start and the end")
    void setsEndPauses() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addCreekRoute("Waldweg");

        assertFalse(builder.setCreekEndPause("Waldweg", true, 1000), "no point yet");
        builder.addCreekPoint("Waldweg", new Vec(0, 80, 0));
        assertTrue(builder.setCreekEndPause("Waldweg", true, 1000));
        assertFalse(builder.setCreekEndPause("Waldweg", false, 2000), "the end needs two points");
        builder.addCreekPoint("Waldweg", new Vec(5, 80, 0));
        assertTrue(builder.setCreekEndPause("Waldweg", false, 2000));
        assertFalse(builder.setCreekEndPause("Waldweg", false, -1), "no negative pauses");
        assertFalse(builder.setCreekEndPause("Unknown", true, 1000));

        List<CreekWaypoint> points = builder.getCreekRoutePoints("Waldweg");
        assertEquals(1000, points.getFirst().pauseMillis());
        assertEquals(2000, points.getLast().pauseMillis());
    }

    @Test
    @DisplayName("The end pause moves with the end")
    void endPauseMovesWithTheEnd() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addCreekRoute("Waldweg");
        builder.addCreekPoint("Waldweg", new Vec(0, 80, 0));
        builder.addCreekPoint("Waldweg", new Vec(5, 80, 0));
        builder.setCreekEndPause("Waldweg", false, 2000);

        builder.addCreekPoint("Waldweg", new Vec(9, 80, 0));
        List<CreekWaypoint> grown = builder.getCreekRoutePoints("Waldweg");
        assertEquals(List.of(0, 0, 2000), grown.stream().map(CreekWaypoint::pauseMillis).toList());

        builder.removeLastCreekPoint("Waldweg");
        List<CreekWaypoint> shrunk = builder.getCreekRoutePoints("Waldweg");
        assertEquals(List.of(0, 2000), shrunk.stream().map(CreekWaypoint::pauseMillis).toList());
    }

    @Test
    @DisplayName("With one point left the start keeps its own pause")
    void startKeepsItsPause() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addCreekRoute("Waldweg");
        builder.addCreekPoint("Waldweg", new Vec(0, 80, 0));
        builder.setCreekEndPause("Waldweg", true, 1000);
        builder.addCreekPoint("Waldweg", new Vec(5, 80, 0));
        builder.setCreekEndPause("Waldweg", false, 2000);

        builder.removeLastCreekPoint("Waldweg");

        assertEquals(List.of(1000), builder.getCreekRoutePoints("Waldweg").stream().map(CreekWaypoint::pauseMillis).toList());
    }

    /** A route from (0,80,0) over (5,80,0) to (9,80,0), paused 1000 ms at the start and 2000 ms at the end. */
    private static GameMapBuilder threePointRoute() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addCreekRoute("Waldweg");
        builder.addCreekPoint("Waldweg", new Vec(0, 80, 0));
        builder.addCreekPoint("Waldweg", new Vec(5, 80, 0));
        builder.addCreekPoint("Waldweg", new Vec(9, 80, 0));
        builder.setCreekEndPause("Waldweg", true, 1000);
        builder.setCreekEndPause("Waldweg", false, 2000);
        return builder;
    }

    private static List<Vec> positions(GameMapBuilder builder) {
        return builder.getCreekRoutePoints("Waldweg").stream().map(CreekWaypoint::position).toList();
    }

    private static List<Integer> pauses(GameMapBuilder builder) {
        return builder.getCreekRoutePoints("Waldweg").stream().map(CreekWaypoint::pauseMillis).toList();
    }

    @Test
    @DisplayName("A point in the middle can be removed")
    void removesAMiddlePoint() {
        GameMapBuilder builder = threePointRoute();

        assertEquals(2, builder.removeCreekPoint("Waldweg", new Vec(5, 80, 0)));

        assertEquals(List.of(new Vec(0, 80, 0), new Vec(9, 80, 0)), positions(builder));
        assertEquals(List.of(1000, 2000), pauses(builder));
    }

    @Test
    @DisplayName("Removing the first point hands its pause to the new first point")
    void removesTheFirstPoint() {
        GameMapBuilder builder = threePointRoute();

        assertEquals(1, builder.removeCreekPoint("Waldweg", new Vec(0, 80, 0)));

        assertEquals(List.of(new Vec(5, 80, 0), new Vec(9, 80, 0)), positions(builder));
        assertEquals(List.of(1000, 2000), pauses(builder));
    }

    @Test
    @DisplayName("Removing the last point hands its pause to the new last point")
    void removesTheLastPointByPosition() {
        GameMapBuilder builder = threePointRoute();

        assertEquals(3, builder.removeCreekPoint("Waldweg", new Vec(9, 80, 0)));

        assertEquals(List.of(new Vec(0, 80, 0), new Vec(5, 80, 0)), positions(builder));
        assertEquals(List.of(1000, 2000), pauses(builder));
    }

    @Test
    @DisplayName("Without a point at that spot nothing is removed")
    void nothingToRemove() {
        GameMapBuilder builder = threePointRoute();

        assertEquals(-1, builder.removeCreekPoint("Waldweg", new Vec(3, 80, 0)));
        assertEquals(-1, builder.removeCreekPoint("Unknown", new Vec(0, 80, 0)));
        assertEquals(3, positions(builder).size());
    }
}
