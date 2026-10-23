package net.onelitefeather.cygnus.bridge;

import net.onelitefeather.cygnus.common.event.LobbyReturnRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Reacts to {@link LobbyReturnRequestEvent} by sending the player to the network lobby.
 * <p>
 * The event is marked as handled only when the connect went through. Failures are logged and never
 * propagate into the game thread; the unhandled event makes the game use its own fallback.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.19.0
 */
public final class LobbyReturnListener implements Consumer<LobbyReturnRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyReturnListener.class);

    private final LobbyConnector connector;

    /**
     * Creates a new listener.
     *
     * @param connector sends the player to the lobby
     */
    public LobbyReturnListener(LobbyConnector connector) {
        this.connector = connector;
    }

    @Override
    public void accept(LobbyReturnRequestEvent event) {
        try {
            connector.connect(event.player().getUuid());
            event.markHandled();
        } catch (Throwable throwable) {
            LOGGER.error("Could not send {} to the lobby", event.player().getUsername(), throwable);
        }
    }
}
