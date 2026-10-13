package net.onelitefeather.cygnus.camera;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.server.network.packet.server.play.SetTimePacket;
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
import static net.onelitefeather.cygnus.camera.WakeUpTransition.Kind.CLOSE;
import static net.onelitefeather.cygnus.camera.WakeUpTransition.Kind.OPEN;

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

        transition.open(player, instance);

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

        transition.open(player, game);

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
        transition.open(player, game);

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
        transition.open(player, game);

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
        transition.open(player, game);

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
        transition.open(player, instance);
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
        transition.open(player, instance);
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

        transition.open(player, instance);
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

        transition.open(player, instance);

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
        transition.open(player, instance);
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
        transition.open(player, instance);
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
        transition.open(player, instance);
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
        transition.open(player, game);
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
        transition.open(player, game);

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
        transition.open(player, game);

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
        transition.open(player, instance);
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
        transition.open(player, game);
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
        transition.open(player, instance);
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


    @Test
    void testCloseSwitchesTheCameraToTheSpiderInTheLobby(Env env) {
        Instance lobby = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();

        transition.close(player, lobby);
        Entity spider = spiderIn(lobby).orElseThrow(() -> new AssertionError("No spider was spawned in the lobby"));
        tickUntilCameraSwitch(env, cameras, spider);

        assertEquals(spider.getEntityId(), cameras.last(), "The camera must be on the lobby spider after the close switch");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
    }

    @Test
    void testCloseCameraDoesNotEndByItselfWhilePlayerStaysInTheLobby(Env env) {
        Instance lobby = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.close(player, lobby);
        Entity spider = spiderIn(lobby).orElseThrow(() -> new AssertionError("No spider was spawned in the lobby"));
        tickUntilCameraSwitch(env, cameras, spider);

        tick(env, WakeUpTransition.DURATION_TICKS + 50);

        assertEquals(spider.getEntityId(), cameras.last(), "The close camera must stay on the lobby spider while the player is in the lobby");
        assertFalse(spider.isRemoved(), "The lobby spider must still exist while the player is in the lobby");
        assertTrue(transition.isRunning(player, CLOSE), "The close transition must still run while the player is in the lobby");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
    }

    @Test
    void testLeavingTheLobbyEndsTheCloseAndRemovesTheLobbySpiderWithoutError(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());
        transition.close(player, lobby);
        Entity spider = spiderIn(lobby).orElseThrow(() -> new AssertionError("No spider was spawned in the lobby"));
        tickUntilCameraSwitch(env, cameras, spider);

        player.setInstance(game, GAME_SPAWN).join();
        env.tick();

        assertTrue(spider.isRemoved(), "The lobby spider must be removed when the player leaves the lobby");
        assertFalse(transition.isRunning(player, CLOSE), "The close transition must end when the player leaves the lobby");
        assertEquals(player.getEntityId(), cameras.last(), "The camera must return to the player when the player leaves the lobby");
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testCloseSetsTheLobbyWorldAgeToAMultipleOf24000AtTheSwitch(Env env) {
        Instance lobby = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        tick(env, 100);
        CameraLog cameras = new CameraLog(connection);
        PacketLog<SetTimePacket> times = new PacketLog<>(connection, SetTimePacket.class);
        long ageBefore = lobby.getWorldAge();
        WakeUpTransition transition = new WakeUpTransition();

        transition.close(player, lobby);
        Entity spider = spiderIn(lobby).orElseThrow(() -> new AssertionError("No spider was spawned in the lobby"));
        tickUntilCameraSwitch(env, cameras, spider);

        assertTrue(ageBefore > 0 && ageBefore < 24000, "Precondition: the lobby age must be inside the first period, was " + ageBefore);
        assertEquals(1, gameTimeCount(times, 24000L), "The lobby must receive the world age 24000 exactly once, at the close switch");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
    }

    @Test
    void testOpenSetsTheGameWorldAgeToTheOpenBandAtTheSwitch(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        tick(env, 100);
        CameraLog cameras = new CameraLog(connection);
        PacketLog<SetTimePacket> times = new PacketLog<>(connection, SetTimePacket.class);
        WakeUpTransition transition = new WakeUpTransition();
        transition.open(player, game);

        player.setInstance(game, GAME_SPAWN).join();
        env.tick();
        Entity spider = spiderIn(game).orElseThrow(() -> new AssertionError("No spider was spawned after the arrival"));
        tickUntilCameraSwitch(env, cameras, spider);

        assertEquals(1, gameTimeCount(times, 12000L), "The game instance must receive the world age 12000 exactly once, at the open switch");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testOneCloseBatchSetsTheLobbyWorldAgeOnlyOnceForTwoPlayers(Env env) {
        Instance lobby = env.createFlatInstance();
        TestConnection first = env.createConnection();
        Player firstPlayer = first.connect(lobby, SPAWN);
        TestConnection second = env.createConnection();
        Player secondPlayer = second.connect(lobby, new Pos(2.5, 41, 0.5));
        tick(env, 100);
        PacketLog<SetTimePacket> times = new PacketLog<>(first, SetTimePacket.class);
        CameraLog firstCameras = new CameraLog(first);
        CameraLog secondCameras = new CameraLog(second);
        WakeUpTransition transition = new WakeUpTransition();

        transition.close(List.of(firstPlayer, secondPlayer), lobby);
        tick(env, MAX_TICKS_UNTIL_SWITCH);

        assertEquals(1, gameTimeCount(times, 24000L), "Two players of one close batch must set the lobby world age once, not once per player");
        assertEquals(spiderViewedBy(lobby, firstPlayer).getEntityId(), firstCameras.last(), "The first player's camera must be on its spider");
        assertEquals(spiderViewedBy(lobby, secondPlayer).getEntityId(), secondCameras.last(), "The second player's camera must be on its spider");
        transition.cancel(firstPlayer);
        transition.cancel(secondPlayer);
        env.destroyInstance(lobby, true);
    }

    @Test
    void testOpeningTheGameDoesNotEndARunningCloseInTheLobby(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        WakeUpTransition transition = new WakeUpTransition();
        transition.close(player, lobby);
        Entity lobbySpider = spiderIn(lobby).orElseThrow(() -> new AssertionError("No spider was spawned in the lobby"));
        tickUntilCameraSwitch(env, cameras, lobbySpider);

        transition.open(player, game);
        tick(env, 1);

        assertTrue(transition.isRunning(player, CLOSE), "Starting the open transition must not end the close in the lobby");
        assertEquals(lobbySpider.getEntityId(), cameras.last(), "The camera must stay on the lobby spider until the player leaves the lobby");
        transition.cancel(player);
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testFullSequenceCloseInTheLobbyThenTeleportThenOpenThenReturn(Env env) {
        Instance lobby = env.createFlatInstance();
        Instance game = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby, SPAWN);
        CameraLog cameras = new CameraLog(connection);
        PacketLog<SetTimePacket> times = new PacketLog<>(connection, SetTimePacket.class);
        WakeUpTransition transition = new WakeUpTransition();
        transition.register(env.process().eventHandler());

        transition.close(player, lobby);
        Entity lobbySpider = spiderIn(lobby).orElseThrow(() -> new AssertionError("No spider was spawned in the lobby"));
        tickUntilCameraSwitch(env, cameras, lobbySpider);
        transition.open(player, game);
        player.setInstance(game, GAME_SPAWN).join();
        env.tick();

        assertFalse(transition.isRunning(player, CLOSE), "The close must end once the player left the lobby");
        assertTrue(lobbySpider.isRemoved(), "The lobby spider must be removed once the player left the lobby");
        Entity gameSpider = spiderIn(game).orElseThrow(() -> new AssertionError("No spider was spawned after the arrival"));
        tickUntilCameraSwitch(env, cameras, gameSpider);
        assertEquals(1, gameTimeCount(times, 12000L), "The open switch must set the game world age into the open band");

        tick(env, WakeUpTransition.DURATION_TICKS);

        assertEquals(player.getEntityId(), cameras.last(), "The camera must return to the player after the open duration");
        assertTrue(gameSpider.isRemoved(), "The game spider must be removed after the open duration");
        assertFalse(transition.isRunning(player), "No transition may run after the open duration");
        env.destroyInstance(lobby, true);
        env.destroyInstance(game, true);
    }

    @Test
    void testCloseWorldAgeIsTheNextMultipleOf24000() {
        assertEquals(24000L, WakeUpTransition.worldAgeFor(CLOSE, 1L), "An age just past zero must move to the next multiple of 24000");
        assertEquals(48000L, WakeUpTransition.worldAgeFor(CLOSE, 24001L), "An age just past a multiple must move to the next one");
    }

    @Test
    void testCloseWorldAgeKeepsAnAgeThatIsAlreadyAMultipleOf24000() {
        assertEquals(24000L, WakeUpTransition.worldAgeFor(CLOSE, 24000L), "An age that is already a multiple of 24000 must not move");
    }

    @Test
    void testOpenWorldAgeIsTheNextTickInTheOpenBand() {
        assertEquals(36000L, WakeUpTransition.worldAgeFor(OPEN, 12001L), "An age past the open band start must move to the next band start");
        assertEquals(132000L, WakeUpTransition.worldAgeFor(OPEN, 114000L), "The game map's new moon clock time must move forward into the open band");
    }

    @Test
    void testOpenWorldAgeKeepsAnAgeThatIsAlreadyAtTheOpenBandStart() {
        assertEquals(12000L, WakeUpTransition.worldAgeFor(OPEN, 12000L), "An age at the open band start must not move");
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

    private static Entity spiderViewedBy(Instance instance, Player viewer) {
        return instance.getEntities().stream()
                .filter(entity -> entity.getEntityType() == EntityType.SPIDER && entity.isViewer(viewer))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No spider is viewed by " + viewer.getUsername()));
    }

    /**
     * Collects the packets of one type received by a test connection. A {@link Collector} stops tracking once it is
     * collected, so it is re-created after every poll and nothing sent in between is lost.
     */
    private static final class PacketLog<T extends ServerPacket> {

        private final TestConnection connection;
        private final Class<T> type;
        private final List<T> packets = new ArrayList<>();
        private Collector<T> collector;

        PacketLog(TestConnection connection, Class<T> type) {
            this.connection = connection;
            this.type = type;
            this.collector = connection.trackIncoming(type);
        }

        private void poll() {
            packets.addAll(collector.collect());
            collector = connection.trackIncoming(type);
        }

        List<T> all() {
            poll();
            return List.copyOf(packets);
        }
    }

    private static long gameTimeCount(PacketLog<SetTimePacket> log, long gameTime) {
        return log.all().stream().filter(packet -> packet.gameTime() == gameTime).count();
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
