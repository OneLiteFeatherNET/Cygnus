package net.onelitefeather.spectator;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.FlexibleListener;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.spectator.event.SpectatorAddEvent;
import net.onelitefeather.spectator.event.SpectatorRemoveEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class SpectatorServiceIntegrationTest {

    static SpectatorService spectatorService;

    @BeforeAll
    static void setup() {
        spectatorService = SpectatorService.builder().build();
    }

    @AfterEach
    void tearDown() {
        spectatorService.clear();
    }

    @Test
    void testSpectatorAdd(@NotNull Env env) {
        assertFalse(spectatorService.hasSpectators());
        Instance instance = env.createFlatInstance();

        Player player = env.createPlayer(instance);
        player.setUsernameField("Bob");
        FlexibleListener<SpectatorAddEvent> listener = env.listen(SpectatorAddEvent.class);

        listener.followup(event -> {
            assertEquals("Bob", event.getPlayer().getUsername());
            assertEquals(player.getUuid(), event.getPlayer().getUuid());
        });

        spectatorService.add(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorRemove(@NotNull Env env) {
        assertFalse(spectatorService.hasSpectators());
        Instance instance = env.createFlatInstance();

        Player player = env.createPlayer(instance);
        player.setUsernameField("Bob");
        FlexibleListener<SpectatorRemoveEvent> listener = env.listen(SpectatorRemoveEvent.class);
        spectatorService.add(player);
        listener.followup(event -> {
            assertEquals("Bob", event.getPlayer().getUsername());
            assertEquals(player.getUuid(), event.getPlayer().getUuid());
        });

        spectatorService.remove(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorSetGet(@NotNull Env env) {
        assertFalse(spectatorService.hasSpectators());
        Instance instance = env.createFlatInstance();

        Player player = env.createPlayer(instance);
        player.setUsernameField("Bob");
        spectatorService.add(player);

        assertTrue(spectatorService.hasSpectators());
        assertEquals(1, spectatorService.getSpectators().size());
        assertTrue(spectatorService.getSpectators().contains(player));

        env.destroyInstance(instance, true);
    }
}