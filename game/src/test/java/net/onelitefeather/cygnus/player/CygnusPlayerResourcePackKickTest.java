package net.onelitefeather.cygnus.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.minestom.server.entity.Player;
import net.minestom.server.listener.common.ResourcePackListener;
import net.minestom.server.network.packet.client.common.ClientResourcePackStatusPacket;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the ResourcePack pop {@link CygnusPlayer#kick(Component)} sends.
 *
 * <p>Behind a proxy a kick does not end the connection the pack hangs on - the player is moved to
 * another backend - so the pack outlives the service unless it is popped, and the pop only counts if
 * the client reads it before the disconnect. The disconnect therefore waits for the client to
 * confirm the pop, or for a short timeout.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.1
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
    void testKickPopsThePackAndDoesNotDisconnectYet(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        UUID packId = UUID.randomUUID();
        useResourcePackId(env, packId);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ServerPacket> packets = connection.trackIncoming();

        player.kick(KICK_MESSAGE);

        List<ServerPacket> sent = packets.collect();
        assertNotEquals(-1, indexOfPop(sent, packId), "the kick has to pop the pack this service pushed");
        assertEquals(-1, indexOfDisconnect(sent),
                "the disconnect has to wait until the client confirmed the pop, otherwise the proxy may move it first");
        assertTrue(player.isOnline(), "the player stays connected while the client drops the pack");

        env.destroyInstance(instance, true);
    }

    @Test
    void testDiscardedAckDisconnectsWithTheOriginalMessage(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        UUID packId = UUID.randomUUID();
        useResourcePackId(env, packId);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.kick(KICK_MESSAGE);
        Collector<DisconnectPacket> disconnects = connection.trackIncoming(DisconnectPacket.class);

        ResourcePackListener.listener(new ClientResourcePackStatusPacket(packId, ResourcePackStatus.DISCARDED), player);

        List<DisconnectPacket> sent = disconnects.collect();
        assertEquals(1, sent.size(), "the ack has to disconnect the player exactly once");
        assertEquals(KICK_MESSAGE, sent.getFirst().message(), "the original kick message has to be kept");
        assertFalse(player.isOnline());

        env.destroyInstance(instance, true);
    }

    @Test
    void testAckOfAnotherPackDoesNotDisconnect(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        useResourcePackId(env, UUID.randomUUID());
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.kick(KICK_MESSAGE);

        ResourcePackListener.listener(
                new ClientResourcePackStatusPacket(UUID.randomUUID(), ResourcePackStatus.DISCARDED), player);

        assertTrue(player.isOnline(), "only the pop of our own pack is the ack the kick waits for");

        env.destroyInstance(instance, true);
    }

    @Test
    void testTimeoutDisconnectsAfterTenTicksAndNotBefore(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        useResourcePackId(env, UUID.randomUUID());
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.kick(KICK_MESSAGE);

        for (int i = 0; i < 9; i++) env.tick();
        assertTrue(player.isOnline(), "the kick must wait for the ack within the timeout");

        Collector<DisconnectPacket> disconnects = connection.trackIncoming(DisconnectPacket.class);
        for (int i = 0; i < 2; i++) env.tick();

        assertEquals(1, disconnects.collect().size(), "a client that never answers is disconnected after the timeout");
        assertFalse(player.isOnline());

        env.destroyInstance(instance, true);
    }

    @Test
    void testAckAndTimeoutDisconnectOnlyOnce(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        UUID packId = UUID.randomUUID();
        useResourcePackId(env, packId);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.kick(KICK_MESSAGE);
        player.kick(KICK_MESSAGE);
        Collector<DisconnectPacket> disconnects = connection.trackIncoming(DisconnectPacket.class);

        ResourcePackListener.listener(new ClientResourcePackStatusPacket(packId, ResourcePackStatus.DISCARDED), player);
        for (int i = 0; i < 15; i++) env.tick();

        assertEquals(1, disconnects.collect().size(), "repeated kicks, the ack and the timeout must not disconnect twice");

        env.destroyInstance(instance, true);
    }

    @Test
    void testKickWithoutPackIdStaysImmediate(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        useResourcePackId(env, null);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<DisconnectPacket> disconnects = connection.trackIncoming(DisconnectPacket.class);

        player.kick(KICK_MESSAGE);

        assertEquals(1, disconnects.collect().size(), "without a pack there is nothing to wait for");
        assertFalse(player.isOnline());

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
