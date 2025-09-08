package net.onelitefeather.cygnus;

import net.kyori.adventure.key.Key;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.config.GameConfig;
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
            @NotNull GameConfig gameConfig,
            @NotNull TeamService teamService,
            @NotNull AmbientProvider ambientProvider
    ) {
        teamService.add(Team.of(Key.key("cygnus", GameConfig.SLENDER_TEAM_NAME))
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
