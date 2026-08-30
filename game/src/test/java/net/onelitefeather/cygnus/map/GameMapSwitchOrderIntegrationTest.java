package net.onelitefeather.cygnus.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MicrotusExtension.class)
class GameMapSwitchOrderIntegrationTest {

    private static final String ARENA_NAME = "arena";
    private static final Pos LOBBY_SPAWN = Pos.ZERO;
    private static final Pos SLENDER_SPAWN = new Pos(1, 1, 1);
    private static final Pos SURVIVOR_SPAWN = new Pos(2, 2, 2);

    @Test
    void testTeleportAfterSwitchMovesEveryoneIntoTheGameInstance(Env env, @TempDir Path root) throws IOException {
        GameMapProvider provider = createProvider(root);
        InstanceContainer lobbyInstance = (InstanceContainer) provider.getActiveInstance().get();
        provider.loadGameMap();

        Player slender = env.createPlayer(lobbyInstance, LOBBY_SPAWN);
        Player survivor = env.createPlayer(lobbyInstance, LOBBY_SPAWN);
        TeamService teamService = createTeamService();
        teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow().addPlayer(slender);
        teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().addPlayers(Set.of(survivor));

        provider.switchToGameMap();
        InstanceContainer gameInstance = (InstanceContainer) provider.getActiveInstance().get();
        assertNotSame(lobbyInstance, gameInstance);

        TeamHelper.teleportTeams(teamService, provider.getGameMap(), gameInstance);
        // Player#setInstance is asynchronous (chunk loading), so poll instead of guessing a fixed tick count.
        for (int i = 0; i < 100 && (slender.getInstance() != gameInstance || survivor.getInstance() != gameInstance); i++) {
            env.tick();
        }
        assertSame(gameInstance, slender.getInstance());
        assertSame(gameInstance, survivor.getInstance());
        assertEquals(SLENDER_SPAWN, slender.getPosition());

        assertDoesNotThrow(provider::releasePreviousInstance);
        assertFalse(MinecraftServer.getInstanceManager().getInstances().contains(lobbyInstance));

        provider.close();
        env.destroyInstance(gameInstance, true);
    }

    @Test
    void testSwitchWithoutLoadedGameMapThrows(Env env, @TempDir Path root) throws IOException {
        GameMapProvider provider = createProvider(root);
        InstanceContainer lobbyInstance = (InstanceContainer) provider.getActiveInstance().get();

        assertThrows(IllegalStateException.class, provider::switchToGameMap);

        provider.close();
        env.destroyInstance(lobbyInstance, true);
    }

    @Test
    void testReleaseIsRepeatable(Env env, @TempDir Path root) throws IOException {
        GameMapProvider provider = createProvider(root);
        provider.loadGameMap();
        provider.switchToGameMap();

        InstanceContainer gameInstance = (InstanceContainer) provider.getActiveInstance().get();

        assertDoesNotThrow(provider::releasePreviousInstance);
        assertDoesNotThrow(provider::releasePreviousInstance);

        provider.close();
        env.destroyInstance(gameInstance, true);
    }

    @Test
    void testUpdateInstanceWithinSameInstanceTeleports(Env env, @TempDir Path root) throws IOException {
        GameMapProvider provider = createProvider(root);
        provider.loadGameMap();
        provider.switchToGameMap();

        InstanceContainer gameInstance = (InstanceContainer) provider.getActiveInstance().get();
        Player slender = env.createPlayer(gameInstance, LOBBY_SPAWN);
        Player survivor = env.createPlayer(gameInstance, LOBBY_SPAWN);

        TeamService teamService = createTeamService();
        teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow().addPlayer(slender);
        teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().addPlayers(Set.of(survivor));

        assertDoesNotThrow(() -> TeamHelper.teleportTeams(teamService, provider.getGameMap(), gameInstance));
        assertEquals(SLENDER_SPAWN, slender.getPosition());

        provider.close();
        env.destroyInstance(gameInstance, true);
    }

    private TeamService createTeamService() {
        TeamService teamService = TeamService.of();
        teamService.add(Team.of(GameConfig.SLENDER_KEY, 1));
        teamService.add(Team.of(GameConfig.SURVIVOR_KEY, 10));
        teamService.add(Team.of(GameConfig.SPECTATOR_KEY, 10));
        return teamService;
    }

    private GameMapProvider createProvider(Path root) throws IOException {
        Path maps = root.resolve("game").resolve("maps");
        writeMap(maps.resolve("lobby"), new BaseMap("lobby", LOBBY_SPAWN, List.of()), false);
        writeMap(
                maps.resolve(ARENA_NAME),
                new GameMap(ARENA_NAME, LOBBY_SPAWN, SLENDER_SPAWN, Set.of(), Set.of(SURVIVOR_SPAWN), List.of(), null),
                true
        );
        return new GameMapProvider(root);
    }

    private void writeMap(Path directoryRoot, BaseMap map, boolean dimensionLayout) throws IOException {
        Path regionDirectory = dimensionLayout
                ? directoryRoot.resolve("dimensions").resolve("minecraft").resolve("overworld").resolve("region")
                : directoryRoot.resolve("region");
        Files.createDirectories(regionDirectory);
        Files.writeString(directoryRoot.resolve("map.json"), GsonHelper.GSON.toJson(map), StandardCharsets.UTF_8);
    }
}
