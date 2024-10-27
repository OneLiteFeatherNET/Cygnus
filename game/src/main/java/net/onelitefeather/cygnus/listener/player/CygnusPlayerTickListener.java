package net.onelitefeather.cygnus.listener.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class CygnusPlayerTickListener implements Consumer<PlayerTickEvent> {

    @Override
    public void accept(@NotNull PlayerTickEvent event) {
        Player player = event.getPlayer();

        if (!(player instanceof CygnusPlayer cygnusPlayer)) return;
        if (!cygnusPlayer.hasBlockedSprinting()) return;
        cygnusPlayer.sendPacket(cygnusPlayer.getPropertiesPacket());
    }
}
