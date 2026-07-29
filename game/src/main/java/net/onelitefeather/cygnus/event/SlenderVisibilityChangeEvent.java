package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when the slender changes their visibility state.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.6
 **/
@SuppressWarnings("java:S6206")
public final class SlenderVisibilityChangeEvent implements PlayerEvent {

    private final Player player;
    private final boolean hidden;

    /**
     * Creates a new instance of the {@link SlenderVisibilityChangeEvent}.
     *
     * @param player the slender player whose visibility changed
     * @param hidden true if the slender is now hidden, false if visible
     */
    public SlenderVisibilityChangeEvent(Player player, boolean hidden) {
        this.player = player;
        this.hidden = hidden;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns whether the slender is now hidden.
     *
     * @return true if hidden, false if visible
     */
    public boolean isHidden() {
        return hidden;
    }
}
