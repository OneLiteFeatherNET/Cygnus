package net.onelitefeather.cygnus.lobby;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.event.LobbyReturnRequestEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.utils.Items;
import net.theevilreaper.xerus.api.phase.Phase;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Helps players who just joined the lobby: it reminds them to turn their game audio on, tells them
 * which server they are on and gives them the items to see that again or to return to the network lobby.
 * <p>
 * Everything happens on the first spawn while the lobby or the waiting phase runs. The items are taken
 * away again when the round starts. Without a server name, for example outside of CloudNet, neither the
 * line nor the server item exist.
 * </p>
 * <p>
 * The lobby item does not move the player itself: it fires a {@link LobbyReturnRequestEvent} for the
 * CloudNet bridge. If nobody handles it, the player is kicked so the proxy can move them to its
 * fallback server.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.19.0
 */
public final class LobbyHintsFeature implements GameFeature {

    private final EventNode<Event> node = EventNode.all("lobby-hints");
    private final @Nullable String serverName;
    private final Supplier<Phase> phase;
    private final ConnectionManager connections;
    private final Consumer<LobbyReturnRequestEvent> requests;

    /**
     * Creates the feature that fires its requests through the global event handler.
     *
     * @param serverName  the name of the server, or {@code null} when it is not known
     * @param phase       reads the running phase
     * @param connections finds the online players
     */
    public LobbyHintsFeature(@Nullable String serverName, Supplier<Phase> phase, ConnectionManager connections) {
        this(serverName, phase, connections, EventDispatcher::call);
    }

    /**
     * Creates the feature and puts its listeners on its node.
     *
     * @param serverName  the name of the server, or {@code null} when it is not known
     * @param phase       reads the running phase
     * @param connections finds the online players
     * @param requests    delivers a lobby return request to whoever can handle it
     */
    LobbyHintsFeature(@Nullable String serverName, Supplier<Phase> phase, ConnectionManager connections,
                      Consumer<LobbyReturnRequestEvent> requests) {
        this.serverName = serverName;
        this.phase = phase;
        this.connections = connections;
        this.requests = requests;
        registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    private void registerListeners() {
        this.node.addListener(PlayerSpawnEvent.class, this::onSpawn);
        this.node.addListener(PlayerUseItemEvent.class, this::onUse);
        this.node.addListener(GameStartEvent.class, _ -> this.connections.getOnlinePlayers().forEach(Items::removeLobbyItems));
        this.node.addListener(ItemDropEvent.class, event -> {
            if (isLobbyHintItem(event.getItemStack())) {
                event.setCancelled(true);
            }
        });
        this.node.addListener(PlayerSwapItemEvent.class, event -> {
            if (isLobbyHintItem(event.getMainHandItem()) || isLobbyHintItem(event.getOffHandItem())) {
                event.setCancelled(true);
            }
        });
    }

    private static boolean isLobbyHintItem(ItemStack stack) {
        return Items.isServerItem(stack) || Items.isLobbyItem(stack);
    }

    private void onSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) {
            return;
        }
        Phase running = this.phase.get();
        if (!(running instanceof LobbyPhase) && !(running instanceof WaitingPhase)) {
            return;
        }
        Player player = event.getPlayer();
        player.sendMessage(Messages.audioHint());
        if (this.serverName != null) {
            player.sendMessage(Messages.serverInfo(this.serverName));
            Items.setServerItem(player, this.serverName);
        }
        Items.setLobbyItem(player);
    }

    private void onUse(PlayerUseItemEvent event) {
        Player player = event.getPlayer();
        if (this.serverName != null && Items.isServerItem(event.getItemStack())) {
            player.sendMessage(Messages.serverInfo(this.serverName));
        } else if (Items.isLobbyItem(event.getItemStack())) {
            LobbyReturnRequestEvent request = new LobbyReturnRequestEvent(player);
            this.requests.accept(request);
            if (!request.handled()) {
                player.kick(Messages.LOBBY_RETURN_KICK);
            }
        }
    }
}
