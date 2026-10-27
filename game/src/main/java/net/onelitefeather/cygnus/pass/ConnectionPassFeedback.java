package net.onelitefeather.cygnus.pass;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.utils.Items;

import java.util.UUID;

/**
 * Delivers the pass feedback to the online players of this server.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class ConnectionPassFeedback implements PassFeedback {

    private final ConnectionManager connections;

    /**
     * Creates the feedback on the server's connections.
     *
     * @param connections finds the online players
     */
    public ConnectionPassFeedback(ConnectionManager connections) {
        this.connections = connections;
    }

    @Override
    public void tell(UUID player, Component message) {
        Player online = this.connections.getOnlinePlayerByUuid(player);
        if (online != null) {
            online.sendMessage(message);
        }
    }

    @Override
    public void tellLobby(Component message, UUID except) {
        this.connections.getOnlinePlayers().stream()
                .filter(player -> !player.getUuid().equals(except))
                .forEach(player -> player.sendMessage(message));
    }

    @Override
    public void showBalance(UUID player, int balance) {
        Player online = this.connections.getOnlinePlayerByUuid(player);
        if (online != null) {
            Items.setPassItem(online, balance);
        }
    }
}
