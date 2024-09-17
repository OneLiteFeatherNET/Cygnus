package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class MapSetupSelectEventTest {

    @Test
    void testMapSetupSelectEventCreation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        MapEntry mapEntry = new MapEntry(Paths.get(""));

        MapSetupSelectEvent event = new MapSetupSelectEvent(player, mapEntry, SetupMode.LOBBY);

        assertEquals(player.getUuid(), event.getPlayer().getUuid());
        assertFalse(event.isCancelled());
        assertEquals(mapEntry, event.getMapEntry());

        event.setCancelled(true);

        assertTrue(event.isCancelled());

        env.destroyInstance(instance, true);
    }
}
