package net.onelitefeather.cygnus.common.map.adapter;

import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapAdapterTest {

    @Test
    void testDeserializeCompleteMap() {
        String json = """
                {
                  "name": "Forest",
                  "spawn": { "x": 0.0, "y": 64.0, "z": 0.0, "yaw": 0.0, "pitch": 0.0 },
                  "slenderSpawn": { "x": 10.0, "y": 64.0, "z": 10.0, "yaw": 0.0, "pitch": 0.0 },
                  "pageFaces": [],
                  "survivorSpawns": [],
                  "builders": ["Builder1"]
                }
                """;

        GameMap gameMap = GsonHelper.GSON.fromJson(json, GameMap.class);

        assertNotNull(gameMap);
        assertEquals("Forest", gameMap.name());
        assertNotNull(gameMap.spawn());
        assertNotNull(gameMap.getSlenderSpawn());
        assertNotNull(gameMap.getPageFaces());
        assertTrue(gameMap.getPageFaces().isEmpty());
        assertNotNull(gameMap.getSurvivorSpawns());
        assertTrue(gameMap.getSurvivorSpawns().isEmpty());
        assertEquals(1, gameMap.builders().size());
    }

    @Test
    void testDeserializeIncompleteMapWithMissingFields() {
        String json = """
                {
                  "name": "IncompleteMap"
                }
                """;

        GameMap gameMap = GsonHelper.GSON.fromJson(json, GameMap.class);

        assertNotNull(gameMap);
        assertEquals("IncompleteMap", gameMap.name());
        assertNull(gameMap.getSlenderSpawn());
        assertNotNull(gameMap.getPageFaces());
        assertTrue(gameMap.getPageFaces().isEmpty());
        assertNotNull(gameMap.getSurvivorSpawns());
        assertTrue(gameMap.getSurvivorSpawns().isEmpty());
        assertNotNull(gameMap.builders());
        assertTrue(gameMap.builders().isEmpty());
    }

    @Test
    void testDeserializeMapWithNullCollections() {
        String json = """
                {
                  "name": "NullCollectionsMap",
                  "pageFaces": null,
                  "survivorSpawns": null,
                  "builders": null
                }
                """;

        GameMap gameMap = GsonHelper.GSON.fromJson(json, GameMap.class);

        assertNotNull(gameMap);
        assertEquals("NullCollectionsMap", gameMap.name());
        assertNotNull(gameMap.getPageFaces());
        assertTrue(gameMap.getPageFaces().isEmpty());
        assertNotNull(gameMap.getSurvivorSpawns());
        assertTrue(gameMap.getSurvivorSpawns().isEmpty());
        assertNotNull(gameMap.builders());
        assertTrue(gameMap.builders().isEmpty());
    }
}
