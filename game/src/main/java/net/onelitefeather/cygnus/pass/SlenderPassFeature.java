package net.onelitefeather.cygnus.pass;

import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.utils.Items;

/**
 * Brings the slender pass to the players: the lobby item that reserves the Slender, the {@code /pass} command and
 * the release of a reservation when its holder leaves.
 * <p>
 * The feature is disabled while passes are not stored; its command is still registered and answers that the
 * feature is unavailable. The item is handed out by the lobby spawn listener, and taken away when the round starts.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class SlenderPassFeature implements GameFeature {

    private final EventNode<Event> node = EventNode.all("slender-pass");
    private final SlenderPassService service;
    private final ConnectionManager connections;

    /**
     * Creates the feature and puts its listeners on its node.
     *
     * @param service     the service the item and the command act on
     * @param connections finds the online players
     */
    public SlenderPassFeature(SlenderPassService service, ConnectionManager connections) {
        this.service = service;
        this.connections = connections;
        registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    @Override
    public boolean enabled() {
        return this.service.available();
    }

    @Override
    public void registerCommands(CommandManager manager) {
        manager.register(new PassCommand(this.service));
    }

    private void registerListeners() {
        this.node.addListener(PlayerUseItemEvent.class, event -> {
            if (Items.isPassItem(event.getItemStack())) {
                this.service.toggle(event.getPlayer());
            }
        });
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.service.release(event.getPlayer().getUuid()));
        this.node.addListener(GameStartEvent.class, _ -> this.connections.getOnlinePlayers().forEach(Items::removePassItem));
        this.node.addListener(ItemDropEvent.class, event -> {
            if (Items.isPassItem(event.getItemStack())) {
                event.setCancelled(true);
            }
        });
    }
}
