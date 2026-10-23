package net.onelitefeather.cygnus.common.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;

/**
 * The {@link LobbyReturnRequestEvent} is called when a player asks to go back to the network lobby.
 * <p>
 * It is a signal for components outside the game module, for example the CloudNet bridge extension,
 * which must not depend on the game module itself. A component that sends the player away marks the
 * event as {@linkplain #handled() handled}; if nobody does, the game falls back to its own way.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.19.0
 */
public final class LobbyReturnRequestEvent implements Event {

    private final Player player;
    private boolean handled;

    /**
     * Creates a new request.
     *
     * @param player the player who wants to go back to the lobby
     */
    public LobbyReturnRequestEvent(Player player) {
        this.player = player;
    }

    /**
     * Returns the player who wants to go back to the lobby.
     *
     * @return the player
     */
    public Player player() {
        return this.player;
    }

    /**
     * Tells whether a listener took care of sending the player away.
     *
     * @return {@code true} if the request was handled
     */
    public boolean handled() {
        return this.handled;
    }

    /**
     * Marks the request as handled, so the game does not use its fallback.
     */
    public void markHandled() {
        this.handled = true;
    }
}
