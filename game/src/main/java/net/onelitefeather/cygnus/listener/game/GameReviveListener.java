package net.onelitefeather.cygnus.listener.game;

import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public class GameReviveListener implements Consumer<SlenderReviveEvent> {

    private final GameMap gameMap;
    private final Items items;
    private final StaminaService staminaService;

    public GameReviveListener(@NotNull GameMap gameMap, @NotNull Items items, @NotNull StaminaService staminaService) {
        this.gameMap = gameMap;
        this.items = items;
        this.staminaService = staminaService;
    }

    @Override
    public void accept(SlenderReviveEvent event) {
        var player = event.getPlayer();
        staminaService.switchToSlenderBar(player);
        player.setTag(Tags.GAME_TAG, Helper.SLENDER_ID);
        player.teleport(gameMap.getSlenderSpawn());
        items.setSlenderEye(player);
    }
}
