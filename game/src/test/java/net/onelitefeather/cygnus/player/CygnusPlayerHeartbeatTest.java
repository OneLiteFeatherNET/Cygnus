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
    void testHeartbeatSendsVirtualBorderCenteredOnPlayer(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Pos spawnPos = new Pos(120.5, 40, -75.25);
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, spawnPos);

        player.setHealth(4.0f);
        var collector = connection.trackIncoming(InitializeWorldBorderPacket.class);
        player.tickHeartbeat();

        collector.assertSingle(packet -> {
            assertEquals(spawnPos.x(), packet.x(), 0.001);
            assertEquals(spawnPos.z(), packet.z(), 0.001);
            assertEquals(100.0, packet.newDiameter(), 0.001);
        });

        env.destroyInstance(instance, true);
    }

    @Test
    void testHeartbeatWarningBlocksStayWithinIntRangeNearDeath(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, Pos.ZERO);

        player.setHealth(0.5f);
        var collector = connection.trackIncoming(InitializeWorldBorderPacket.class);
        player.tickHeartbeat();

        collector.assertSingle(packet -> assertTrue(packet.warningBlocks() <= 10_000,
                "warningBlocks should stay in a sane range, was " + packet.warningBlocks()));

        env.destroyInstance(instance, true);
    }

    @Test
    void testHeartbeatResetRestoresRealBorder(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, Pos.ZERO);

        player.setHealth(4.0f);
        player.tickHeartbeat();
        assertTrue(player.isHeartbeatActive());

        player.setHealth(20.0f);
        var collector = connection.trackIncoming(InitializeWorldBorderPacket.class);
        player.tickHeartbeat();

        collector.assertSingle(packet -> {
            assertEquals(instance.getWorldBorder().centerX(), packet.x(), 0.001);
            assertEquals(instance.getWorldBorder().centerZ(), packet.z(), 0.001);
            assertEquals(instance.getWorldBorder().diameter(), packet.newDiameter(), 0.001);
        });

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
