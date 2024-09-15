package net.onelitefeather.cygnus.movement;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player starts sprinting.
 */
public class PlayerStartSprintingEvent implements PlayerInstanceEvent, CancellableEvent {

    private final Player player;
    private boolean cancelled;

    public PlayerStartSprintingEvent(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
