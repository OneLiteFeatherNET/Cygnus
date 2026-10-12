package net.onelitefeather.cygnus.minimap;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.ClientApiConfig;
import net.onelitefeather.cygnus.common.config.MinimapConfig;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Asks LabyMod to switch off the features that could give a survivor an edge.
 * <p>
 * LabyMod 4 talks to servers on the plugin channel {@value #CHANNEL}. A packet is a VarInt id followed by
 * its fields. The client opens the conversation with a {@code VersionLoginPacket} (id {@value #LOGIN_ID}),
 * and only then the policy answers with a {@code PermissionPacket} (id {@value #PERMISSION_ID}): a VarInt
 * count, then per permission its identifier as a VarInt-prefixed UTF-8 string and an allowed flag as one
 * byte. The few bytes are written by hand instead of pulling in the Server API.
 * </p>
 * <p>
 * LabyMod 4 has no minimap or radar of its own, so there is no permission to take away for it. What the
 * client does register and what could leak the position of others is {@value #ENTITY_MARKER}, which lets
 * a player put a marker on an entity. It is off by default; denying it makes that explicit. Like the
 * minimap lines this is a request that a modified client can ignore.
 * </p>
 * <p>
 * The same hello also gets an {@code AddonDisablePacket} (id {@value #ADDON_DISABLE_ID}, client bound) for every
 * configured addon namespace: a boolean action, {@code true} for disable, then a VarInt count and each namespace
 * as a VarInt-prefixed UTF-8 string. The layout comes from {@code AddonDisablePacket}, and the id from
 * {@code registerPacket(24, AddonDisablePacket.class, Direction.CLIENTBOUND)} in {@code LabyModProtocol}, both in
 * {@code core/src/main/java/net/labymod/serverapi/core} of github.com/LabyMod/labymod4-server-api. The LabyMod documentation
 * (dev.labymod.net, moderation/disable-addons) only says that the packet forcefully disables addons, and that
 * it works for addons the user can also disable. Like the permissions this is a request a modified client can
 * ignore.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class LabyModPolicy implements GameFeature {

    /** The channel LabyMod 4 uses. */
    public static final String CHANNEL = "labymod:neo";

    /** The id of the packet the client opens the conversation with. */
    public static final int LOGIN_ID = 0;

    /** The id of the packet that allows and denies features. */
    public static final int PERMISSION_ID = 10;

    /** The id of the client bound packet that force disables addons. */
    public static final int ADDON_DISABLE_ID = 24;

    /** Lets a player put a marker on an entity. */
    public static final String ENTITY_MARKER = "entity_marker";

    private final EventNode<Event> node = EventNode.all("labymod");
    private final MinimapConfig config;
    private final ClientApiConfig clientApi;

    /**
     * Creates the policy.
     *
     * @param config    the mode, shared with the minimap policy
     * @param clientApi the addon namespaces to disable, see {@link ClientApiConfig#labyModDisabledAddons()}
     */
    public LabyModPolicy(MinimapConfig config, ClientApiConfig clientApi) {
        this.config = config;
        this.clientApi = clientApi;
        this.node.addListener(PlayerPluginMessageEvent.class, event -> {
            if (CHANNEL.equals(event.getIdentifier()) && isLogin(event.getMessage())) {
                this.apply(event.getPlayer());
            }
        });
    }

    /**
     * Returns the permissions a mode denies.
     *
     * @param mode the mode
     * @return the identifiers, empty for {@link MinimapConfig.Mode#OFF}
     */
    public static List<String> denied(MinimapConfig.Mode mode) {
        return mode == MinimapConfig.Mode.OFF ? List.of() : List.of(ENTITY_MARKER);
    }

    /**
     * Encodes a {@code PermissionPacket} that denies the given permissions.
     *
     * @param denied the identifiers to deny
     * @return the payload to send on {@value #CHANNEL}
     */
    public static byte[] permissionPacket(List<String> denied) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarInt(out, PERMISSION_ID);
        writeVarInt(out, denied.size());
        for (String identifier : denied) {
            byte[] bytes = identifier.getBytes(StandardCharsets.UTF_8);
            writeVarInt(out, bytes.length);
            out.writeBytes(bytes);
            out.write(0);
        }
        return out.toByteArray();
    }

    /**
     * Encodes an {@code AddonDisablePacket} that force disables the given addon namespaces.
     *
     * @param namespaces the addon namespaces to disable
     * @return the payload to send on {@value #CHANNEL}
     */
    public static byte[] addonDisablePacket(List<String> namespaces) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarInt(out, ADDON_DISABLE_ID);
        out.write(1);
        writeVarInt(out, namespaces.size());
        for (String namespace : namespaces) {
            byte[] bytes = namespace.getBytes(StandardCharsets.UTF_8);
            writeVarInt(out, bytes.length);
            out.writeBytes(bytes);
        }
        return out.toByteArray();
    }

    /**
     * Sends the permissions of the configured mode and the disabled addons to a player. The permissions are
     * skipped for {@link MinimapConfig.Mode#OFF}, the addon packet when no addon is configured.
     *
     * @param player the LabyMod player
     */
    public void apply(Player player) {
        List<String> denied = denied(this.config.mode());
        if (!denied.isEmpty()) {
            player.sendPluginMessage(CHANNEL, permissionPacket(denied));
        }
        if (!this.clientApi.labyModDisabledAddons().isEmpty()) {
            player.sendPluginMessage(CHANNEL, addonDisablePacket(this.clientApi.labyModDisabledAddons()));
        }
    }

    private static boolean isLogin(byte[] message) {
        // The id is a VarInt; 0 is a single zero byte.
        return message.length > 0 && message[0] == LOGIN_ID;
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        int rest = value;
        while ((rest & -128) != 0) {
            out.write(rest & 127 | 128);
            rest >>>= 7;
        }
        out.write(rest);
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    @Override
    public boolean enabled() {
        return this.config.mode() != MinimapConfig.Mode.OFF || !this.clientApi.labyModDisabledAddons().isEmpty();
    }
}
