package net.onelitefeather.cygnus.utils;

import de.icevizion.xerus.api.ColorData;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import de.icevizion.xerus.api.team.TeamServiceImpl;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    static TeamService<Team> teamService;

    @BeforeAll
    static void init() {
       teamService = new TeamServiceImpl<>();
    }

    @AfterEach
    void tearDown() {
        teamService.clear();
    }

    @Test
    void testScoreboardDisplay(@NotNull Env env) {
        Team team = Team.builder().name("Test").capacity(10).colorData(ColorData.AQUA).build();
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
        Team slenderTeam = Team.builder().name(GameConfig.SLENDER_TEAM_NAME).capacity(1).colorData(ColorData.AQUA).build();
        Team survivorTeam = Team.builder().name(GameConfig.SURVIVOR_TEAM_NAME).capacity(10).colorData(ColorData.AQUA).build();

        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        Instance instance = env.createFlatInstance();
        Player testPlayer = env.createPlayer(instance);

        ScoreboardDisplay scoreboardDisplay = new ScoreboardDisplay(teamService.getTeams());
        assertNotNull(scoreboardDisplay);

        TeamManager teamManager = env.process().team();
        scoreboardDisplay.addPlayer(testPlayer, (byte) 0x00);
        String rawTeamName = PlainTextComponentSerializer.plainText().serialize(slenderTeam.getName());
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.removePlayer(testPlayer, (byte) 0x00);
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));


        scoreboardDisplay.addPlayer(testPlayer, (byte) 0x01);
        rawTeamName = PlainTextComponentSerializer.plainText().serialize(survivorTeam.getName());
        assertTrue(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
        scoreboardDisplay.removePlayer(testPlayer, (byte) 0x01);
        assertFalse(teamManager.getTeam(rawTeamName).getMembers().contains(testPlayer.getUsername()));
    }
}
