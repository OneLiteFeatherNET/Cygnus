package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when a new player is selected as Slender because the original Slender has left the round.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S6206")
public final class SlenderReviveEvent implements PlayerEvent {

    private final Player player;

    /**
     * Creates a new instance of the {@link SlenderReviveEvent}.
     *
     * @param player who should be revived
     */
    public SlenderReviveEvent(Player player) {
        this.player = player;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }
}
