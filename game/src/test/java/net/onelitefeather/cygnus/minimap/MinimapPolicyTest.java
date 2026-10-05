package net.onelitefeather.cygnus.minimap;

import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.MinimapConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimapPolicyTest extends CygnusPlayerTestBase {

    private Env installedIn;
    private MinimapPolicy installed;

    /** The event handler outlives a test, so a policy left on it would answer the next test's joins too. */
    @AfterEach
    void uninstall() {
        if (this.installed != null) {
            this.installedIn.process().eventHandler().removeChild(this.installed.node());
        }
    }

    private MinimapPolicy install(Env env, MinimapConfig.Mode mode) {
        MinimapPolicy policy = new MinimapPolicy(new MinimapConfig(mode));
        env.process().eventHandler().addChild(policy.node());
        this.installedIn = env;
        this.installed = policy;
        return policy;
    }

    private static List<String> raw(Collector<SystemChatPacket> collector) {
        return collector.collect().stream()
                .map(packet -> ((TextComponent) packet.message()).content())
                .toList();
    }

    @Test
    @DisplayName("Disabled mode sends the Xaero and VoxelMap lines on join")
    void disabledSendsAllCodes(Env env) {
        install(env, MinimapConfig.Mode.DISABLED);
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        connection.connect(instance);

        assertEquals(List.of("§n§o§m§i§n§i§m§a§p", "§f§a§i§r§x§a§e§r§o",
                "§3 §6 §3 §6 §3 §6 §e", "§3 §6 §3 §6 §3 §6 §d"), raw(chat));
    }

    @Test
    @DisplayName("Fair mode keeps the map and only asks for radar and cave mode off")
    void fairSendsNoMinimapOffCode(Env env) {
        install(env, MinimapConfig.Mode.FAIR);
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        connection.connect(instance);

        List<String> lines = raw(chat);
        assertEquals(List.of("§f§a§i§r§x§a§e§r§o", "§3 §6 §3 §6 §3 §6 §e", "§3 §6 §3 §6 §3 §6 §d"), lines);
        assertFalse(lines.contains(MinimapPolicy.XAERO_NO_MINIMAP), "fair play must not switch the minimap off");
    }

    @Test
    @DisplayName("Off mode sends nothing and the feature is disabled")
    void offSendsNothing(Env env) {
        MinimapPolicy policy = install(env, MinimapConfig.Mode.OFF);
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        connection.connect(instance);

        assertTrue(raw(chat).isEmpty(), "off must not send any message");
        assertFalse(policy.enabled(), "off must not hook into the event tree");
    }

    @Test
    @DisplayName("Spawning into another instance asks the mods again")
    void worldChangeResends(Env env) {
        install(env, MinimapConfig.Mode.FAIR);
        Instance lobby = env.createFlatInstance();
        Instance map = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(lobby);
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        player.setInstance(map).join();

        assertEquals(MinimapPolicy.codes(MinimapConfig.Mode.FAIR), raw(chat));
    }

    @Test
    @DisplayName("The lines are literal text with the section signs intact and nothing visible")
    void linesAreRawLiteralText(Env env) {
        install(env, MinimapConfig.Mode.DISABLED);
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        connection.connect(instance);

        List<SystemChatPacket> packets = chat.collect();
        assertEquals(4, packets.size(), "one message per code");
        for (SystemChatPacket packet : packets) {
            TextComponent text = (TextComponent) packet.message();
            assertTrue(text.content().startsWith("§"), "the section sign must survive: " + text.content());
            assertTrue(text.children().isEmpty(), "no children that could show something");
            assertEquals("", text.content().replaceAll("§.| ", ""), "only colour codes, nothing visible");
            assertFalse(packet.overlay(), "a system line, not the action bar");
        }
    }
}
