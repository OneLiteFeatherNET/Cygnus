package net.onelitefeather.cygnus.utils;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.component.team.ColorComponent;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers what keeps the slender from reading the survivors' names off their heads.
 *
 * <p>The mechanism has two halves and both have to hold: the scoreboard teams must hide names from
 * other teams, and the players must actually be on them. Either half alone does nothing.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.11.0
 */
@ExtendWith(MicrotusExtension.class)
class ScoreboardNameTagTest {

    private final TeamService teamService = TeamService.of();

    @AfterEach
    void tearDown() {
        teamService.clear();
        TeamManager teamManager = net.minestom.server.MinecraftServer.getTeamManager();
        teamManager.getTeams().stream().toList().forEach(teamManager::deleteTeam);
    }

    /**
     * Connects a player under a name of its own. The test environment hands every connection the same
     * username by default, and scoreboard teams are keyed by username - two players called the same
     * are one member as far as a team is concerned.
     *
     * @param env      the test environment
     * @param instance the instance to connect to
     * @param name     the username to connect under
     * @return the connected player
     */
    private Player named(@NotNull Env env, @NotNull Instance instance, String name) {
        return env.createConnection(new GameProfile(UUID.randomUUID(), name)).connect(instance);
    }

    private Team gameTeam(net.kyori.adventure.key.Key key, String name, int capacity) {
        Team team = Team.of(key, capacity);
        team.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        if (name != null) team.add(TeamNameComponent.class, new TeamNameComponent(name));
        teamService.add(team);
        return team;
    }

    private void createGameTeams() {
        gameTeam(GameConfig.SLENDER_KEY, GameConfig.SLENDER_TEAM_NAME, 1);
        gameTeam(GameConfig.SURVIVOR_KEY, GameConfig.SURVIVOR_TEAM_NAME, 10);
        // Exactly as TeamCreator builds it: no TeamNameComponent.
        gameTeam(GameConfig.SPECTATOR_KEY, null, 11);
    }

    @Test
    void testTheSpectatorTeamDoesNotBreakTheSetup(@NotNull Env env) {
        createGameTeams();

        assertDoesNotThrow(() -> new ScoreboardDisplay(teamService.getTeams()),
                "the spectator team carries no name component, and that must not stop the others from being created");

        TeamManager teamManager = env.process().team();
        assertNotNull(teamManager.getTeam(GameConfig.SLENDER_TEAM_NAME));
        assertNotNull(teamManager.getTeam(GameConfig.SURVIVOR_TEAM_NAME));
    }

    @Test
    void testNamesAreHiddenFromTheOtherTeamOnly(@NotNull Env env) {
        createGameTeams();
        new ScoreboardDisplay(teamService.getTeams());

        TeamManager teamManager = env.process().team();
        assertEquals(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS,
                teamManager.getTeam(GameConfig.SURVIVOR_TEAM_NAME).getNameTagVisibility(),
                "the slender must not read survivor names, but the survivors have to keep reading each other's");
        assertEquals(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS,
                teamManager.getTeam(GameConfig.SLENDER_TEAM_NAME).getNameTagVisibility());
    }

    @Test
    void testSyncMirrorsTheRolesOntoTheScoreboardTeams(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        createGameTeams();
        ScoreboardDisplay display = new ScoreboardDisplay(teamService.getTeams());

        Player slender = named(env, instance, "TheSlender");
        Player survivor = named(env, instance, "ASurvivor");
        teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow().addPlayer(slender);
        teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().addPlayer(survivor);

        display.sync(teamService);

        TeamManager teamManager = env.process().team();
        assertTrue(teamManager.getTeam(GameConfig.SLENDER_TEAM_NAME).getMembers().contains(slender.getUsername()));
        assertTrue(teamManager.getTeam(GameConfig.SURVIVOR_TEAM_NAME).getMembers().contains(survivor.getUsername()));
        assertFalse(teamManager.getTeam(GameConfig.SURVIVOR_TEAM_NAME).getMembers().contains(slender.getUsername()),
                "a player on both teams would read the names of both");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorsRideAlongOnTheSurvivorTeam(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        createGameTeams();
        ScoreboardDisplay display = new ScoreboardDisplay(teamService.getTeams());

        Player spectator = named(env, instance, "ASpectator");
        teamService.getTeam(GameConfig.SPECTATOR_KEY).orElseThrow().addPlayer(spectator);

        display.sync(teamService);

        TeamManager teamManager = env.process().team();
        assertNull(teamManager.getTeam("spectator"), "spectators get no team of their own");
        assertTrue(teamManager.getTeam(GameConfig.SURVIVOR_TEAM_NAME).getMembers().contains(spectator.getUsername()),
                "a spectator has to stay on the survivor team, otherwise they lose the names they are watching");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSyncDropsAPlayerWhoChangedSides(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        createGameTeams();
        ScoreboardDisplay display = new ScoreboardDisplay(teamService.getTeams());

        Player player = named(env, instance, "TheRevived");
        Team survivors = teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow();
        survivors.addPlayer(player);
        display.sync(teamService);

        // What a revive does: the survivor becomes the slender.
        survivors.removePlayer(player);
        teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow().addPlayer(player);
        display.sync(teamService);

        TeamManager teamManager = env.process().team();
        assertFalse(teamManager.getTeam(GameConfig.SURVIVOR_TEAM_NAME).getMembers().contains(player.getUsername()),
                "a revived slender left on the survivor team would still read every survivor name");
        assertTrue(teamManager.getTeam(GameConfig.SLENDER_TEAM_NAME).getMembers().contains(player.getUsername()));

        env.destroyInstance(instance, true);
    }
}
