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
    private static final Pos GAME_SPAWN = new Pos(10.5, 41, -7.5, -45F, 10F);
    private static final int MAX_TICKS_UNTIL_SWITCH = 20;

    @Test
    void testStartSpawnsTheSpiderImmediatelyWhenThePlayerIsAlreadyInTheTargetInstance(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();

        transition.start(player, instance);

        Entity spider = spiderIn(instance).orElseThrow(() -> new AssertionError("No spider was spawned in the target instance"));
        assertSame(instance, spider.getInstance(), "The spider must live in the target instance");
        transition.cancel(player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testStartDoesNotSpawnTheSpiderBeforeThePlayerArrivesInTheTargetInstance(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();

        transition.start(player, game);

        assertTrue(spiderIn(game).isEmpty(), "No spider may exist in the game instance before the player arrived there");
        assertTrue(transition.isRunning(player), "The transition must wait for the player to arrive");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testArrivalInTheTargetInstanceSpawnsTheSpiderAtThePlayersPositionThere(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player, game);

        player.setInstance(game, GAME_SPAWN).join();
        env.tick();

        Entity spider = spiderIn(game).orElseThrow(() -> new AssertionError("No spider was spawned after the player arrived"));
        assertSame(game, player.getInstance(), "The player must be in the game instance");
        double expectedY = player.getPosition().y() + player.getEyeHeight() - spider.getEyeHeight();
        assertEquals(player.getPosition().x(), spider.getPosition().x(), 1e-9, "Spider x must match the player's position in the game instance");
        assertEquals(player.getPosition().z(), spider.getPosition().z(), 1e-9, "Spider z must match the player's position in the game instance");
        assertEquals(expectedY, spider.getPosition().y(), 1e-9, "Spider eye must sit at the player's eye in the game instance");
        assertEquals(player.getPosition().yaw(), spider.getPosition().yaw(), 1e-4, "Spider yaw must match the player");
        assertEquals(player.getPosition().pitch(), spider.getPosition().pitch(), 1e-4, "Spider pitch must match the player");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testLeavingTheLobbyDoesNotCancelTheTransitionWaitingForTheGameInstance(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        Instance otherLobby = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player, game);

        player.setInstance(otherLobby, SPAWN).join();
        tick(env, 1);

        assertTrue(transition.isRunning(player), "Leaving the lobby must not cancel the transition");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
        env.destroyInstance(otherLobby, true);
    }

    @Test
    void testRegressionLobbyToGameArrivalKeepsTheTransitionAndSwitchesTheCamera(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player, game);

        // The lobby-to-game switch fires RemoveEntityFromInstanceEvent for the lobby, which must not abandon it
        player.setInstance(game, GAME_SPAWN).join();
        env.tick();
        Entity spider = spiderIn(game).orElseThrow(() -> new AssertionError("No spider was spawned after the arrival"));
        tickUntilCameraSwitch(env, cameras, spider);

        assertTrue(transition.isRunning(player), "The transition must not be abandoned by the lobby exit");
        assertFalse(spider.isRemoved(), "The spider must still exist after the camera switched");
        tick(env, WakeUpTransition.DURATION_TICKS);
        assertEquals(player.getEntityId(), cameras.last(), "The camera must return to the player after the duration");
        assertTrue(spider.isRemoved(), "The spider must be removed after the duration");
        assertFalse(transition.isRunning(player), "The transition must end after the duration");
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testCameraIsNotSwitchedOnTheFirstTickAfterStart(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player, instance);
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
        transition.start(player, instance);
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

        transition.start(player, instance);
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

        transition.start(player, instance);

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
        transition.start(player, instance);
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
        transition.start(player, instance);
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
        transition.start(player, instance);
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
    void testDisconnectWhileWaitingForArrivalRemovesEverythingWithoutCameraPackets(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player, game);
        CameraLog cameras = new CameraLog(connection);

        env.process().eventHandler().call(new PlayerDisconnectEvent(player));
        tick(env, WakeUpTransition.PENDING_TIMEOUT_TICKS + 5);

        assertFalse(transition.isRunning(player), "The transition waiting for arrival must be cancelled on disconnect");
        assertTrue(spiderIn(game).isEmpty(), "No spider may be spawned for a player who disconnected before arriving");
        assertEquals(0, cameras.count(), "No camera packet may be sent for a transition that never switched the camera");
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testPendingTransitionIsNotAbandonedBeforeTheTimeout(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player, game);

        tick(env, WakeUpTransition.PENDING_TIMEOUT_TICKS - 1);

        assertTrue(transition.isRunning(player), "The transition must still wait one tick before the timeout");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testNeverArrivingAbandonsTheTransitionAfterTheTimeout(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.start(player, game);

        tick(env, WakeUpTransition.PENDING_TIMEOUT_TICKS);

        assertFalse(transition.isRunning(player), "The transition must be abandoned when the player never arrives");
        assertTrue(spiderIn(game).isEmpty(), "No spider may be spawned when the player never arrives");
        assertEquals(0, cameras.count(), "The camera must never be switched when the player never arrives");
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testDisconnectAfterTheSwitchRemovesSpiderAndCancelsTheScheduledEnd(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player, instance);
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
    void testLeavingTheGameInstanceAfterArrivalRemovesTheSpider(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        Instance otherInstance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player, game);
        player.setInstance(game, GAME_SPAWN).join();
        env.tick();
        Entity spider = spiderIn(game).orElseThrow(() -> new AssertionError("No spider was spawned after the arrival"));
        tick(env, 50);

        player.setInstance(otherInstance, SPAWN).join();
        tick(env, 1);

        assertTrue(spider.isRemoved(), "The spider must be removed when the player leaves the game instance after arriving");
        assertFalse(transition.isRunning(player), "The transition must be cancelled when the player leaves the game instance");
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
        env.destroyInstance(otherInstance, true);
    }

    @Test
    void testGameFinishRemovesTheSpiderAndCancelsTheScheduledEnd(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.start(player, instance);
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
