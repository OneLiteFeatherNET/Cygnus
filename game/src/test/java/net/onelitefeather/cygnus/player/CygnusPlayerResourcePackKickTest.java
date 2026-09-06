package net.onelitefeather.cygnus.player;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.DisconnectPacket;
import net.minestom.server.network.packet.server.common.ResourcePackPopPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the ResourcePack pop {@link CygnusPlayer#kick(Component)} sends.
 *
 * <p>Behind a proxy a kick does not end the connection the pack hangs on - the player is moved to
 * another backend - so the pack outlives the service unless it is popped, and the pop only counts if
 * the client reads it before the disconnect.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.11.0
 */
@ExtendWith(MicrotusExtension.class)
class CygnusPlayerResourcePackKickTest {

    private static final Component KICK_MESSAGE = Component.text("bye");

    /**
     * Installs a player provider handing out players with the given pack id, so a connection made
     * afterwards produces the player under test.
     *
     * @param env            the test environment
     * @param resourcePackId the pack id the players should carry, or {@code null} for none
     */
    private static void useResourcePackId(@NotNull Env env, @Nullable UUID resourcePackId) {
        env.process().connection().setPlayerProvider(
                (connection, gameProfile) -> new CygnusPlayer(connection, gameProfile, resourcePackId));
    }

    private static int indexOfPop(@NotNull List<ServerPacket> packets, @NotNull UUID packId) {
        for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i) instanceof ResourcePackPopPacket pop && packId.equals(pop.id())) return i;
        }
        return -1;
    }

    private static int indexOfDisconnect(@NotNull List<ServerPacket> packets) {
        for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i) instanceof DisconnectPacket) return i;
        }
        return -1;
    }

    @Test
    void testKickPopsThePackBeforeTheDisconnect(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        UUID packId = UUID.randomUUID();
        useResourcePackId(env, packId);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ServerPacket> packets = connection.trackIncoming();

        player.kick(KICK_MESSAGE);

        List<ServerPacket> sent = packets.collect();
        int pop = indexOfPop(sent, packId);
        int disconnect = indexOfDisconnect(sent);

        assertNotEquals(-1, pop, "the kick has to pop the pack this service pushed");
        assertNotEquals(-1, disconnect, "the kick still has to disconnect the player");
        assertTrue(pop < disconnect,
                "the pop has to reach the client before the disconnect, otherwise the client is gone before it reads it");

        env.destroyInstance(instance, true);
    }

    @Test
    void testKickPopsNothingWithoutAResourcePack(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        useResourcePackId(env, null);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);

        player.kick(KICK_MESSAGE);

        assertEquals(List.of(), pops.collect(), "a service without a ResourcePack must not pop one");

        env.destroyInstance(instance, true);
    }
}
