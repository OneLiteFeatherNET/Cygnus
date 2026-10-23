package net.onelitefeather.cygnus.bridge;

import java.util.UUID;

/**
 * Sends a player to the network lobby.
 * <p>
 * The indirection keeps {@link LobbyReturnListener} free of any CloudNet type, so it can be tested
 * without a CloudNet node.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.19.0
 */
@FunctionalInterface
public interface LobbyConnector {

    /**
     * Connects the player to a lobby service.
     *
     * @param playerId the unique id of the player
     * @throws Exception if the player cannot be connected
     */
    void connect(UUID playerId) throws Exception;
}
