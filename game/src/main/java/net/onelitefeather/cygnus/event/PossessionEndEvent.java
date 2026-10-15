package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when the slender is back in his body after looking through the creek's eyes.
 * <p>
 * His other items were locked meanwhile, so each of them can show its own cooldown again.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 **/
@SuppressWarnings("java:S6206")
public final class PossessionEndEvent implements PlayerEvent {

    private final Player player;

    /**
     * Creates a new instance of the {@link PossessionEndEvent}.
     *
     * @param player the slender who is back in his body
     */
    public PossessionEndEvent(Player player) {
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
