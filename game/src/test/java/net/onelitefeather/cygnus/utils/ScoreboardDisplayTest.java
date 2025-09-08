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

import java.util.Locale;

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
        Team team = Team.of(Key.key("test", "test"), 10);
        team.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        team.add(TeamNameComponent.class, new TeamNameComponent("Test"));
        teamService.add(team);
        ScoreboardDisplay scoreboardDisplay = new ScoreboardDisplay(teamService.getTeams());
        assertNotNull(scoreboardDisplay);
        TeamManager teamManager = env.process().team();
        assertEquals(1, teamManager.getTeams().size());
        net.minestom.server.scoreboard.Team testTeam = teamManager.getTeam("Test");
        assertNotNull(testTeam);
        assertEquals("Test", testTeam.getTeamName());
    }

    @Test
    void testScoreboardDisplayFlow(@NotNull Env env) {
        Team slenderTeam = Team.of(Key.key("cygnus", GameConfig.SLENDER_TEAM_NAME.toLowerCase(Locale.ROOT)), 1);
        slenderTeam.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        slenderTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SLENDER_TEAM_NAME));

        Team survivorTeam = Team.of(Key.key("cygnus", GameConfig.SURVIVOR_TEAM_NAME.toLowerCase(Locale.ROOT)), 10);
        survivorTeam.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        survivorTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SURVIVOR_TEAM_NAME));

        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        Instance instance = env.createFlatInstance();
        Player testPlayer = env.createPlayer(instance);

        ScoreboardDisplay scoreboardDisplay = new ScoreboardDisplay(teamService.getTeams());
        assertNotNull(scoreboardDisplay);

        TeamManager teamManager = env.process().team();
        scoreboardDisplay.addPlayer(testPlayer, (byte) 0x00);
        String rawTeamName = slenderTeam.get(TeamNameComponent.class).teamName();
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.removePlayer(testPlayer, (byte) 0x00);
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));


        scoreboardDisplay.addPlayer(testPlayer, (byte) 0x01);
        rawTeamName = survivorTeam.get(TeamNameComponent.class).teamName();
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.removePlayer(testPlayer, (byte) 0x01);
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
    }
}
