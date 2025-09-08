package net.onelitefeather.cygnus;

import net.theevilreaper.xerus.api.team.TeamService;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TeamCreatorTest {

    @Test
    void testTeamCreation() {
        GameConfig gameConfig = new GameConfigReader(Paths.get("")).getConfig();
        TeamService teamService = TeamService.of();
        AmbientProvider ambientProvider = new AmbientProvider();


        TeamCreator teamCreator = new TeamCreator() {};
        teamCreator.createTeams(gameConfig, teamService, ambientProvider);

        for (int i = 0; i < teamService.getTeams().size(); i++) {
            assertNotNull(teamService.getTeams().get(i));
        }
    }
}
