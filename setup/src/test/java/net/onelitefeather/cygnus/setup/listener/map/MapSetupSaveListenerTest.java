package net.onelitefeather.cygnus.setup.listener.map;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.onelitefeather.guira.SetupDataService;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class MapSetupSaveListenerTest {

    @Test
    void testSaveRemovesDataFromDataService(Env env, @TempDir Path tempDir) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = MapEntry.of(tempDir);

        SetupDataService dataService = SetupDataService.create();
        GameData gameData = new GameData(player, mapEntry);
        dataService.add(player.getUuid(), gameData);

        assertTrue(dataService.get(player.getUuid()).isPresent());

        MapSetupSaveListener listener = new MapSetupSaveListener(dataService, p -> {});
        listener.accept(new MapSetupSaveEvent(player));

        assertFalse(dataService.get(player.getUuid()).isPresent(), "Data should be removed from dataService on save");

        env.destroyInstance(instance, true);
    }
}
