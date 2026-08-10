package net.onelitefeather.cygnus.listener.map;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.map.event.GameMapLoadedEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameMapLoadedListenerTest extends CygnusPlayerTestBase {

    @Test
    void testBroadcastsMapAnnouncementToOnlinePlayers(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        InstanceContainer gameInstance = (InstanceContainer) env.createFlatInstance();

        TestConnection connection = env.createConnection();
        connection.connect(instance);
        Collector<SystemChatPacket> packets = connection.trackIncoming(SystemChatPacket.class);

        GameMap gameMap = new GameMap(
                "Granskoga",
                Pos.ZERO,
                Pos.ZERO,
                Set.of(),
                Set.of(),
                List.of("Alice", "Bob")
        );

        new GameMapLoadedListener().accept(new GameMapLoadedEvent(gameMap, gameInstance));

        List<SystemChatPacket> received = packets.collect();
        assertEquals(1, received.size(), "Player should have received exactly one map announcement message.");
        assertEquals(
                "\n──────────────────────\nNow playing: Granskoga\nBuilt by: Alice, Bob\n──────────────────────\n",
                PlainTextComponentSerializer.plainText().serialize(received.getFirst().message())
        );

        env.destroyInstance(instance, true);
        env.destroyInstance(gameInstance, true);
    }

    @Test
    void testOmitsBuilderLineWhenMapHasNoBuilders(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        InstanceContainer gameInstance = (InstanceContainer) env.createFlatInstance();

        TestConnection connection = env.createConnection();
        connection.connect(instance);
        Collector<SystemChatPacket> packets = connection.trackIncoming(SystemChatPacket.class);

        GameMap gameMap = new GameMap(
                "Granskoga",
                Pos.ZERO,
                Pos.ZERO,
                Set.of(),
                Set.of(),
                List.of()
        );

        new GameMapLoadedListener().accept(new GameMapLoadedEvent(gameMap, gameInstance));

        List<SystemChatPacket> received = packets.collect();
        assertEquals(1, received.size(), "Player should have received exactly one map announcement message.");
        assertEquals(
                "\n──────────────────────\nNow playing: Granskoga\n──────────────────────\n",
                PlainTextComponentSerializer.plainText().serialize(received.getFirst().message())
        );

        env.destroyInstance(instance, true);
        env.destroyInstance(gameInstance, true);
    }
}
