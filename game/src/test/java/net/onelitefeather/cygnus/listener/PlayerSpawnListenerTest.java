package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSpawnListenerTest extends CygnusPlayerTestBase {

    private static GameConfig lobbyConfig() {
        return GameConfig.builder().lobbyTime(30).minPlayers(2).gameTime(600).maxPlayers(10).build();
    }

    @Test
    void testFirstSpawnInLobbyPhaseSetsDisplayNameAndTeleports(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        LobbyPhase lobbyPhase = new LobbyPhase(lobbyConfig());

        AtomicBoolean spawned = new AtomicBoolean(false);
        PlayerSpawnListener listener = new PlayerSpawnListener(p -> spawned.set(true), () -> lobbyPhase);

        PlayerSpawnEvent firstSpawn = new PlayerSpawnEvent(player, instance, true);
        listener.accept(firstSpawn);

        assertTrue(spawned.get(), "Spawn supplier must be called on first spawn in lobby");
        assertEquals(Component.text(player.getUsername()), player.getDisplayName());

        env.destroyInstance(instance, true);
    }

    @Test
    void testSubsequentSpawnDoesNothing(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        LobbyPhase lobbyPhase = new LobbyPhase(lobbyConfig());

        AtomicInteger spawnCount = new AtomicInteger(0);
        PlayerSpawnListener listener = new PlayerSpawnListener(p -> spawnCount.incrementAndGet(), () -> lobbyPhase);

        PlayerSpawnEvent subsequentSpawn = new PlayerSpawnEvent(player, instance, false);
        listener.accept(subsequentSpawn);

        assertEquals(0, spawnCount.get(), "Subsequent spawn must be ignored");

        env.destroyInstance(instance, true);
    }

    @Test
    void testFirstSpawnWithoutTeamTagTeleportsOutsideLobby(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());

        AtomicBoolean spawned = new AtomicBoolean(false);
        PlayerSpawnListener listener = new PlayerSpawnListener(p -> spawned.set(true), () -> gamePhase);

        PlayerSpawnEvent firstSpawn = new PlayerSpawnEvent(player, instance, true);
        listener.accept(firstSpawn);

        assertTrue(spawned.get(), "Player without team tag must be teleported to spawn on first spawn");

        env.destroyInstance(instance, true);
    }

    @Test
    void testFirstSpawnWithTeamTagSkipsTeleportOutsideLobby(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());

        AtomicBoolean spawned = new AtomicBoolean(false);
        PlayerSpawnListener listener = new PlayerSpawnListener(p -> spawned.set(true), () -> gamePhase);

        PlayerSpawnEvent firstSpawn = new PlayerSpawnEvent(player, instance, true);
        listener.accept(firstSpawn);

        assertFalse(spawned.get(), "Player with existing team tag must not be teleported to default spawn");

        env.destroyInstance(instance, true);
    }
}
