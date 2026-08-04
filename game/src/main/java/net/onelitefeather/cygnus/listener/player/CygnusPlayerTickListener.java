package net.onelitefeather.cygnus.listener.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.player.CygnusPlayer;

import java.util.function.Consumer;

public final class CygnusPlayerTickListener implements Consumer<PlayerTickEvent> {

    private final JumpScareManager jumpscareManager;

    public CygnusPlayerTickListener(JumpScareManager jumpscareManager) {
        this.jumpscareManager = jumpscareManager;
    }

    @Override
    public void accept(PlayerTickEvent event) {
        Player player = event.getPlayer();

        this.jumpscareManager.checkTurnAround(player);

        if (!(player instanceof CygnusPlayer cygnusPlayer)) return;
        if (!cygnusPlayer.hasBlockedSprinting()) return;
        cygnusPlayer.sendPacket(cygnusPlayer.getPropertiesPacket());
        cygnusPlayer.sendPacket(cygnusPlayer.getMetadataPacket());
    }
}

