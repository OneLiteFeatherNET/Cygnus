package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when a survivor takes their painkillers and the healing starts.
 * <p>
 * A refused take - nothing missing, already used, not a survivor - does not raise it.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
@SuppressWarnings("java:S6206")
public final class PainkillerUsedEvent implements PlayerEvent {

    private final Player player;

    /**
     * Creates the event.
     *
     * @param player the survivor who took the painkillers
     */
    public PainkillerUsedEvent(Player player) {
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
