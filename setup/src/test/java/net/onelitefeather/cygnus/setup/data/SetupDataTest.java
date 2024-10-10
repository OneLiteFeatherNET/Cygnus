package net.onelitefeather.cygnus.setup.data;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class SetupDataTest {

    @ParameterizedTest
    @EnumSource(SetupMode.class)
    void testSetupDataCreation(@NotNull SetupMode mode, @NotNull Env env) {
        Path testPath = Path.of("");
        Instance instance = env.createFlatInstance();
        Player testPlayer = env.createPlayer(instance);

        assertNotNull(instance);
        assertNotNull(testPlayer);

        SetupData.Builder builder = SetupData.builder();

        assertNotNull(builder);

        BaseMap baseMap = switch (mode) {
            case LOBBY -> new BaseMap();
            case GAME -> new GameMap();
        };

        SetupData data = builder
                .mapEntry(new MapEntry(testPath))
                .player(testPlayer)
                .mode(mode)
                .baseMap(baseMap)
                .build();

        Class<?> setupDataClass = switch (mode) {
            case LOBBY -> LobbyData.class;
            case GAME -> GameData.class;
        };

        assertInstanceOf(setupDataClass, data);
        assertNotNull(data);
        assertEquals(mode, data.getSetupMode());
        assertEquals(testPlayer, data.getPlayer());
        assertEquals(testPath, data.getMapEntry().path());

        env.destroyInstance(instance, true);
    }

    @Test
    void testPageModeSwap(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player testPlayer = env.createPlayer(instance);

        SetupData data = SetupData.builder()
                .player(testPlayer)
                .mode(SetupMode.GAME)
                .baseMap(new GameMap())
                .build();

        assertNotNull(data);
        assertFalse(data.hasPageMode());
        data.swapPageMode();
        assertTrue(data.hasPageMode());

        env.destroyInstance(instance, true);
    }
}