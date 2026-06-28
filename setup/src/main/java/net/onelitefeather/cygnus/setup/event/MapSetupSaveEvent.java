package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when a {@link Player} wants to save the data from a map.
 *
 * @version 1.0.0
 * @since 2.1.0
 * @author theEvilReaper
 */
public final class MapSetupSaveEvent implements PlayerEvent {

    private final Player player;

    /**
     * Creates a new instance of the event with the given parameters.
     *
     * @param player    which is involved
     */
    public MapSetupSaveEvent(Player player) {
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
