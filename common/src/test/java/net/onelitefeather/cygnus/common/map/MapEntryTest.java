package net.onelitefeather.cygnus.common.map;

import net.onelitefeather.cygnus.common.config.GameConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MapEntryTest {

    @TempDir(cleanup = CleanupMode.ALWAYS)
    static Path tempPath;

    @Test
    void testObjectCreation() {
        MapEntry mapEntry = new MapEntry(tempPath);
        assertNotNull(mapEntry);
        assertFalse(mapEntry.hasMapFile());

        Path mapFile = mapEntry.getMapFile();

        assertTrue(mapFile.endsWith(GameConfig.MAP_FILE_NAME));
    }
}
