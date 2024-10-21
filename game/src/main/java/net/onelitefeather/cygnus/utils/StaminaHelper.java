package net.onelitefeather.cygnus.utils;

import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.stamina.StaminaService;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link StaminaHelper} is a utility class which contains some helper methods for the stamina system.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class StaminaHelper {

    /**
     * Initializes the stamina objects for the slender and the survivors.
     *
     * @param teamService    The service which contains the teams
     * @param staminaService The service which contains the stamina bars
     */
    public static void initStaminaObjects(@NotNull TeamService<Team> teamService, @NotNull StaminaService staminaService) {
        Team slenderTeam = teamService.getTeams().getFirst();
        Team survivorTeam = teamService.getTeams().getLast();

        Player slenderPlayer = TeamHelper.prepareTeamAllocation(slenderTeam, survivorTeam);

        staminaService.setSlenderBar(slenderPlayer, true);
        staminaService.createStaminaBars(survivorTeam);
    }

    private StaminaHelper() {
        throw new UnsupportedOperationException();
    }
}
