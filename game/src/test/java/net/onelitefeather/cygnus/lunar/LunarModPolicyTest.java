package net.onelitefeather.cygnus.lunar;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.lunarclient.apollo.configurable.v1.ConfigurableSettings;
import com.lunarclient.apollo.configurable.v1.OverrideConfigurableSettingsMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.LunarConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LunarModPolicyTest extends CygnusPlayerTestBase {

    private static final String OVERRIDE_TYPE_URL =
            "type.googleapis.com/lunarclient.apollo.configurable.v1.OverrideConfigurableSettingsMessage";

    private Env installedIn;
    private LunarModPolicy installed;

    /** The event handler outlives a test, so a policy left on it would answer the next test too. */
    @AfterEach
    void uninstall() {
        if (this.installed != null) {
            this.installedIn.process().eventHandler().removeChild(this.installed.node());
        }
    }

    private LunarModPolicy install(Env env, LunarConfig config) {
        LunarModPolicy policy = new LunarModPolicy(config);
        env.process().eventHandler().addChild(policy.node());
        this.installedIn = env;
        this.installed = policy;
        return policy;
    }

    /** The client's registration of its channels: NUL separated names, as Minecraft sends them. */
    private static void clientRegisters(Player player, String channels) {
        EventDispatcher.call(new PlayerPluginMessageEvent(player, LunarModPolicy.REGISTER_CHANNEL,
                channels.getBytes(StandardCharsets.UTF_8)));
    }

    private static OverrideConfigurableSettingsMessage decode(byte[] payload) throws InvalidProtocolBufferException {
        return Any.parseFrom(payload).unpack(OverrideConfigurableSettingsMessage.class);
    }

    private static List<PluginMessagePacket> onChannel(List<PluginMessagePacket> packets, String channel) {
        return packets.stream().filter(packet -> channel.equals(packet.channel())).toList();
    }

    @Test
    @DisplayName("The override payload is an Any wrapping the Apollo override message")
    void overrideIsWrappedInAny() throws InvalidProtocolBufferException {
        Any any = Any.parseFrom(LunarModPolicy.overridePacket(List.of("minimap")));

        assertEquals(OVERRIDE_TYPE_URL, any.getTypeUrl(), "the Apollo type URL");
    }

    @Test
    @DisplayName("The override targets the mod_setting module and turns it on")
    void overrideTargetsModSettingModule() throws InvalidProtocolBufferException {
        ConfigurableSettings settings = decode(LunarModPolicy.overridePacket(List.of("minimap"))).getConfigurableSettings(0);

        assertEquals("mod_setting", settings.getApolloModule(), "the module that carries mod settings");
        assertTrue(settings.getEnable(), "the module must be enabled");
    }

    @Test
    @DisplayName("Each listed mod gets its enabled option set to false")
    void overrideDisablesEachListedMod() throws InvalidProtocolBufferException {
        ConfigurableSettings settings = decode(LunarModPolicy.overridePacket(List.of("minimap", "zoom"))).getConfigurableSettings(0);

        assertEquals(Map.of(
                "minimap.enabled", Value.newBuilder().setBoolValue(false).build(),
                "zoom.enabled", Value.newBuilder().setBoolValue(false).build()), settings.getPropertiesMap());
    }

    @Test
    @DisplayName("The enabled key of a mod is its name followed by .enabled")
    void enabledKeyFollowsApolloNaming() {
        assertEquals("directionhud.enabled", LunarModPolicy.enabledKey("directionhud"));
    }

    @Test
    @DisplayName("A registration that lists the Apollo channel among others is recognised")
    void registrationWithOtherChannelsIsRecognised() {
        assertTrue(LunarModPolicy.registersApollo("minecraft:brand\0lunar:apollo".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("A registration without the Apollo channel is not recognised")
    void registrationWithoutApolloIsNotRecognised() {
        assertFalse(LunarModPolicy.registersApollo("minecraft:brand\0labymod:neo".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("The announcement carries the Apollo channel name")
    void announcementNamesApolloChannel() {
        assertArrayEquals("lunar:apollo".getBytes(StandardCharsets.UTF_8), LunarModPolicy.registerPayload());
    }

    @Test
    @DisplayName("The first spawn announces lunar:apollo on minecraft:register")
    void firstSpawnAnnouncesApollo(Env env) {
        install(env, LunarConfig.DEFAULT);
        TestConnection connection = env.createConnection();
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        connection.connect(env.createFlatInstance());

        List<PluginMessagePacket> announcements = onChannel(messages.collect(), LunarModPolicy.REGISTER_CHANNEL);
        assertEquals(1, announcements.size(), "one announcement on the first spawn");
        assertArrayEquals(LunarModPolicy.registerPayload(), announcements.get(0).data());
    }

    @Test
    @DisplayName("A client registering lunar:apollo gets one override for the configured mods")
    void registrationGetsOneOverride(Env env) {
        install(env, LunarConfig.DEFAULT);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientRegisters(player, "lunar:apollo");

        List<PluginMessagePacket> overrides = onChannel(messages.collect(), LunarModPolicy.CHANNEL);
        assertEquals(1, overrides.size(), "exactly one override packet on lunar:apollo");
    }

    @Test
    @DisplayName("The override a registered client gets disables the configured mods")
    void registrationGetsDisablingOverride(Env env) throws InvalidProtocolBufferException {
        install(env, new LunarConfig(true, List.of("minimap", "waypoints")));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientRegisters(player, "lunar:apollo");

        byte[] data = onChannel(messages.collect(), LunarModPolicy.CHANNEL).get(0).data();
        assertEquals(Map.of(
                "minimap.enabled", Value.newBuilder().setBoolValue(false).build(),
                "waypoints.enabled", Value.newBuilder().setBoolValue(false).build()),
                decode(data).getConfigurableSettings(0).getPropertiesMap());
    }

    @Test
    @DisplayName("A registration of other channels gets no override")
    void otherChannelsGetNoOverride(Env env) {
        install(env, LunarConfig.DEFAULT);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientRegisters(player, "minecraft:brand\0labymod:neo");

        assertTrue(onChannel(messages.collect(), LunarModPolicy.CHANNEL).isEmpty(), "no Apollo packet for other channels");
    }

    @Test
    @DisplayName("A disabled request sends no Apollo packet and does not hook into the server")
    void disabledRequestSendsNothing(Env env) {
        LunarModPolicy policy = install(env, new LunarConfig(false, LunarConfig.KNOWN_MODS));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientRegisters(player, "lunar:apollo");
        policy.apply(player);

        assertTrue(onChannel(messages.collect(), LunarModPolicy.CHANNEL).isEmpty(), "disabled must not send an override");
        assertFalse(policy.enabled(), "disabled must not be enabled");
    }

    @Test
    @DisplayName("An empty mod list has nothing to switch off and sends no Apollo packet")
    void emptyListSendsNothing(Env env) {
        LunarModPolicy policy = install(env, new LunarConfig(true, List.of()));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<PluginMessagePacket> messages = connection.trackIncoming(PluginMessagePacket.class);

        clientRegisters(player, "lunar:apollo");

        assertTrue(onChannel(messages.collect(), LunarModPolicy.CHANNEL).isEmpty(), "no mods, no override");
        assertFalse(policy.enabled(), "an empty list must not be enabled");
    }
}
