package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.listener.common.ResourcePackListener;
import net.minestom.server.network.packet.client.common.ClientResourcePackStatusPacket;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the span {@code CygnusPlayer#kick} creates: which of the pack ack and the timeout ended the
 * wait, and the reason the player was kicked with.
 * <p>
 * Driven by the test {@code Env}: the timeout is ten explicit ticks, never a wait.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
@ExtendWith(MicrotusExtension.class)
class CygnusPlayerKickTracingTest {

    private static final Component KICK_MESSAGE = Component.text("bye ").append(Component.text("now"));

    private static void useTracedPlayers(Env env, @Nullable UUID packId, TestTelemetry telemetry) {
        KickTracer tracer = new KickTracer(telemetry.tracing());
        env.process().connection().setPlayerProvider(
                (connection, gameProfile) -> new CygnusPlayer(connection, gameProfile, packId, tracer));
    }

    @Test
    void testAckEndsTheKickSpanAndSaysSo(Env env) {
        try (TestTelemetry telemetry = new TestTelemetry()) {
            Instance instance = env.createFlatInstance();
            UUID packId = UUID.randomUUID();
            useTracedPlayers(env, packId, telemetry);
            Player player = env.createConnection().connect(instance);
            player.kick(KICK_MESSAGE);
            assertTrue(telemetry.spans().isEmpty(), "the span stays open while the client drops the pack");

            ResourcePackListener.listener(new ClientResourcePackStatusPacket(packId, ResourcePackStatus.DISCARDED), player);

            SpanData kick = telemetry.span(CygnusAttributes.SPAN_PLAYER_KICK);
            assertEquals(CygnusAttributes.KICK_BY_ACK, kick.getAttributes().get(CygnusAttributes.KICK_COMPLETED_BY));
            assertEquals("bye now", kick.getAttributes().get(CygnusAttributes.KICK_REASON),
                    "the reason is the plain text of the component");
            assertEquals(player.getUuid().toString(), kick.getAttributes().get(CygnusAttributes.PLAYER_UUID));
            env.destroyInstance(instance, true);
        }
    }

    @Test
    void testTimeoutEndsTheKickSpanAndSaysSo(Env env) {
        try (TestTelemetry telemetry = new TestTelemetry()) {
            Instance instance = env.createFlatInstance();
            useTracedPlayers(env, UUID.randomUUID(), telemetry);
            Player player = env.createConnection().connect(instance);
            player.kick(KICK_MESSAGE);

            for (int i = 0; i < 11; i++) env.tick();

            assertEquals(CygnusAttributes.KICK_BY_TIMEOUT, telemetry.span(CygnusAttributes.SPAN_PLAYER_KICK)
                    .getAttributes().get(CygnusAttributes.KICK_COMPLETED_BY));
            env.destroyInstance(instance, true);
        }
    }

    @Test
    void testTheTimeoutAfterAnAckDoesNotEndTheSpanTwice(Env env) {
        try (TestTelemetry telemetry = new TestTelemetry()) {
            Instance instance = env.createFlatInstance();
            UUID packId = UUID.randomUUID();
            useTracedPlayers(env, packId, telemetry);
            Player player = env.createConnection().connect(instance);
            player.kick(KICK_MESSAGE);
            ResourcePackListener.listener(new ClientResourcePackStatusPacket(packId, ResourcePackStatus.DISCARDED), player);

            for (int i = 0; i < 11; i++) env.tick();

            assertEquals(1, telemetry.spans().size(), "the ack decided, the timeout finds nothing pending");
            env.destroyInstance(instance, true);
        }
    }

    @Test
    void testAKickWithoutAPackIsImmediate(Env env) {
        try (TestTelemetry telemetry = new TestTelemetry()) {
            Instance instance = env.createFlatInstance();
            useTracedPlayers(env, null, telemetry);
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);

            player.kick(KICK_MESSAGE);

            assertEquals(CygnusAttributes.KICK_BY_IMMEDIATE, telemetry.span(CygnusAttributes.SPAN_PLAYER_KICK)
                    .getAttributes().get(CygnusAttributes.KICK_COMPLETED_BY));
            env.destroyInstance(instance, true);
        }
    }
}
