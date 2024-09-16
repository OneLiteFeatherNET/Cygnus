package net.onelitefeather.cygnus.common;

import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
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
    }
}