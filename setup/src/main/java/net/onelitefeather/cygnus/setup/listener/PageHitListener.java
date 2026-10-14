package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientAttackPacket;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.guira.SetupDataService;

import java.util.function.Consumer;

/**
 * Hands the builder's hits on the pages of their page preview to it.
 * <p>
 * The preview's pages exist only as packets, so the server knows no entity behind them and none of
 * its interaction events fire. The listener reads the raw hit instead. Every packet passes through
 * here, so anything that is not a hit is let go at once.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.18.0
 */
public final class PageHitListener implements Consumer<PlayerPacketEvent> {

    private final SetupDataService dataService;

    /**
     * Creates the listener.
     *
     * @param dataService looks up the setup a player is working on
     */
    public PageHitListener(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void accept(PlayerPacketEvent event) {
        if (!(event.getPacket() instanceof ClientAttackPacket hit)) return;
        if (!(this.dataService.get(event.getPlayer().getUuid()).orElse(null) instanceof GameData data)) return;
        // A page the server does not know: nothing else should try to handle the hit.
        if (data.hitPage(hit.targetId())) event.setCancelled(true);
    }
}
