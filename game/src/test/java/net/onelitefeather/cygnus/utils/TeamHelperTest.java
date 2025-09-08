package net.onelitefeather.cygnus.utils;

import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.theevilreaper.xerus.api.team.TeamServiceImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.TeamCreator;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.Helper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class TeamHelperTest {

    private static GameConfig gameConfig;
    private static Instance instance;
    private static Player player;

    @BeforeAll
    static void init(@NotNull Env env) {
        instance = env.createFlatInstance();
        player = env.createPlayer(instance);
        gameConfig = new GameConfigReader(Paths.get("")).getConfig();
    }

    @AfterEach
    void cleanup() {
        player.removeTag(Tags.TEAM_ID);
        player.setDisplayName(Component.text(player.getUsername()));
    }

    @AfterAll
    static void cleanup(@NotNull Env env) {
        env.destroyInstance(instance, true);
    }

    @Test
    void testFailedTeamAllocation() {
        Team slenderTeam = Team.builder().name("Slender").capacity(0).colorData(ColorData.AQUA).build();
        Team survivor = Team.builder().name("Survivor").capacity(11).colorData(ColorData.AQUA).build();

        assertNotNull(slenderTeam);
        assertNotNull(survivor);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TeamHelper.prepareTeamAllocation(slenderTeam, survivor)
        );
        assertEquals("The slender team must have a capacity from one", exception.getMessage());
    }

    @Test
    void testTeamAllocation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        for (int i = 0; i <= 3; i++) {
            env.createPlayer(instance);
        }

        Team slenderTeam = Team.builder().name("Slender").capacity(1).colorData(ColorData.AQUA).build();
        Team survivor = Team.builder().name("Survivor").capacity(11).colorData(ColorData.AQUA).build();

        assertNotNull(slenderTeam);
        assertNotNull(survivor);

        TeamHelper.prepareTeamAllocation(slenderTeam, survivor);

        assertEquals(1, slenderTeam.getPlayers().size());
        assertEquals(4, survivor.getPlayers().size());

        env.destroyInstance(instance, true);
    }

    @Disabled
    @Test
    void testSlenderTeleport(@NotNull Env env) {
        Instance testInstance = env.createFlatInstance();
        AmbientProvider ambientProvider = new AmbientProvider();
        TeamService<Team> teamService = new TeamServiceImpl<>();
        TeamCreator teamCreator = new TeamCreator() {
        };
        teamCreator.createTeams(gameConfig, teamService, ambientProvider);
        Pos slenderSpawn = new Pos(10, 10, 10);
        GameMap gameMap = new GameMap("Test", Pos.ZERO, slenderSpawn, Set.of(), Set.of(), "");
        assertNotNull(gameMap);
        assertEquals(slenderSpawn, gameMap.getSlenderSpawn());

        assertEquals(2, teamService.getTeams().size());

        teamService.getTeam(GameConfig.SLENDER_TEAM_NAME).get().addPlayer(player);
        TeamHelper.teleportTeams(teamService, gameMap, testInstance);

        assertEquals(slenderSpawn, player.getPosition());
        player.setInstance(instance);
        env.destroyInstance(testInstance);
    }

    @Test
    void testNoTabListUpdate() {
        TeamService<Team> teamService = new TeamServiceImpl<>();
        assertFalse(teamService.hasTeams());
        TeamHelper.updateTabList(teamService);
        assertNotNull(player.getDisplayName());
        assertFalse(player.getDisplayName().hasStyling());
    }

    @Test
    void testInvalidUpdateTabList() {
        AmbientProvider ambientProvider = new AmbientProvider();
        TeamService<Team> teamService = new TeamServiceImpl<>();
        TeamCreator teamCreator = new TeamCreator() {
        };
        teamCreator.createTeams(gameConfig, teamService, ambientProvider);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> TeamHelper.updateTabList(teamService)
        );

        assertEquals("The slender team must have at least one player", exception.getMessage());
    }

    @Test
    void testUpdateTabList(@NotNull Env env) {
        AmbientProvider ambientProvider = new AmbientProvider();
        TeamService<Team> teamService = new TeamServiceImpl<>();
        TeamCreator teamCreator = new TeamCreator() {
        };
        teamCreator.createTeams(gameConfig, teamService, ambientProvider);

        Set<Player> survivors = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            survivors.add(env.createPlayer(instance));
        }

        assertEquals(3, survivors.size());

        teamService.getTeams().getFirst().addPlayer(player);
        Team survivorTeam = teamService.getTeams().getLast();
        survivorTeam.addPlayers(survivors);

        TeamHelper.updateTabList(teamService);

        Component displayName = player.getDisplayName();
        assertNotNull(displayName);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(displayName).contains("⛧"));

        survivorTeam.getPlayers().forEach(survivor -> {
            Component survivorDisplayName = survivor.getDisplayName();
            assertNotNull(survivorDisplayName);
            assertTrue(survivorDisplayName.hasStyling());
            assertEquals(NamedTextColor.GREEN, survivorDisplayName.style().color());
        });

        survivorTeam.removePlayers(survivors, Entity::remove);
        survivors.clear();
    }

    @Test
    void testIsInSlenderTeam() {
        player.setTag(Tags.TEAM_ID, Helper.SLENDER_ID);
        assertTrue(TeamHelper.isSlenderTeam(player));
        assertFalse(TeamHelper.isSurvivorTeam(player));
    }

    @Test
    void testIsInSurvivorTeam() {
        player.setTag(Tags.TEAM_ID, Helper.SURVIVOR_ID);
        assertFalse(TeamHelper.isSlenderTeam(player));
        assertTrue(TeamHelper.isSurvivorTeam(player));
    }
}
