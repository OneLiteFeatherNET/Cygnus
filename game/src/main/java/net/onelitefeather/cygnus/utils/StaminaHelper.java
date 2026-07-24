package net.onelitefeather.cygnus.utils;

import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.onelitefeather.cygnus.stamina.StaminaService;


/**
 * The {@link StaminaHelper} is a utility class which contains some helper methods for the stamina system.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 */
public final class StaminaHelper {

    /**
     * Initializes the stamina objects for the slender and the survivors.
     *
     * @param teamService    The service that contains the teams
     * @param staminaService The service that contains the stamina bars
     */
    public static void initStaminaObjects(TeamService teamService, StaminaService staminaService) {
        Team slenderTeam = teamService.getTeams().getFirst();
        Team survivorTeam = teamService.getTeams().getLast();

        TeamHelper.TeamAllocation allocation = TeamHelper.prepareTeamAllocation(slenderTeam, survivorTeam);

        staminaService.setSlenderBar(allocation.slender(), true);
        staminaService.createStaminaBars(allocation.survivors());
    }

    private StaminaHelper() {
        throw new UnsupportedOperationException();
    }
}