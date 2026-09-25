package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.guira.SetupDataService;
import net.onelitefeather.guira.data.SetupData;

import java.util.function.Consumer;

/**
 * Appends a point to the active creek route when a block is broken in the creek route mode, or
 * removes the point that already sits on that block.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRouteListener implements Consumer<PlayerBlockBreakEvent> {

    private final SetupDataService setupService;

    public CreekRouteListener(SetupDataService setupService) {
        this.setupService = setupService;
    }

    @Override
    public void accept(PlayerBlockBreakEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!player.hasTag(SetupTags.SETUP_ID_TAG)) return;

        SetupData setupData = this.setupService.get(player.getUuid()).orElse(null);

        if (setupData == null) return;

        if (!(setupData instanceof GameData gameData) || !gameData.hasCreekRouteMode()) return;

        String activeRoute = gameData.activeCreekRoute();
        if (activeRoute == null) {
            player.sendMessage(SetupMessages.NO_ACTIVE_CREEK_ROUTE);
            return;
        }

        Vec point = pointOnTop(event.getBlockPosition());
        int removed = gameData.removeCreekPointAt(point);
        if (removed > 0) {
            player.sendMessage(SetupMessages.getCreekPointRemovedAt(activeRoute, removed, gameData.activeCreekPointCount()));
            return;
        }

        gameData.addCreekPoint(point);
        player.sendMessage(SetupMessages.getCreekPointAdded(activeRoute, gameData.activeCreekPointCount()));
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
