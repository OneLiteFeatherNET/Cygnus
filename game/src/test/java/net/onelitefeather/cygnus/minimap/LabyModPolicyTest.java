package net.onelitefeather.cygnus.minimap;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.MinimapConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabyModPolicyTest extends CygnusPlayerTestBase {

    /** VarInt 10, VarInt 1, VarInt 13, "entity_marker", false. */
    private static final byte[] DENY_ENTITY_MARKER = concat(
            new byte[]{10, 1, 13}, "entity_marker".getBytes(StandardCharsets.UTF_8), new byte[]{0});

    private Env installedIn;
    private LabyModPolicy installed;

    /** The event handler outlives a test, so a policy left on it would answer the next test too. */
    @AfterEach
    void uninstall() {
        if (this.installed != null) {
            this.installedIn.process().eventHandler().removeChild(this.installed.node());
        }
    }

    private LabyModPolicy install(Env env, MinimapConfig.Mode mode) {
        LabyModPolicy policy = new LabyModPolicy(new MinimapConfig(mode));
        env.process().eventHandler().addChild(policy.node());
        this.installedIn = env;
        this.installed = policy;
        return policy;
    }

    private static byte[] concat(byte[] first, byte[]... more) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.writeBytes(first);
        for (byte[] part : more) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    /** The client's hello: VarInt id 0, then its version string. */
    private static void clientHello(Player player, String channel) {
        byte[] version = "4.2.0".getBytes(StandardCharsets.UTF_8);
        byte[] payload = concat(new byte[]{0, (byte) version.length}, version);
        EventDispatcher.call(new PlayerPluginMessageEvent(player, channel, payload));
    }

    @Test
    @DisplayName("The permission packet denying the entity marker has the documented bytes")
    void packetBytes() {
        assertArrayEquals(DENY_ENTITY_MARKER, LabyModPolicy.permissionPacket(List.of("entity_marker")));
    }

    @Test
    @DisplayName("Disabled mode answers the LabyMod hello with the permission packet")
    void disabledAnswersHello(Env env) {
        install(env, MinimapConfig.Mode.DISABLED);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientHello(player, "labymod:neo");

        List<PluginMessagePacket> sent = messages.collect();
        assertEquals(1, sent.size(), "one permission packet");
        assertEquals("labymod:neo", sent.get(0).channel());
        assertArrayEquals(DENY_ENTITY_MARKER, sent.get(0).data());
    }

    @Test
    @DisplayName("Fair mode sends the same permissions")
    void fairAnswersHello(Env env) {
        install(env, MinimapConfig.Mode.FAIR);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientHello(player, "labymod:neo");

        assertArrayEquals(DENY_ENTITY_MARKER, messages.collect().get(0).data());
    }

    @Test
    @DisplayName("Off mode sends nothing and is disabled")
    void offSendsNothing(Env env) {
        LabyModPolicy policy = install(env, MinimapConfig.Mode.OFF);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientHello(player, "labymod:neo");
        policy.apply(player);

        assertTrue(messages.collect().isEmpty(), "off must not send any plugin message");
        assertFalse(policy.enabled(), "off must not hook into the event tree");
    }

    @Test
    @DisplayName("Other channels and other LabyMod packets do not trigger an answer")
    void ignoresOtherMessages(Env env) {
        install(env, MinimapConfig.Mode.DISABLED);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientHello(player, "minecraft:brand");
        EventDispatcher.call(new PlayerPluginMessageEvent(player, "labymod:neo", new byte[]{35, 0}));

        assertTrue(messages.collect().isEmpty(), "only the hello is answered");
    }
}
