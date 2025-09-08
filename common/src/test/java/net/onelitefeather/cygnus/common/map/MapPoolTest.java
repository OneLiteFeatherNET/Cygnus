package net.onelitefeather.cygnus.common.map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MapPoolTest {

    @TempDir
    static Path tempPath;

    @Test
    void testMapPoolUsageWithoutMapEntries() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new MapPool(tempPath),
                "The lobby map can't be null"
        );
    }

    @Disabled("Investigate why lobby is not found")
    @Test
    void testMapPoolWithDummyMaps() {
        Path lobbyMap = null;
        Path gameMap = null;
        try {
            lobbyMap = Files.createTempDirectory(tempPath, "lobby");
            gameMap = Files.createTempDirectory(tempPath, "game");
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        assertNotNull(lobbyMap);
        MapPool mapPool = new MapPool(tempPath);
        assertFalse(mapPool.getAvailableMaps().isEmpty());

        Path lobbyMapPath = mapPool.getLobbyEntry().path();

        assertEquals(lobbyMap, lobbyMapPath);

        assertNotNull(mapPool.getMapEntry());

        assertEquals(gameMap, mapPool.getMapEntry().path());

        try {
            Files.deleteIfExists(lobbyMap);
            Files.deleteIfExists(gameMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}