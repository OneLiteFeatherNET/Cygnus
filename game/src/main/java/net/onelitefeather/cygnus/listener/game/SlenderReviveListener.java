package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import net.theevilreaper.xerus.api.team.TeamService;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class is responsible for handling the {@link SlenderReviveEvent} and performing the necessary actions to revive the player in the game.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 **/
public final class SlenderReviveListener implements Consumer<SlenderReviveEvent> {

    private final Supplier<GameMap> gameMapSupplier;
    private final StaminaService staminaService;
    private final TeamService teamService;

    public SlenderReviveListener(Supplier<GameMap> gameMapSupplier, StaminaService staminaService, TeamService teamService) {
        this.gameMapSupplier = gameMapSupplier;
        this.staminaService = staminaService;
        this.teamService = teamService;
    }

    /**
     * Turns the given player into the new slender.
     * <p>
     * Hidden state and viewable rule are set exactly like in the initial team allocation. Without them the
     * revived slender would keep the survivor default and stay visible to everybody for the rest of the round.
     *
     * @param event the revive event carrying the player to promote
     */
    @Override
    public void accept(SlenderReviveEvent event) {
        Player player = event.getPlayer();
        this.staminaService.removePlayer(player);
        this.staminaService.setSlenderBar(player, true);
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        player.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        player.updateViewableRule(VisibilityRules.slenderRule(player));
        VisibilityRules.refresh(player);
        TeamHelper.updateTabList(this.teamService);
        player.sendMessage(Messages.SLENDER_JOIN_PART);
        GameMap gameMap = gameMapSupplier.get();
        if (gameMap != null && gameMap.getSlenderSpawn() != null) {
            player.teleport(gameMap.getSlenderSpawn());
        }
        Items.setSlenderEye(player);
    }
}
