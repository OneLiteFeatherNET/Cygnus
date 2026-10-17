package net.onelitefeather.cygnus.listener;

import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.ping.ServerListPingType;
import net.minestom.server.ping.Status;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MicrotusExtension.class)
class ServerListPingListenerTest {

    @Test
    void testReportsConfiguredMaxPlayers(Env env) {
        ServerListPingEvent event = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);

        new ServerListPingListener(8).accept(event);

        Status.PlayerInfo info = event.getStatus().playerInfo();
        assertNotNull(info, "The ping response must carry player info");
        assertEquals(8, info.maxPlayers(), "The ping must report the configured max players");
    }

    @Test
    void testReportsRealOnlineCount(Env env) {
        Instance instance = env.createFlatInstance();
        env.createPlayer(instance);
        env.createPlayer(instance);
        ServerListPingEvent event = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);

        new ServerListPingListener(8).accept(event);

        assertEquals(2, event.getStatus().playerInfo().onlinePlayers(), "The online count must stay the real count");
    }

    @Test
    void testKeepsDefaultDescription(Env env) {
        ServerListPingEvent event = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);
        Status before = event.getStatus();

        new ServerListPingListener(8).accept(event);

        assertEquals(before.description(), event.getStatus().description(), "Only max players may change");
        assertEquals(before.versionInfo(), event.getStatus().versionInfo(), "Only max players may change");
    }
}
