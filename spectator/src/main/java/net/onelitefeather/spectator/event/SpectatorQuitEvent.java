package net.onelitefeather.spectator.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The SpectatorQuitEvent is called when a player leaves the server and is currently in spectator mode.
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public class SpectatorQuitEvent implements PlayerEvent {

    private final Player player;

    /**
     * Creates a new instance of the {@link SpectatorQuitEvent}.
     *
     * @param player the player which is involved in the event
     */
    public SpectatorQuitEvent(@NotNull Player player) {
        this.player = player;
    }

    /**
     * Retrieves the player which left the server while in spectator mode.
     *
     * @return a non-null {@link Player} reference
     */
    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }
}
