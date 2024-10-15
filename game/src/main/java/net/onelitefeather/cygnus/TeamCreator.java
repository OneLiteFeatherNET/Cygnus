package net.onelitefeather.cygnus;

import de.icevizion.xerus.api.ColorData;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.configv2.GameConfig;
import org.jetbrains.annotations.NotNull;

/**
 * The interface contains a method to create the teams objects which are required for the game.
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
     * @param ambientProvider the provider to set the ambient team
     */
    default void createTeams(
            @NotNull net.onelitefeather.cygnus.common.config.GameConfig gameConfig,
            @NotNull TeamService<Team> teamService,
            @NotNull AmbientProvider ambientProvider
    ) {
        teamService.add(Team.builder()
                .name(GameConfig.SLENDER_TEAM_NAME)
                .capacity(gameConfig.slenderTeamSize())
                .colorData(ColorData.BLACK)
                .build());
        var survivorTeam = Team.builder()
                .name(GameConfig.SURVIVOR_TEAM_NAME)
                .capacity(gameConfig.survivorTeamSize())
                .colorData(ColorData.LIGHT_GREEN)
                .build();
        ambientProvider.setTeam(survivorTeam);
        teamService.add(survivorTeam);
    }
}
