package net.onelitefeather.cygnus.common;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class ListenerHandlingTest {

    @Test
    void testCancelListenerCreation(@NotNull Env env) {
        ListenerHandling listenerHandling = new ListenerHandling() {};
        GlobalEventHandler eventHandler = env.process().eventHandler();
        listenerHandling.registerCancelListener(eventHandler);

        assertTrue(eventHandler.hasListener(PlayerBlockBreakEvent.class));
        assertTrue(eventHandler.hasListener(PlayerBlockPlaceEvent.class));
        assertTrue(eventHandler.hasListener(ItemDropEvent.class));
        assertTrue(eventHandler.hasListener(PlayerSwapItemEvent.class));
        assertTrue(eventHandler.hasListener(PlayerBlockInteractEvent.class));
        assertFalse(eventHandler.hasListener(InventoryPreClickEvent.class));
    }

    @Test
    void testOnlyOwnInventoryClicksAreCancelled(@NotNull Env env) {
        ListenerHandling listenerHandling = new ListenerHandling() {};
        GlobalEventHandler eventHandler = env.process().eventHandler();
        listenerHandling.registerInventoryLock(eventHandler);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(0, 40, 0));

        InventoryPreClickEvent own = new InventoryPreClickEvent(player.getInventory(), player, new Click.Left(0));
        InventoryPreClickEvent opened = new InventoryPreClickEvent(
                new Inventory(InventoryType.CHEST_1_ROW, Component.text("Menu")), player, new Click.Left(0));
        EventDispatcher.call(own);
        EventDispatcher.call(opened);

        assertTrue(own.isCancelled());
        assertFalse(opened.isCancelled());
        env.destroyInstance(instance, true);
    }
}