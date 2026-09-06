package net.onelitefeather.cygnus.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.InitializeWorldBorderPacket;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
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
    void testHeartbeatNeverTouchesTheWorldBorder(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, Pos.ZERO);

        var collector = connection.trackIncoming(InitializeWorldBorderPacket.class);

        player.setHealth(4.0f);
        player.tickHeartbeat();
        player.setHealth(0.5f);
        player.tickHeartbeat();
        player.setHealth(20.0f);
        player.tickHeartbeat();

        collector.assertEmpty();

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
