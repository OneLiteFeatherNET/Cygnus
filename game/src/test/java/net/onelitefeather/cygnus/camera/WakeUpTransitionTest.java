package net.onelitefeather.cygnus.camera;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class WakeUpTransitionTest {

    private static final Pos SPAWN = new Pos(0.5, 41, 0.5, 90F, 20F);

    @Test
    void testStartPointsTheCameraAtASpiderInThePlayersInstance(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        WakeUpTransition transition = new WakeUpTransition();

        transition.start(player);

        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned in the player's instance"));
        assertSame(instance, spider.getInstance(), "The spider must live in the instance of the player");
        assertEquals(spider.getEntityId(), lastCameraTarget(cameras), "The camera must point at the spider");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSpiderIsVisibleOnlyToItsPlayer(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection owner = env.createConnection();
        Player player = owner.connect(instance, SPAWN);
        TestConnection other = env.createConnection();
        Player bystander = other.connect(instance, new Pos(2.5, 41, 0.5));
        WakeUpTransition transition = new WakeUpTransition();

        transition.start(player);
        env.tick();

        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        assertTrue(spider.isViewer(player), "The player who owns the transition must see the spider");
        assertFalse(spider.isViewer(bystander), "A second player must not see the spider");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSpiderIsPlacedAtTheEyeWithTheViewOfThePlayer(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();

        transition.start(player);

        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        double expectedY = player.getPosition().y() + player.getEyeHeight() - spider.getEyeHeight();
        assertEquals(player.getPosition().x(), spider.getPosition().x(), 1e-9, "Spider x must match the player");
        assertEquals(player.getPosition().z(), spider.getPosition().z(), 1e-9, "Spider z must match the player");
        assertEquals(expectedY, spider.getPosition().y(), 1e-9, "Spider eye must sit at the player's eye");
        assertEquals(player.getPosition().yaw(), spider.getPosition().yaw(), 1e-4, "Spider yaw must match the player");
        assertEquals(player.getPosition().pitch(), spider.getPosition().pitch(), 1e-4, "Spider pitch must match the player");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testCameraIsStillOnTheSpiderAt99Ticks(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));

        tick(env, 99);

        assertEquals(spider.getEntityId(), lastCameraTarget(cameras), "The camera must still be on the spider before 100 ticks");
        assertFalse(spider.isRemoved(), "The spider must still exist before 100 ticks");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testCameraReturnsToThePlayerAndSpiderIsRemovedAt100Ticks(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));

        tick(env, 100);

        assertEquals(player.getEntityId(), lastCameraTarget(cameras), "The camera must be back on the player after 100 ticks");
        assertTrue(spider.isRemoved(), "The spider must be removed after 100 ticks");
        assertFalse(transition.isRunning(player), "The transition must no longer be running");
        env.destroyInstance(instance, true);
    }

    @Test
    void testDisconnectRemovesSpiderAndCancelsTheScheduledEnd(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        tick(env, 50);

        env.process().eventHandler().call(new PlayerDisconnectEvent(player));
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        tick(env, 60);

        assertTrue(spider.isRemoved(), "The spider must be removed when the player disconnects");
        assertFalse(transition.isRunning(player), "The transition must be cancelled when the player disconnects");
        assertEquals(0, cameras.collect().size(), "No camera packet may be sent after the disconnect cancelled the transition");
        env.destroyInstance(instance, true);
    }

    @Test
    void testLeavingTheInstanceRemovesTheSpider(Env env) {
        Instance instance = env.createFlatInstance();
        Instance otherInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        tick(env, 50);

        player.setInstance(otherInstance, SPAWN).join();
        tick(env, 1);

        assertTrue(spider.isRemoved(), "The spider must be removed when the player leaves its instance");
        assertFalse(transition.isRunning(player), "The transition must be cancelled when the player leaves the instance");
        env.destroyInstance(instance, true);
        env.destroyInstance(otherInstance, true);
    }

    @Test
    void testGameFinishRemovesTheSpiderAndCancelsTheScheduledEnd(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        tick(env, 50);

        env.process().eventHandler().call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));
        int packetsAfterFinish = cameras.collect().size();
        tick(env, 60);

        assertTrue(spider.isRemoved(), "The spider must be removed when the game finishes");
        assertEquals(player.getEntityId(), lastCameraTarget(cameras), "The camera must return to the player when the game finishes");
        assertFalse(transition.isRunning(player), "The transition must no longer be running after the game finished");
        assertEquals(packetsAfterFinish, cameras.collect().size(), "The scheduled end must not run after the game finished");
        env.destroyInstance(instance, true);
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    private static Optional<Entity> spiderIn(Instance instance) {
        return instance.getEntities().stream()
                .filter(entity -> entity.getEntityType() == EntityType.SPIDER)
                .findFirst();
    }

    private static int lastCameraTarget(Collector<CameraPacket> cameras) {
        List<CameraPacket> packets = cameras.collect();
        assertFalse(packets.isEmpty(), "Expected at least one camera packet");
        return packets.getLast().cameraId();
    }
}
