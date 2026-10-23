package net.onelitefeather.cygnus.bridge;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * {@link LobbyConnector} backed by CloudNet.
 * <p>
 * The bridge registers its {@link PlayerManager} in the service registry. Its player executor asks the
 * proxy to move the player to the service of the {@value #LOBBY_TASK} task with the fewest players.
 * The manager is resolved lazily so a missing CloudNet surfaces as an exception the listener handles.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.19.0
 */
public final class CloudNetLobbyConnector implements LobbyConnector {

    /** The CloudNet task of the network lobby. */
    public static final String LOBBY_TASK = "Lobby";

    private static final String PLAYER_MANAGER = "PlayerManager";

    private final Supplier<PlayerManager> players;

    /**
     * Creates a connector that resolves the player manager from CloudNet's service registry.
     */
    public CloudNetLobbyConnector() {
        this(() -> ServiceRegistry.registry().instance(PlayerManager.class, PLAYER_MANAGER));
    }

    CloudNetLobbyConnector(Supplier<PlayerManager> players) {
        this.players = players;
    }

    @Override
    public void connect(UUID playerId) {
        this.players.get().playerExecutor(playerId).connectToTask(LOBBY_TASK, ServerSelectorType.LOWEST_PLAYERS);
    }
}
