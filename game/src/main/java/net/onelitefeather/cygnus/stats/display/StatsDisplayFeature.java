package net.onelitefeather.cygnus.stats.display;

import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.player.event.SpectatorStatsRequestEvent;
import net.onelitefeather.cygnus.utils.Items;
import net.theevilreaper.xerus.api.phase.Phase;

import java.util.function.Supplier;

/**
 * Brings the statistics to the players: the {@code /stats} command, the lobby item that opens their own
 * menu, and the cache that makes the click instant.
 * <p>
 * The item is handed out on the first spawn while the lobby or the waiting phase runs, and taken away
 * again when the round starts. The item and the menu are locked: nothing can be dropped, swapped or
 * moved, and every click in the menu is cancelled. While statistics are off the feature is disabled and
 * hands out nothing; the command stays registered so it can say that statistics are not available.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class StatsDisplayFeature implements GameFeature {

    private final EventNode<Event> node = EventNode.all("stats-display");
    private final PlayerStatsService service;
    private final Supplier<Phase> phase;
    private final ConnectionManager connections;

    /**
     * Creates the feature and puts its listeners on its node.
     *
     * @param service     reads and shows the statistics
     * @param phase       reads the running phase
     * @param connections finds the online players
     */
    public StatsDisplayFeature(PlayerStatsService service, Supplier<Phase> phase, ConnectionManager connections) {
        this.service = service;
        this.phase = phase;
        this.connections = connections;
        registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    @Override
    public boolean enabled() {
        return this.service.enabled();
    }

    @Override
    public void registerCommands(CommandManager manager) {
        manager.register(new StatsCommand(this.service));
    }

    private void registerListeners() {
        this.node.addListener(PlayerSpawnEvent.class, this::onSpawn);
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.service.forget(event.getPlayer()));
        this.node.addListener(PlayerUseItemEvent.class, event -> {
            if (Items.isStatsItem(event.getItemStack())) {
                this.service.openOwnMenu(event.getPlayer());
            }
        });
        this.node.addListener(GameStartEvent.class, _ -> this.connections.getOnlinePlayers().forEach(Items::removeStatsItem));
        this.node.addListener(GameFinishEvent.class, _ -> this.service.invalidate());
        this.node.addListener(ItemDropEvent.class, event -> {
            if (Items.isStatsItem(event.getItemStack())) {
                event.setCancelled(true);
            }
        });
        this.node.addListener(PlayerSwapItemEvent.class, event -> {
            if (Items.isStatsItem(event.getMainHandItem()) || Items.isStatsItem(event.getOffHandItem())) {
                event.setCancelled(true);
            }
        });
        this.node.addListener(InventoryPreClickEvent.class, this::onClick);
        this.node.addListener(SpectatorStatsRequestEvent.class, event -> this.service.openMenu(event.getPlayer(),
                event.getTarget().getUsername(), this.service.currentYear()));
    }

    private void onSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn() || !this.service.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        this.service.preload(player);
        Phase running = this.phase.get();
        if (running instanceof LobbyPhase || running instanceof WaitingPhase) {
            Items.setStatsItem(player);
        }
    }

    /**
     * Cancels every click that could change the menu or move the item: any click in the menu, any click
     * in the player's own inventory while the menu is open, and any click in the inventory of a player
     * who carries the item.
     */
    private void onClick(InventoryPreClickEvent event) {
        Player player = event.getPlayer();
        boolean menu = event.getInventory() instanceof StatsMenu || player.getOpenInventory() instanceof StatsMenu;
        boolean item = event.getInventory() instanceof PlayerInventory && Items.hasStatsItem(player);
        if (menu || item) {
            event.setCancelled(true);
        }
    }
}
