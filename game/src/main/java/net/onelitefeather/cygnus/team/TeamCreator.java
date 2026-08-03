package net.onelitefeather.cygnus.team;

import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.component.team.ColorComponent;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.onelitefeather.cygnus.common.config.GameConfig;

import static net.onelitefeather.cygnus.common.config.GameConfig.SLENDER_KEY;
import static net.onelitefeather.cygnus.common.config.GameConfig.SURVIVOR_KEY;

/**
 * The interface contains a method to create the team objects which are required for the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public interface TeamCreator {

    /**
     * Creates the teams for the game.
     *
     * @param gameConfig      the configuration to get some values from it
     * @param teamService     the service to add the teams
     */
    default void createTeams(GameConfig gameConfig, TeamService teamService) {
        Team slenderTeam = Team.of(SLENDER_KEY, gameConfig.slenderTeamSize());
        slenderTeam.add(ColorComponent.class, new ColorComponent(ColorData.BLACK));
        slenderTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SLENDER_TEAM_NAME));

        teamService.add(slenderTeam);

        Team survivorTeam = Team.of(SURVIVOR_KEY, gameConfig.survivorTeamSize());
        survivorTeam.add(ColorComponent.class, new ColorComponent(ColorData.GREEN));
        survivorTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SURVIVOR_TEAM_NAME));
        teamService.add(survivorTeam);
    }
}
