package net.onelitefeather.cygnus;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import org.jetbrains.annotations.NotNull;

import static de.icevizion.aves.inventory.util.InventoryConstants.CANCELLABLE_EVENT;

public interface ListenerHandling {

    default void registerCancelListener(@NotNull EventNode<Event> eventNode) {
        eventNode.addListener(PlayerBlockBreakEvent.class, CANCELLABLE_EVENT::accept);
        eventNode.addListener(PlayerBlockPlaceEvent.class, CANCELLABLE_EVENT::accept);
        eventNode.addListener(ItemDropEvent.class, CANCELLABLE_EVENT::accept);
        eventNode.addListener(PlayerSwapItemEvent.class, CANCELLABLE_EVENT::accept);
        eventNode.addListener(PlayerBlockInteractEvent.class, CANCELLABLE_EVENT::accept);
    }
}
