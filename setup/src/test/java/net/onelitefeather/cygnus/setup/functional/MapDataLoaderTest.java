package net.onelitefeather.cygnus.setup.functional;

import de.icevizion.aves.map.BaseMap;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MapDataLoaderTest {

    @ParameterizedTest(name = "Test dummy map loader with setup mode {0}")
    @EnumSource(SetupMode.class)
    void testDummyMapLoader(@NotNull SetupMode setupMode) {
        Path testPath = Path.of("");
        BaseMap baseMap = testMapLoader(testPath, setupMode);

        assertNotNull(baseMap);

        Class<?> mapClass = switch (setupMode) {
            case LOBBY -> BaseMap.class;
            case GAME -> GameMap.class;
        };

        assertInstanceOf(mapClass, baseMap);
    }

    private @NotNull BaseMap testMapLoader(@NotNull Path path, @NotNull SetupMode setupMode) {
        return switch (setupMode) {
            case LOBBY -> new BaseMap();
            case GAME -> new GameMap();
        };
    }
}