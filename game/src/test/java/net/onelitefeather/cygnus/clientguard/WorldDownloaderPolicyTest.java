package net.onelitefeather.cygnus.clientguard;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.ClientApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldDownloaderPolicyTest extends CygnusPlayerTestBase {

    /**
     * Section 0 with unknown functions off: int 0, false. Section 1 with general downloading off, save radius 0,
     * and chunk, entity, tile entity and container saving off: int 1, false, int 0, false, false, false, false.
     */
    private static final byte[] DENY_ALL = {
            0, 0, 0, 0, 0,
            0, 0, 0, 1, 0,
            0, 0, 0, 0,
            0, 0, 0, 0
    };

    /** The init packet of the WorldDownloader client, a JSON object. */
    private static final byte[] INIT = "{\"Version\":\"1.13\",\"State\":\"Init?\"}".getBytes(StandardCharsets.UTF_8);

    private Env installedIn;
    private WorldDownloaderPolicy installed;

    /** The event handler outlives a test, so a policy left on it would answer the next test too. */
    @AfterEach
    void uninstall() {
        if (this.installed != null) {
            this.installedIn.process().eventHandler().removeChild(this.installed.node());
        }
    }

    private WorldDownloaderPolicy install(Env env, ClientApiConfig config) {
        WorldDownloaderPolicy policy = new WorldDownloaderPolicy(config);
        this.installedIn = env;
        this.installed = policy;
        // Mirrors GameFeatures.register: a disabled feature never reaches the event tree.
        if (policy.enabled()) {
            env.process().eventHandler().addChild(policy.node());
        }
        return policy;
    }

    @Test
    @DisplayName("The deny-all payload has the byte layout the WorldDownloader client reads")
    void denyAllBytes() {
        assertArrayEquals(DENY_ALL, WorldDownloaderPolicy.denyAllPacket());
    }

    @Test
    @DisplayName("The init packet from a client is answered on wdl:control with the deny-all payload")
    void initIsAnsweredOnControl(Env env) {
        install(env, ClientApiConfig.DEFAULT);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        EventDispatcher.call(new PlayerPluginMessageEvent(player, WorldDownloaderPolicy.INIT_CHANNEL, INIT));

        List<PluginMessagePacket> sent = messages.collect();
        assertEquals(1, sent.size(), "one answer per init packet");
        assertEquals("wdl:control", sent.get(0).channel());
        assertArrayEquals(DENY_ALL, sent.get(0).data());
    }

    @Test
    @DisplayName("Spawning advertises the init channel so the client sends its init packet")
    void spawnAdvertisesInitChannel(Env env) {
        install(env, ClientApiConfig.DEFAULT);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        EventDispatcher.call(new PlayerSpawnEvent(player, player.getInstance(), true));

        List<PluginMessagePacket> sent = messages.collect();
        assertEquals(1, sent.size(), "one registration per spawn");
        assertEquals("minecraft:register", sent.get(0).channel());
        assertArrayEquals("wdl:init".getBytes(StandardCharsets.UTF_8), sent.get(0).data());
    }

    @Test
    @DisplayName("Other plugin channels are not answered")
    void otherChannelsAreIgnored(Env env) {
        install(env, ClientApiConfig.DEFAULT);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        EventDispatcher.call(new PlayerPluginMessageEvent(player, "minecraft:brand", INIT));

        assertTrue(messages.collect().isEmpty(), "only wdl:init is answered");
    }

    @Test
    @DisplayName("A policy with downloading not blocked is disabled and does not answer")
    void disabledPolicyIsSilent(Env env) {
        WorldDownloaderPolicy policy = install(env, new ClientApiConfig(false, List.of()));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        assertFalse(policy.enabled(), "the policy is off when downloading is not blocked");
        EventDispatcher.call(new PlayerPluginMessageEvent(player, WorldDownloaderPolicy.INIT_CHANNEL, INIT));

        assertTrue(messages.collect().isEmpty(), "no answer while the policy is not on the event tree");
    }
}
