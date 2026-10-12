package net.onelitefeather.cygnus.clientguard;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.ClientGuardConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client guard inside a running server: what a client sends decides whether it is kicked, and a disconnect
 * forgets it. The client's messages are fired as the packet listeners would fire them.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class ClientGuardTest extends CygnusPlayerTestBase {

    private static final ClientGuardConfig KICKING = new ClientGuardConfig(ClientGuardConfig.Mode.KICK,
            ClientGuardConfig.DEFAULT_BLOCKED_BRANDS, ClientGuardConfig.DEFAULT_BLOCKED_CHANNELS, List.of());
    private static final ClientGuardConfig LOGGING = new ClientGuardConfig(ClientGuardConfig.Mode.LOG,
            ClientGuardConfig.DEFAULT_BLOCKED_BRANDS, ClientGuardConfig.DEFAULT_BLOCKED_CHANNELS, List.of());

    private Env installedIn;
    private ClientGuard installed;

    /** The event handler outlives a test, so a guard left on it would see the next test's players. */
    @AfterEach
    void uninstall() {
        if (this.installed != null) {
            this.installedIn.process().eventHandler().removeChild(this.installed.node());
        }
    }

    private ClientGuard install(Env env, ClientGuardConfig config) {
        ClientGuard guard = new ClientGuard(config);
        env.process().eventHandler().addChild(guard.node());
        this.installedIn = env;
        this.installed = guard;
        return guard;
    }

    /** A VarInt length and the UTF-8 brand, the way the client writes it. */
    private static byte[] brandPayload(String brand) {
        byte[] bytes = brand.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bytes.length);
        out.writeBytes(bytes);
        return out.toByteArray();
    }

    /** The channel names, separated by zero bytes. */
    private static byte[] registerPayload(String... channels) {
        return String.join("\0", channels).getBytes(StandardCharsets.UTF_8);
    }

    private static void brand(Player player, String brand) {
        EventDispatcher.call(new PlayerPluginMessageEvent(player, ClientGuard.BRAND_CHANNEL, brandPayload(brand)));
    }

    private static void register(Player player, String... channels) {
        EventDispatcher.call(new PlayerPluginMessageEvent(player, ClientGuard.REGISTER_CHANNEL, registerPayload(channels)));
    }

    @Test
    @DisplayName("A client announcing a blocked brand is kicked in the kick mode")
    void blockedBrandIsKicked(Env env) {
        install(env, KICKING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        brand(player, "meteor");

        assertFalse(player.isOnline(), "a client with a blocked brand must be disconnected");
    }

    @Test
    @DisplayName("A vanilla client stays connected")
    void vanillaClientStaysConnected(Env env) {
        install(env, KICKING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        brand(player, "vanilla");
        register(player, "minecraft:brand", "minecraft:register");

        assertTrue(player.isOnline(), "a vanilla client must not be kicked");
    }

    @Test
    @DisplayName("A blocked channel registered on its own kicks the client")
    void blockedChannelIsKicked(Env env) {
        install(env, KICKING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        register(player, "minecraft:brand", "wdl:init");

        assertFalse(player.isOnline(), "a client registering a blocked channel must be disconnected");
    }

    @Test
    @DisplayName("A blocked client is only logged in the log mode")
    void logModeKeepsBlockedClient(Env env) {
        install(env, LOGGING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        brand(player, "meteor");

        assertTrue(player.isOnline(), "the log mode must not disconnect anybody");
    }

    @Test
    @DisplayName("A register message before the brand does not kick a whitelisted client")
    void registerBeforeBrandWithWhitelistDoesNotKick(Env env) {
        ClientGuardConfig whitelist = new ClientGuardConfig(ClientGuardConfig.Mode.KICK,
                ClientGuardConfig.DEFAULT_BLOCKED_BRANDS, ClientGuardConfig.DEFAULT_BLOCKED_CHANNELS, List.of("vanilla"));
        install(env, whitelist);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        register(player, "minecraft:brand", "minecraft:register");

        assertTrue(player.isOnline(), "a client that has not named its brand yet must not be kicked by the whitelist");

        brand(player, "vanilla");

        assertTrue(player.isOnline(), "a whitelisted client must stay once it names its brand");
    }

    @Test
    @DisplayName("Brand and channels from separate messages are combined in one profile")
    void profileCombinesMessages(Env env) {
        ClientGuard guard = install(env, LOGGING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        brand(player, "fabric");
        register(player, "minecraft:brand", "labymod:neo");

        ClientProfile profile = guard.profile(player.getUuid()).orElseThrow();
        assertEquals("fabric", profile.brand(), "the brand from the first message must be kept");
        assertTrue(profile.channels().contains("labymod:neo"), "the channel from the second message must be added");
    }

    @Test
    @DisplayName("Disconnecting forgets the profile of the player")
    void disconnectForgetsProfile(Env env) {
        ClientGuard guard = install(env, LOGGING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        brand(player, "vanilla");

        EventDispatcher.call(new PlayerDisconnectEvent(player));

        assertFalse(guard.profile(player.getUuid()).isPresent(), "a disconnected player must not keep a profile");
    }

    @Test
    @DisplayName("Messages on other channels do not create a profile")
    void otherChannelsAreIgnored(Env env) {
        ClientGuard guard = install(env, KICKING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        EventDispatcher.call(new PlayerPluginMessageEvent(player, "labymod:neo", new byte[]{0, 1}));

        assertFalse(guard.profile(player.getUuid()).isPresent(), "only the brand and register channels build a profile");
        assertTrue(player.isOnline(), "an unrelated message must not kick anybody");
    }

    @Test
    @DisplayName("A brand payload that does not decode is ignored and does not kick")
    void malformedBrandIsIgnored(Env env) {
        ClientGuard guard = install(env, KICKING);
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());

        // Claims five bytes of UTF-8 but carries only one.
        EventDispatcher.call(new PlayerPluginMessageEvent(player, ClientGuard.BRAND_CHANNEL, new byte[]{5, 'a'}));

        assertFalse(guard.profile(player.getUuid()).isPresent(), "a malformed brand must not create a profile");
        assertTrue(player.isOnline(), "a malformed brand must not kick anybody");
    }

    @Test
    @DisplayName("The off mode is not hooked into the event tree")
    void offModeIsDisabled() {
        assertFalse(new ClientGuard(new ClientGuardConfig(ClientGuardConfig.Mode.OFF, List.of(), List.of(), List.of()))
                .enabled(), "the off mode must not be attached");
    }
}
