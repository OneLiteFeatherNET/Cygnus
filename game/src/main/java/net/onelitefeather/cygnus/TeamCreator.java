package net.onelitefeather.cygnus;

import de.icevizion.xerus.api.ColorData;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.config.GameConfig;
import org.jetbrains.annotations.NotNull;

public interface TeamCreator {

    default void createTeams(@NotNull GameConfig gameConfig, @NotNull TeamService<Team> teamService, @NotNull AmbientProvider ambientProvider) {
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
