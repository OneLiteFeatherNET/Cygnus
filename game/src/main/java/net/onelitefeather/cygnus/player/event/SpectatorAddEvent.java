package net.onelitefeather.cygnus.player.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

public class SpectatorAddEvent implements PlayerEvent {

    private final Player player;

    public SpectatorAddEvent(Player player) {
        this.player = player;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return player;
    }
}
