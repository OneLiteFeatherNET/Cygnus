package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.guira.SetupDataService;

import java.util.function.Consumer;

/**
 * Appends a point to the active creek route when a block is left-clicked in the creek route mode.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRouteListener implements Consumer<PlayerBlockBreakEvent> {

    private final SetupDataService dataService;

    public CreekRouteListener(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void accept(PlayerBlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasTag(SetupTags.SETUP_ID_TAG)) return;
        if (!(this.dataService.get(player.getUuid()).orElse(null) instanceof GameData gameData)) return;
        if (!gameData.hasCreekRouteMode()) return;
        event.setCancelled(true);

        String active = gameData.activeCreekRoute();
        if (active == null || !gameData.addCreekPoint(pointOnTop(event.getBlockPosition()))) {
            player.sendMessage(SetupMessages.NO_ACTIVE_CREEK_ROUTE);
            return;
        }
        player.sendMessage(SetupMessages.getCreekPointAdded(active, gameData.activeCreekPointCount()));
    }

    /**
     * Returns where the creek's feet stand on a block: in the middle, on top of it.
     *
     * @param block the clicked block
     * @return the point
     */
    static Vec pointOnTop(Point block) {
        return new Vec(block.blockX() + 0.5D, block.blockY() + 1.0D, block.blockZ() + 0.5D);
    }
}
