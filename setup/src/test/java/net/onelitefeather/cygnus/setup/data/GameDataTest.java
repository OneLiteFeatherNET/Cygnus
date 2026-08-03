package net.onelitefeather.cygnus.setup.data;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MicrotusExtension.class)
class GameDataTest {

    @Test
    void testGameDataCreation(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(Paths.get(""));

        GameData gameData = new GameData(player, mapEntry);

        assertNotNull(gameData);

        env.destroyInstance(instance, true);
    }

    @Test
    void testAddAndRemovePage(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(Paths.get(""));

        GameData gameData = new GameData(player, mapEntry);

        gameData.addPage(Vec.ZERO, Direction.NORTH);
        assertNotNull(gameData);

        env.destroyInstance(instance, true);
    }
}
