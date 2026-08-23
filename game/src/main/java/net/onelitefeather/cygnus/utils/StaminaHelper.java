package net.onelitefeather.cygnus.utils;

import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.stamina.StaminaService;


/**
 * The {@link StaminaHelper} is a utility class which contains some helper methods for the stamina system.
 *
 * @author theEvilReaper
 * @version 1.3.0
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
        Team slenderTeam = teamService.getTeam(GameConfig.SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"));
        Team survivorTeam = teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"));
        TeamHelper.TeamAllocation allocation = TeamHelper.prepareTeamAllocation(slenderTeam, survivorTeam);

        staminaService.setSlenderBar(allocation.slender(), true);
        staminaService.createStaminaBars(allocation.survivors());
    }

    /**
     * Reads how much of a survivor's stamina is left, as a share between zero and one.
     * <p>
     * Reads a full bar for a player who has none registered yet. The effects that drive themselves
     * off stamina are ticked from a scheduler, so they ask about players who have joined but are not
     * playing a round - answering "untouched" there is what keeps the effect off for them, and it
     * keeps a null check out of every caller.
     * </p>
     *
     * @param staminaService the service holding the bars
     * @param player         the player to read
     * @return the remaining share in {@code [0, 1]}, or {@code 1} while the player has no bar
     */
    public static double remainingShare(StaminaService staminaService, Player player) {
        FoodBar bar = staminaService.getFoodBar(player);
        return bar == null ? 1.0D : bar.remainingShare();
    }

    private StaminaHelper() {
        throw new UnsupportedOperationException();
    }
}