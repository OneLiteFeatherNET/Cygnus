package net.onelitefeather.spectator.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The SpectatorAddEvent is called when a player is added to the spectator list.
 * This event is cancellable, allowing you to prevent the player from being added to the spectator list.
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public class SpectatorAddEvent implements PlayerEvent, CancellableEvent {

    private final Player player;
    private boolean cancelled;

    /**
     * Creates a new instance of the {@link SpectatorAddEvent}.
     *
     * @param player the player to be added to the spectator list
     */
    public SpectatorAddEvent(@NotNull Player player) {
        this.player = player;
    }

    /**
     * Returns an indication of whether the event has been cancelled.
     *
     * @return {@code true} if the event has been cancelled, {@code false} otherwise
     */
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Sets the cancelled state of the event.
     *
     * @param cancel {@code true} to cancel the event, {@code false} otherwise
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Retrieves the player which is being added to the spectator list.
     *
     * @return a non-null {@link Player} reference
     */
    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }
}
