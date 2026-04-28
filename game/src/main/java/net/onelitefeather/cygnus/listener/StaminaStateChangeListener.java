package net.onelitefeather.cygnus.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.PacketSendingUtils;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.stamina.StaminaBar;

import java.util.function.Consumer;

/**
 * Listener for stamina state changes to handle packet broadcasting.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class StaminaStateChangeListener implements Consumer<StaminaStateChangeEvent> {

    @Override
    public void accept(StaminaStateChangeEvent event) {
        Player player = event.getPlayer();
        StaminaBar.State state = event.getState();

        if (state == StaminaBar.State.DRAINING) {
            PacketSendingUtils.broadcastPlayPacket(player.getMetadataPacket());
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .stream().filter(p -> !p.getUuid().equals(player.getUuid())).forEach(player::updateNewViewer);
            PacketSendingUtils.broadcastPlayPacket(player.getMetadataPacket());
            return;
        }

        if (state == StaminaBar.State.REGENERATING) {
            PacketSendingUtils.broadcastPlayPacket(player.getMetadataPacket());
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .stream().filter(p -> !p.getUuid().equals(player.getUuid())).forEach(player::updateOldViewer);
            PacketSendingUtils.broadcastPlayPacket(player.getMetadataPacket());
        }
    }
}
