package net.onelitefeather.cygnus.movement;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public class PlayerStopSprintingEvent implements PlayerInstanceEvent {

    private final Player player;

    public PlayerStopSprintingEvent(Player player) {
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
