package net.onelitefeather.cygnus.player;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.inventory.PlayerInventory;
import net.onelitefeather.cygnus.GameFeature;

/**
 * Keeps the items the game hands out where the game put them.
 * <p>
 * Players may neither drop them, nor move them around in their own inventory, nor swap them into
 * the offhand. A dropped item would not even land on the ground, Minestom only takes it out of the
 * inventory, and the SlenderEye or the tracker would be gone for the rest of the round. Clicks in
 * an opened inventory, like the spectator menu, are left to that inventory. Creative players stay
 * free, so builders can still work.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public final class InventoryLock implements GameFeature {

    private final EventNode<Event> node = EventNode.all("inventory-lock");

    /**
     * Creates the lock.
     */
    public InventoryLock() {
        this.node.addListener(ItemDropEvent.class, event -> event.setCancelled(isLocked(event.getPlayer())));
        this.node.addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(isLocked(event.getPlayer())));
        this.node.addListener(InventoryPreClickEvent.class, event -> {
            if (event.getInventory() instanceof PlayerInventory && isLocked(event.getPlayer())) {
                event.setCancelled(true);
            }
        });
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Tells whether a player's inventory is locked.
     *
     * @param player the player
     * @return {@code true} for everyone outside creative mode
     */
    static boolean isLocked(Player player) {
        return player.getGameMode() != GameMode.CREATIVE;
    }
}
