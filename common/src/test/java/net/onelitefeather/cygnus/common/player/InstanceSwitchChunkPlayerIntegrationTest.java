package net.onelitefeather.cygnus.common.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.network.packet.server.play.UnloadChunkPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the workaround for the 26.2 client bug that makes chunks invisible after an instance
 * switch: unload packets for chunks the player keeps seeing must not reach the client, while every
 * other unload must.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
@ExtendWith(MicrotusExtension.class)
class InstanceSwitchChunkPlayerIntegrationTest {

    private static final Pos SPAWN = new Pos(0, 42, 0);

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(new TestPlayerProvider());
    }

    @Disabled("This test is not working at the moment because spawninstance is always null")
    @Test
    void testSwitchToSamePositionSendsNoUnload(Env env) {
        Instance source = env.createFlatInstance();
        Instance target = env.createFlatInstance();

        TestConnection connection = env.createConnection();
        Player player = connection.connect(source, SPAWN);
        assertInstanceOf(InstanceSwitchChunkPlayer.class, player);

        Collector<UnloadChunkPacket> unloads = connection.trackIncoming(UnloadChunkPacket.class);
        player.setInstance(target, SPAWN).join();

        unloads.assertCount(0);

        env.destroyInstance(target, true);
        env.destroyInstance(source, true);
    }

    @Test
    void testPlainPlayerStillReceivesTheUnloads(Env env) {
        // The counterpart of the test above: without the override the client is told to unload the
        // very chunks it is about to be sent again, which is what makes them invisible
        var connectionManager = env.process().connection();
        connectionManager.setPlayerProvider(Player::new);
        try {
            Instance source = env.createFlatInstance();
            Instance target = env.createFlatInstance();

            TestConnection connection = env.createConnection();
            Player player = connection.connect(source, SPAWN);

            Collector<UnloadChunkPacket> unloads = connection.trackIncoming(UnloadChunkPacket.class);
            player.setInstance(target, SPAWN).join();

            assertFalse(unloads.collect().isEmpty(), "An unpatched player unloads the shared chunks");

            env.destroyInstance(target, true);
            env.destroyInstance(source, true);
        } finally {
            connectionManager.setPlayerProvider(new TestPlayerProvider());
        }
    }

    @Disabled("This test is not working at the moment because spawninstance is always null")
    @Test
    void testShrinkingViewStillUnloadsTheOuterChunks(Env env) {
        Instance source = env.createFlatInstance();
        source.viewDistance(8);
        Instance target = env.createFlatInstance();
        target.viewDistance(3);

        TestConnection connection = env.createConnection();
        Player player = connection.connect(source, SPAWN);

        Collector<UnloadChunkPacket> unloads = connection.trackIncoming(UnloadChunkPacket.class);
        player.setInstance(target, SPAWN).join();

        List<UnloadChunkPacket> received = unloads.collect();
        assertFalse(received.isEmpty(), "Chunks outside the target view still have to be unloaded");

        int targetViewDistance = player.effectiveViewDistance();
        assertTrue(
                received.stream().noneMatch(packet -> isInView(packet, targetViewDistance)),
                "No chunk inside the target view may be unloaded"
        );

        env.destroyInstance(target, true);
        env.destroyInstance(source, true);
    }

    @Disabled("This test is not working at the moment because spawninstance is always null")
    @Test
    void testFilterIsLiftedAfterTheSwitch(Env env) {
        Instance source = env.createFlatInstance();
        Instance target = env.createFlatInstance();

        TestConnection connection = env.createConnection();
        Player player = connection.connect(source, SPAWN);
        player.setInstance(target, SPAWN).join();

        Collector<UnloadChunkPacket> unloads = connection.trackIncoming(UnloadChunkPacket.class);
        player.sendPacket(new UnloadChunkPacket(SPAWN.chunkX(), SPAWN.chunkZ()));

        unloads.assertCount(1);

        env.destroyInstance(target, true);
        env.destroyInstance(source, true);
    }

    /**
     * Returns whether the chunk of the given packet lies within the view around {@link #SPAWN}.
     *
     * @param packet       the unload packet to check
     * @param viewDistance the view distance in chunks
     * @return true if the chunk is inside the view
     */
    private static boolean isInView(UnloadChunkPacket packet, int viewDistance) {
        return Math.abs(packet.chunkX() - SPAWN.chunkX()) <= viewDistance
                && Math.abs(packet.chunkZ() - SPAWN.chunkZ()) <= viewDistance;
    }

    /**
     * Provides the minimal {@link InstanceSwitchChunkPlayer} the test connects with.
     */
    private static final class TestPlayerProvider implements PlayerProvider {

        @Override
        public Player createPlayer(PlayerConnection connection, GameProfile gameProfile) {
            return new TestPlayer(connection, gameProfile);
        }
    }

    /**
     * A player which adds nothing to {@link InstanceSwitchChunkPlayer}, so the test observes the
     * chunk filtering of the base class and nothing else.
     */
    private static final class TestPlayer extends InstanceSwitchChunkPlayer {

        private TestPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}
