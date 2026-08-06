package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class is responsible for handling the {@link SlenderReviveEvent} and performing the necessary actions to revive the player in the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public class SlenderReviveListener implements Consumer<SlenderReviveEvent> {

    private final Supplier<GameMap> gameMapSupplier;
    private final StaminaService staminaService;

    public SlenderReviveListener(Supplier<GameMap> gameMapSupplier, StaminaService staminaService) {
        this.gameMapSupplier = gameMapSupplier;
        this.staminaService = staminaService;
    }

    @Override
    public void accept(SlenderReviveEvent event) {
        Player player = event.getPlayer();
        staminaService.setSlenderBar(player, true);
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        GameMap gameMap = gameMapSupplier.get();
        if (gameMap != null && gameMap.getSlenderSpawn() != null) {
            player.teleport(gameMap.getSlenderSpawn());
        }
        Items.setSlenderEye(player);
    }
}
