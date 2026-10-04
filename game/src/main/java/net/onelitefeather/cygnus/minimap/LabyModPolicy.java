package net.onelitefeather.cygnus.minimap;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.onelitefeather.cygnus.GameFeature;
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

    /** Lets a player put a marker on an entity. */
    public static final String ENTITY_MARKER = "entity_marker";

    private final EventNode<Event> node = EventNode.all("labymod");
    private final MinimapConfig config;

    /**
     * Creates the policy.
     *
     * @param config the mode, shared with the minimap policy
     */
    public LabyModPolicy(MinimapConfig config) {
        this.config = config;
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
     * Sends the permissions of the configured mode to a player. Does nothing for
     * {@link MinimapConfig.Mode#OFF}.
     *
     * @param player the LabyMod player
     */
    public void apply(Player player) {
        List<String> denied = denied(this.config.mode());
        if (denied.isEmpty()) {
            return;
        }
        player.sendPluginMessage(CHANNEL, permissionPacket(denied));
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
        return this.config.mode() != MinimapConfig.Mode.OFF;
    }
}
