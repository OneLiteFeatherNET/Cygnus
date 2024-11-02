package net.onelitefeather.spectator.item;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.listener.UseItemListener;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.FlexibleListener;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.spectator.SpectatorService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class SpectatorHotBarIntegrationTest {

    @Test
    void testHotBarItemSet(@NotNull Env env) {
        ItemStack.Builder teleporterItemBuilder = ItemStack.builder(Material.COMPASS);
        Instance instance = env.createFlatInstance();
        Consumer<Player> teleporterConsumer = player -> player.setGameMode(GameMode.SPECTATOR);

        SpectatorHotBarItem teleporterItem = new SpectatorHotBarItem(teleporterItemBuilder, 1, 0, teleporterConsumer);
        assertNotNull(teleporterItem);
        SpectatorService service = SpectatorService.builder()
                .hotbarItem(teleporterItem)
                .build();
        assertNotNull(service);

        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, Pos.ZERO).join();
        assertEquals(ItemStack.AIR, player.getInventory().getItemStack(0));
        service.add(player);

        assertTrue(service.hasSpectators());
        assertTrue(player.hasTag(SpectatorService.SPECTATOR_TAG));

        ItemStack rawItem = teleporterItemBuilder.build();

        ItemStack teleportStack = player.getInventory().getItemStack(0);
        assertNotNull(teleportStack);
        assertEquals(rawItem, teleportStack);
        player.setHeldItemSlot((byte) 0);

        Collector<PlayerUseItemEvent> useItemCollector = env.trackEvent(PlayerUseItemEvent.class, EventFilter.PLAYER, player);
        UseItemListener.useItemListener(new ClientUseItemPacket(Player.Hand.MAIN, 42, 0f, 0f), player);

        useItemCollector.assertSingle(event -> {
            ItemStack stack = event.getItemStack();
            assertEquals(Material.COMPASS, stack.material());
            assertEquals(rawItem, stack);
            assertTrue(stack.hasTag(SpectatorItem.SPEC_ITEM_TAG));

            int id = stack.getTag(SpectatorItem.SPEC_ITEM_TAG);

            assertEquals(1, id);
            assertEquals(GameMode.SPECTATOR, player.getGameMode());
        });

        env.destroyInstance(instance, true);
    }
}
