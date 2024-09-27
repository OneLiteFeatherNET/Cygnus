package net.onelitefeather.cygnus.stamina;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class StaminaServiceTest {

    private static StaminaService staminaService;

    @BeforeAll
    static void setup() {
        staminaService = new StaminaService();
    }

    @AfterEach
    void cleanUp() {
        staminaService.cleanUp();
    }

    @AfterAll
    static void teardown() {
        staminaService = null;
    }

    @Test
    void testSetSlenderBar(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertNull(staminaService.getSlenderBar());
        staminaService.setSlenderBar(player);
        assertNotNull(staminaService.getSlenderBar());

        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderForceStop(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertNull(staminaService.getSlenderBar());
        staminaService.setSlenderBar(player);
        assertNotNull(staminaService.getSlenderBar());

        staminaService.forceStopSlenderBar();

        assertNull(staminaService.getSlenderBar());
        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderBarSwitch(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertNull(staminaService.getSlenderBar());
        staminaService.setSlenderBar(player);

        Player anotherPlayer = env.createPlayer(instance);

        StaminaBar oldBar = staminaService.getSlenderBar();

        staminaService.switchToSlenderBar(anotherPlayer);

        StaminaBar slenderBar = staminaService.getSlenderBar();

        assertNotEquals(oldBar, slenderBar);

        assertNotEquals(player.getUuid(), slenderBar.player.getUuid());
        assertEquals(anotherPlayer.getUuid(), slenderBar.player.getUuid());

        env.destroyInstance(instance, true);
    }
}
