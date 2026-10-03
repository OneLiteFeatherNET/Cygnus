package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.key.Key;
import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.component.team.ColorComponent;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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
    }

    @Test
    @DisplayName("Moving a survivor to the slender side never removes them from a team the client no longer has them in")
    void promotionKeepsTheClientConsistent(@NotNull Env env) {
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        slenderTeam.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        slenderTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SLENDER_TEAM_NAME));
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 10);
        survivorTeam.add(ColorComponent.class, new ColorComponent(ColorData.AQUA));
        survivorTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SURVIVOR_TEAM_NAME));
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        Instance instance = env.createFlatInstance();
        // Distinct names: the scoreboard teams know players by name only
        TestConnection connection = env.createConnection(new GameProfile(UUID.randomUUID(), "Promoted"));
        Player promoted = connection.connect(instance);
        Player other = env.createConnection(new GameProfile(UUID.randomUUID(), "Other")).connect(instance);
        survivorTeam.addPlayer(promoted);
        survivorTeam.addPlayer(other);
        ScoreboardDisplay display = new ScoreboardDisplay(teamService.getTeams());
        display.sync(teamService);

        // What PlayerQuitListener does when the slender leaves and a survivor takes over
        survivorTeam.removePlayer(promoted);
        slenderTeam.addPlayer(promoted);
        Collector<TeamsPacket> packets = connection.trackIncoming(TeamsPacket.class);
        display.sync(teamService);

        // The vanilla client: an addition silently moves a player out of their old team, a removal
        // from a team they are not on throws and disconnects.
        Map<String, String> teamOf = new HashMap<>();
        teamOf.put(promoted.getUsername(), GameConfig.SURVIVOR_TEAM_NAME);
        teamOf.put(other.getUsername(), GameConfig.SURVIVOR_TEAM_NAME);
        for (TeamsPacket packet : packets.collect()) {
            if (packet.action() instanceof TeamsPacket.AddEntitiesToTeamAction add) {
                add.entities().forEach(name -> teamOf.put(name, packet.teamName()));
            } else if (packet.action() instanceof TeamsPacket.RemoveEntitiesToTeamAction remove) {
                for (String name : remove.entities()) {
                    assertEquals(packet.teamName(), teamOf.get(name),
                            "the client would disconnect: " + name + " is not on " + packet.teamName());
                    teamOf.remove(name);
                }
            }
        }
        assertEquals(GameConfig.SLENDER_TEAM_NAME, teamOf.get(promoted.getUsername()));
        assertEquals(GameConfig.SURVIVOR_TEAM_NAME, teamOf.get(other.getUsername()));
    }
}
