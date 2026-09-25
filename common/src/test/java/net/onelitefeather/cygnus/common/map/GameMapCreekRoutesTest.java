package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
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

    private static final CreekRoute ROUTE = new CreekRoute("Waldweg", List.of(new Vec(0, 80, 0), new Vec(5, 80, 0)));

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
}
