package net.onelitefeather.cygnus.camera;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MicrotusExtension.class)
class WakeUpTransitionTest {

    private static final Pos SPAWN = new Pos(0.5, 41, 0.5, 90F, 20F);
    private static final int MAX_TICKS_UNTIL_SWITCH = 20;

    @Test
    void testStartSpawnsASpiderInThePlayersInstance(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();

        transition.start(player);

        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned in the player's instance"));
        assertSame(instance, spider.getInstance(), "The spider must live in the instance of the player");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testCameraIsNotSwitchedOnTheFirstTickAfterStart(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));

        env.tick();

        assertFalse(cameras.targeted(spider), "The camera must wait for the spider to be viewable and the delay before it switches");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testCameraSwitchesToTheSpiderOnALaterTickThanTheSpiderBecameViewable(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));

        int viewableAt = 0;
        int switchedAt = 0;
        for (int ticks = 1; ticks <= MAX_TICKS_UNTIL_SWITCH && switchedAt == 0; ticks++) {
            env.tick();
            if (viewableAt == 0 && spider.isViewer(player)) viewableAt = ticks;
            if (cameras.targeted(spider)) switchedAt = ticks;
        }

        assertTrue(viewableAt > 0, "The spider must become viewable for the player");
        assertTrue(switchedAt > viewableAt, "The camera must switch on a tick after the spider became viewable");
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
    void testCameraIsStillOnTheSpiderAt99TicksAfterTheSwitch(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        tickUntilCameraSwitch(env, cameras, spider);

        tick(env, WakeUpTransition.DURATION_TICKS - 1);

        assertEquals(spider.getEntityId(), cameras.last(), "The camera must still be on the spider 99 ticks after the switch");
        assertFalse(spider.isRemoved(), "The spider must still exist 99 ticks after the switch");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testCameraReturnsToThePlayerAndSpiderIsRemovedAt100TicksAfterTheSwitch(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        tickUntilCameraSwitch(env, cameras, spider);

        tick(env, WakeUpTransition.DURATION_TICKS);

        assertEquals(player.getEntityId(), cameras.last(), "The camera must be back on the player 100 ticks after the switch");
        assertTrue(spider.isRemoved(), "The spider must be removed 100 ticks after the switch");
        assertFalse(transition.isRunning(player), "The transition must no longer be running");
        env.destroyInstance(instance, true);
    }

    @Test
    void testDisconnectWhilePendingRemovesTheSpiderWithoutSwitchingTheCamera(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        CameraLog cameras = new CameraLog(connection);

        env.process().eventHandler().call(new PlayerDisconnectEvent(player));
        tick(env, WakeUpTransition.PENDING_TIMEOUT_TICKS + 5);

        assertTrue(spider.isRemoved(), "The spider must be removed when the player disconnects while the transition is pending");
        assertFalse(transition.isRunning(player), "The pending transition must be cancelled on disconnect");
        assertFalse(cameras.targeted(spider), "The camera must never be switched to the spider after a disconnect");
        env.destroyInstance(instance, true);
    }

    @Test
    void testPendingTransitionIsAbandonedAfterTheTimeout(Env env) {
        Instance instance = env.createFlatInstance();
        Instance otherInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        // The player never reaches the spider's instance, so the spider can never be viewable for them
        player.setInstance(otherInstance, SPAWN).join();

        tick(env, WakeUpTransition.PENDING_TIMEOUT_TICKS);

        assertTrue(spider.isRemoved(), "The spider must be removed when the pending transition is abandoned");
        assertFalse(transition.isRunning(player), "The pending transition must be abandoned after the timeout");
        assertFalse(cameras.targeted(spider), "The camera must never be switched to a spider that was never viewable");
        env.destroyInstance(instance, true);
        env.destroyInstance(otherInstance, true);
    }

    @Test
    void testDisconnectAfterTheSwitchRemovesSpiderAndCancelsTheScheduledEnd(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        CameraLog cameras = new CameraLog(connection);
        tickUntilCameraSwitch(env, cameras, spider);
        tick(env, 50);

        env.process().eventHandler().call(new PlayerDisconnectEvent(player));
        int packetsAfterDisconnect = cameras.count();
        tick(env, 60);

        assertTrue(spider.isRemoved(), "The spider must be removed when the player disconnects");
        assertFalse(transition.isRunning(player), "The transition must be cancelled when the player disconnects");
        assertEquals(packetsAfterDisconnect, cameras.count(), "No camera packet may be sent after the disconnect cancelled the transition");
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
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player);
        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned"));
        CameraLog cameras = new CameraLog(connection);
        tickUntilCameraSwitch(env, cameras, spider);
        tick(env, 50);

        env.process().eventHandler().call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));
        int packetsAfterFinish = cameras.count();
        tick(env, 60);

        assertTrue(spider.isRemoved(), "The spider must be removed when the game finishes");
        assertEquals(player.getEntityId(), cameras.last(), "The camera must return to the player when the game finishes");
        assertFalse(transition.isRunning(player), "The transition must no longer be running after the game finished");
        assertEquals(packetsAfterFinish, cameras.count(), "The scheduled end must not run after the game finished");
        env.destroyInstance(instance, true);
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    /**
     * Ticks until the camera is on the spider and returns the number of ticks it took.
     */
    private static int tickUntilCameraSwitch(Env env, CameraLog cameras, Entity spider) {
        for (int ticks = 1; ticks <= MAX_TICKS_UNTIL_SWITCH; ticks++) {
            env.tick();
            if (cameras.targeted(spider)) return ticks;
        }
        throw new AssertionError("The camera was never switched to the spider");
    }

    private static Optional<Entity> spiderIn(Instance instance) {
        return instance.getEntities().stream()
                .filter(entity -> entity.getEntityType() == EntityType.SPIDER)
                .findFirst();
    }

    /**
     * Accumulates the camera targets seen so far. A {@link Collector} stops tracking once {@link Collector#collect()}
     * is called, so the collector is re-created after every poll and nothing sent in between is lost.
     */
    private static final class CameraLog {

        private final TestConnection connection;
        private final List<Integer> targets = new ArrayList<>();
        private Collector<CameraPacket> collector;

        CameraLog(TestConnection connection) {
            this.connection = connection;
            this.collector = connection.trackIncoming(CameraPacket.class);
        }

        private void poll() {
            for (CameraPacket packet : collector.collect()) {
                targets.add(packet.cameraId());
            }
            collector = connection.trackIncoming(CameraPacket.class);
        }

        boolean targeted(Entity entity) {
            poll();
            return targets.contains(entity.getEntityId());
        }

        int last() {
            poll();
            assertFalse(targets.isEmpty(), "Expected at least one camera packet");
            return targets.getLast();
        }

        int count() {
            poll();
            return targets.size();
        }
    }
}
