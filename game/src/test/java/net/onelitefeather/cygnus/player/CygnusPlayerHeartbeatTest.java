package net.onelitefeather.cygnus.player;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CygnusPlayerHeartbeatTest extends CygnusPlayerTestBase {

    @Test
    void testHeartbeatInactiveAboveThreshold(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        player.setHealth(20.0f);
        player.tickHeartbeat();

        assertFalse(player.isHeartbeatActive());

        env.destroyInstance(instance, true);
    }

    @Test
    void testHeartbeatActiveBelowThreshold(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        player.setHealth(4.0f);
        player.tickHeartbeat();

        assertTrue(player.isHeartbeatActive());

        env.destroyInstance(instance, true);
    }

    @Test
    void testHeartbeatResetWhenHealed(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        player.setHealth(4.0f);
        player.tickHeartbeat();
        assertTrue(player.isHeartbeatActive());

        player.setHealth(20.0f);
        player.tickHeartbeat();
        assertFalse(player.isHeartbeatActive());

        env.destroyInstance(instance, true);
    }
}
