package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.cygnus.stamina.StaminaBar;

/**
 * Called when the state of a StaminaBar changes.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S6206")
public final class StaminaStateChangeEvent implements PlayerEvent {

    private final Player player;
    private final StaminaBar.State state;

    /**
     * Creates a new instance of the {@link StaminaStateChangeEvent}.
     *
     * @param player the player whose stamina state changed
     * @param state the new state
     */
    public StaminaStateChangeEvent(Player player, StaminaBar.State state) {
        this.player = player;
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns the new state of the stamina bar.
     *
     * @return the state
     */
    public StaminaBar.State getState() {
        return state;
    }
}
