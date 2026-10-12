package net.onelitefeather.cygnus.lunar;

import com.google.protobuf.Any;
import com.google.protobuf.Value;
import com.lunarclient.apollo.configurable.v1.ConfigurableSettings;
import com.lunarclient.apollo.configurable.v1.OverrideConfigurableSettingsMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.LunarConfig;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Asks Lunar Client to switch off the mods that could give a survivor an edge, through Lunar's Apollo API.
 * <p>
 * This is a client courtesy API, not a cheat-proof measure. Lunar Client applies the override it is sent, but a
 * modified client, another client or a player who turns the setting back on is not stopped by it.
 * </p>
 * <p>
 * The exchange has two steps. On its first spawn the server announces the channel {@value #CHANNEL} with a
 * {@value #REGISTER_CHANNEL} packet, which is what Lunar needs to talk Apollo at all. When the client answers
 * by registering {@value #CHANNEL} itself, the policy sends one {@code OverrideConfigurableSettingsMessage}
 * (wrapped in {@code google.protobuf.Any}) for the {@value #MOD_SETTING_MODULE} module. It sets the
 * {@code enabled} option of each configured mod to {@code false}, see {@link #enabledKey(String)}. The
 * override is sent once per registration and not again on a later instance switch.
 * </p>
 * <p>
 * The messages come from {@code com.lunarclient:apollo-protos}. The {@code apollo-minestom} platform artifact is
 * not used: it bundles its own unrelocated Adventure, which would clash with the one Cygnus runs on.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class LunarModPolicy implements GameFeature {

    /** The Apollo plugin channel. */
    public static final String CHANNEL = "lunar:apollo";

    /** The channel announcement a server sends so that the client knows which channels it may use. */
    public static final String REGISTER_CHANNEL = "minecraft:register";

    /** The Apollo module that carries the mod setting overrides. */
    public static final String MOD_SETTING_MODULE = "mod_setting";

    private final EventNode<Event> node = EventNode.all("lunar");
    private final LunarConfig config;

    /**
     * Creates the policy and hooks it onto spawns and on plugin messages.
     *
     * @param config the mods to switch off and whether to send anything at all
     */
    public LunarModPolicy(LunarConfig config) {
        this.config = config;
        this.node.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                announce(event.getPlayer());
            }
        });
        this.node.addListener(PlayerPluginMessageEvent.class, event -> {
            if (REGISTER_CHANNEL.equals(event.getIdentifier()) && registersApollo(event.getMessage())) {
                this.apply(event.getPlayer());
            }
        });
    }

    /**
     * Returns the option key of a mod's {@code enabled} switch, as the Apollo mod documentation names it:
     * {@code <mod>.enabled}, for example {@code minimap.enabled}. Lunar's own example uses {@code lighting.enabled}
     * the same way.
     *
     * @param mod the lower-case mod name, one of {@link LunarConfig#KNOWN_MODS}
     * @return the option key
     */
    public static String enabledKey(String mod) {
        return mod + ".enabled";
    }

    /**
     * Returns the payload of the {@value #REGISTER_CHANNEL} announcement for the Apollo channel. The channels of
     * that packet are separated by NUL bytes; there is only one here.
     *
     * @return the payload
     */
    public static byte[] registerPayload() {
        return CHANNEL.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Checks whether a client's {@value #REGISTER_CHANNEL} payload registers the Apollo channel.
     *
     * @param message the payload the client sent
     * @return {@code true} if one of the NUL separated channels is {@value #CHANNEL}
     */
    public static boolean registersApollo(byte[] message) {
        String channels = new String(message, StandardCharsets.UTF_8);
        for (String channel : channels.split("\u0000")) {
            if (CHANNEL.equals(channel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encodes the Apollo payload that switches off the given mods: an {@code OverrideConfigurableSettingsMessage}
     * with one {@value #MOD_SETTING_MODULE} settings entry, packed into {@code google.protobuf.Any}.
     *
     * @param disabledMods the mods to switch off, each one of {@link LunarConfig#KNOWN_MODS}
     * @return the payload to send on {@value #CHANNEL}
     */
    public static byte[] overridePacket(Collection<String> disabledMods) {
        Map<String, Value> properties = new LinkedHashMap<>();
        for (String mod : disabledMods) {
            properties.put(enabledKey(mod), Value.newBuilder().setBoolValue(false).build());
        }
        ConfigurableSettings settings = ConfigurableSettings.newBuilder()
                .setApolloModule(MOD_SETTING_MODULE)
                .setEnable(true)
                .putAllProperties(properties)
                .build();
        OverrideConfigurableSettingsMessage message = OverrideConfigurableSettingsMessage.newBuilder()
                .addConfigurableSettings(settings)
                .build();
        return Any.pack(message).toByteArray();
    }

    /**
     * Sends the {@value #REGISTER_CHANNEL} announcement, so that the client knows the Apollo channel.
     *
     * @param player the player who spawned
     */
    public void announce(Player player) {
        player.sendPluginMessage(REGISTER_CHANNEL, registerPayload());
    }

    /**
     * Sends the override for the configured mods. Does nothing when the policy is off or the list is empty.
     *
     * @param player the player whose client registered the Apollo channel
     */
    public void apply(Player player) {
        if (!this.enabled()) {
            return;
        }
        player.sendPluginMessage(CHANNEL, overridePacket(this.config.disabledMods()));
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    @Override
    public boolean enabled() {
        return this.config.enabled() && !this.config.disabledMods().isEmpty();
    }
}
