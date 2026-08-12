package net.onelitefeather.cygnus.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.resourcepack.ResourcePackService;
import net.onelitefeather.cygnus.hud.PageCountHudComponent;
import net.onelitefeather.cygnus.hud.PageTimerHudComponent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLoginListenerTest extends CygnusPlayerTestBase {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("resourcepack.url");
        System.clearProperty("resourcepack.hash");
    }

    private static GameConfig lobbyConfig() {
        return GameConfig.builder().lobbyTime(30).minPlayers(2).gameTime(600).maxPlayers(10).build();
    }

    @Test
    void testKicksWhenServerIsFull(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 0, () -> new LobbyPhase(lobbyConfig()), Optional.empty());
        listener.accept(new AsyncPlayerConfigurationEvent(player, true));

        assertFalse(player.isOnline());

        env.destroyInstance(instance, true);
    }

    @Test
    void testKicksWhenPhaseIsNotLobby(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GamePhase gamePhase = new GamePhase(new PageTimerHudComponent(), new PageCountHudComponent(), () -> {
        }, 600, new JumpScareManager());

        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 10, () -> gamePhase, Optional.empty());
        listener.accept(new AsyncPlayerConfigurationEvent(player, true));

        assertFalse(player.isOnline());

        env.destroyInstance(instance, true);
    }

    @Test
    void testSuccessfulLoginSetsSpawningInstanceAndSkipsResourcePackWhenAbsent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 10, () -> new LobbyPhase(lobbyConfig()), Optional.empty());
        AsyncPlayerConfigurationEvent event = new AsyncPlayerConfigurationEvent(player, true);
        listener.accept(event);

        assertSame(instance, event.getSpawningInstance());
        assertNull(player.getResourcePackFuture());

        env.destroyInstance(instance, true);
    }

    @Test
    void testSuccessfulLoginSendsResourcePackWhenPresent(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        System.setProperty("resourcepack.url", "https://example.com/pack.zip");
        System.setProperty("resourcepack.hash", "a".repeat(40));
        Optional<ResourcePackService> resourcePackService = ResourcePackService.create();
        assertTrue(resourcePackService.isPresent());

        PlayerLoginListener listener = new PlayerLoginListener(() -> instance, 10, () -> new LobbyPhase(lobbyConfig()), resourcePackService);
        listener.accept(new AsyncPlayerConfigurationEvent(player, true));

        assertNotNull(player.getResourcePackFuture());

        env.destroyInstance(instance, true);
    }
}
