package net.onelitefeather.spectator.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.spectator.SpectatorService;
import net.onelitefeather.spectator.event.SpectatorQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class SpectatorQuitIntegrationTest {

    @Test
    void testSpectatorQuit(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, Pos.ZERO).join();

        SpectatorService spectatorService = SpectatorService.builder().detectSpectatorQuit().build();

        assertNotNull(spectatorService);

        spectatorService.add(player);

        assertTrue(spectatorService.hasSpectators());
        assertTrue(spectatorService.isSpectator(player));

        Collector<SpectatorQuitEvent> quitCollector = env.trackEvent(SpectatorQuitEvent.class, EventFilter.PLAYER, player);
        player.remove(true);

        quitCollector.assertSingle(event -> {
            assertEquals(player, event.getPlayer());
            assertFalse(spectatorService.isSpectator(player));
            assertFalse(spectatorService.hasSpectators());
        });

        env.destroyInstance(instance, true);
    }
}
