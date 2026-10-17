package net.onelitefeather.cygnus.listener;

import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;

import java.util.function.Consumer;

/**
 * Reports the configured maximum player count in the server list ping.
 * <p>
 * Minestom answers a ping with {@code online + 1} as the maximum, which does not match the limit
 * enforced by the {@link PlayerLoginListener}. Everything else of the default response (description,
 * favicon, online count and sample) is kept.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 **/
public final class ServerListPingListener implements Consumer<ServerListPingEvent> {

    private final int maxPlayers;

    public ServerListPingListener(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void accept(ServerListPingEvent event) {
        Status status = event.getStatus();
        Status.PlayerInfo current = status.playerInfo();
        if (current == null) return;
        Status.PlayerInfo updated = Status.PlayerInfo.builder(current).maxPlayers(this.maxPlayers).build();
        event.setStatus(Status.builder(status).playerInfo(updated).build());
    }
}
