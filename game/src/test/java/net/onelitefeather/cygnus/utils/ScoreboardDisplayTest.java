package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.key.Key;
import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.component.team.ColorComponent;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class ScoreboardDisplayTest {

    static TeamService teamService;

    @BeforeAll
    static void init() {
        teamService = TeamService.of();
    }

    @AfterEach
    void tearDown() {
        teamService.clear();
    }

    @Test
    void testScoreboardDisplay(@NotNull Env env) {
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        slenderTeam.add(ColorComponent.class, new ColorComponent(ColorData.BLACK));
        slenderTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SLENDER_TEAM_NAME));

        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 10);
        survivorTeam.add(ColorComponent.class, new ColorComponent(ColorData.GREEN));
        survivorTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SURVIVOR_TEAM_NAME));

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 11);
        spectatorTeam.add(ColorComponent.class, new ColorComponent(ColorData.GRAY));
        spectatorTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SPECTATOR_TEAM_NAME));

        teamService.add(slenderTeam);
        teamService.add(survivorTeam);
        teamService.add(spectatorTeam);

        ScoreboardDisplay scoreboardDisplay = new ScoreboardDisplay(teamService.getTeams());
        assertNotNull(scoreboardDisplay);
        TeamManager teamManager = env.process().team();
        assertEquals(3, teamManager.getTeams().size());

        net.minestom.server.scoreboard.Team sbSurvivor = teamManager.getTeam(GameConfig.SURVIVOR_TEAM_NAME);
        assertNotNull(sbSurvivor);
        assertEquals(net.minestom.server.network.packet.server.play.TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS, sbSurvivor.getNameTagVisibility());

        net.minestom.server.scoreboard.Team sbSlender = teamManager.getTeam(GameConfig.SLENDER_TEAM_NAME);
        assertNotNull(sbSlender);
        assertEquals(net.minestom.server.network.packet.server.play.TeamsPacket.NameTagVisibility.NEVER, sbSlender.getNameTagVisibility());

        net.minestom.server.scoreboard.Team sbSpectator = teamManager.getTeam(GameConfig.SPECTATOR_TEAM_NAME);
        assertNotNull(sbSpectator);
        assertEquals(net.minestom.server.network.packet.server.play.TeamsPacket.NameTagVisibility.NEVER, sbSpectator.getNameTagVisibility());
    }

    @Test
    void testScoreboardDisplayFlow(@NotNull Env env) {
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        slenderTeam.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        slenderTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SLENDER_TEAM_NAME));

        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 10);
        survivorTeam.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        survivorTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SURVIVOR_TEAM_NAME));

        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        Instance instance = env.createFlatInstance();
        Player testPlayer = env.createPlayer(instance);

        ScoreboardDisplay scoreboardDisplay = new ScoreboardDisplay(teamService.getTeams());
        assertNotNull(scoreboardDisplay);

        TeamManager teamManager = env.process().team();
        scoreboardDisplay.addPlayer(testPlayer, GameConfig.SLENDER_KEY);
        String rawTeamName = slenderTeam.get(TeamNameComponent.class).teamName();
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.removePlayer(testPlayer, GameConfig.SLENDER_KEY);
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));


        scoreboardDisplay.addPlayer(testPlayer, GameConfig.SURVIVOR_KEY);
        rawTeamName = survivorTeam.get(TeamNameComponent.class).teamName();
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.removePlayer(testPlayer, GameConfig.SURVIVOR_KEY);
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));

        scoreboardDisplay.addPlayer(testPlayer, GameConfig.SURVIVOR_KEY);
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.removePlayer(testPlayer);
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));

        scoreboardDisplay.addPlayer(testPlayer, GameConfig.SURVIVOR_KEY);
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.clear();
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
    }
}
