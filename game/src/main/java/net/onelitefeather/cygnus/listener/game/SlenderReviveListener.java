package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.Items;

import java.util.function.Consumer;

/**
 * This class is responsible for handling the {@link SlenderReviveEvent} and performing the necessary actions to revive the player in the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public class SlenderReviveListener implements Consumer<SlenderReviveEvent> {

    private final GameMap gameMap;
    private final StaminaService staminaService;

    public SlenderReviveListener(GameMap gameMap, StaminaService staminaService) {
        this.gameMap = gameMap;
        this.staminaService = staminaService;
    }

    @Override
    public void accept(SlenderReviveEvent event) {
        Player player = event.getPlayer();
        staminaService.setSlenderBar(player, true);
        player.setTag(Tags.GAME_TAG, Helper.SLENDER_ID);
        player.teleport(gameMap.getSlenderSpawn());
        Items.setSlenderEye(player);
    }
}
