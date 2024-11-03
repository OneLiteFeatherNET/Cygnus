package net.onelitefeather.spectator.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.listener.ChatMessageListener;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.spectator.SpectatorService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PlayerChatIntegrationTest {

    @Test
    void testSpectatorChatIntegration(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();

        Player player = connection.connect(instance, Pos.ZERO).join();

        SpectatorService spectatorService = SpectatorService.builder()
                .spectatorChat()
                .build();

        assertNotNull(spectatorService);
        assertFalse(spectatorService.hasSpectators());

        spectatorService.add(player);

        assertTrue(spectatorService.hasSpectators());
        assertTrue(spectatorService.isSpectator(player));

        Collector<PlayerChatEvent> chatCollector = env.trackEvent(PlayerChatEvent.class, EventFilter.PLAYER, player);
        ClientChatMessagePacket packet = new ClientChatMessagePacket(
                "Hello World!",
                System.currentTimeMillis(),
                1,
                null,
                1,
                BitSet.valueOf(new byte[0])
        );
        ChatMessageListener.chatMessageListener(packet, player);

        chatCollector.assertSingle(event -> {
            assertEquals(player, event.getPlayer());
            assertFalse(spectatorService.isSpectator(player));

            String message = event.getMessage();
            assertNotNull(message);
            assertFalse(message.isEmpty());
            assertTrue(message.contains("Hello World!"));
        });

        env.destroyInstance(instance, true);
    }
}
