package net.onelitefeather.cygnus.team;

import net.onelitefeather.cygnus.common.config.GameConfig;
import net.theevilreaper.xerus.api.team.TeamService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamCreatorTest {

    @Test
    void testTeamCreationUsesTheConfiguredSizes() {
        TeamService teamService = TeamService.of();

        new TeamCreator() {
        }.createTeams(new GameConfig.Teams(1, 8), teamService);

        assertEquals(3, teamService.getTeams().size());
        assertEquals(1, teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow().getCapacity());
        assertEquals(8, teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().getCapacity());
        assertEquals(9, teamService.getTeam(GameConfig.SPECTATOR_KEY).orElseThrow().getCapacity(),
                "every survivor can become a spectator, plus one spare slot");
    }
}
