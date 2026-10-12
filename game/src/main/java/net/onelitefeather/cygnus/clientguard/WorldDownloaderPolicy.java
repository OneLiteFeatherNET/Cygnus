package net.onelitefeather.cygnus.clientguard;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.ClientApiConfig;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Asks the WorldDownloader mod to refuse all downloading of the world.
 * <p>
 * The protocol follows the WorldDownloader client, class {@code wdl.WDLPluginChannels} in the
 * {@code share_14} source tree of the Pokechu22/WorldDownloader repository, and the wiki page
 * https://wiki.vg/User:Pokechu22/World_downloader. The flow is:
 * </p>
 * <ol>
 * <li>On spawn the server advertises {@value #INIT_CHANNEL} with {@code minecraft:register}. The client only
 * sends its init packet on a channel the server registered for it ({@code isRegistered} in
 * {@code sendInitPacket}).</li>
 * <li>The client sends its init packet, a JSON object, on {@value #INIT_CHANNEL}.</li>
 * <li>The server answers on {@value #CONTROL_CHANNEL} with the permission sections. All integers are
 * big-endian, booleans one byte, as in {@code ByteStreams.newDataInput} on the client side
 * ({@code handleControlPacket}).</li>
 * </ol>
 * <p>
 * Section 0 ({@code canUseFunctionsUnknownToServer}) is set to {@code false}, so functions the server did
 * not name stay off. Section 1 sets general downloading to {@code false}, with cached chunks, entities, tile
 * entities and containers off as well, and a save radius of {@code 0}.
 * </p>
 * <p>
 * This is a courtesy API. WorldDownloader honours it because its author wrote the permission check in, a
 * modified client can ignore it. It is not an anti-cheat.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class WorldDownloaderPolicy implements GameFeature {

    /** The channel the client sends its init packet on, and that the server advertises. */
    public static final String INIT_CHANNEL = "wdl:init";

    /** The channel the server answers on with the permission sections. */
    public static final String CONTROL_CHANNEL = "wdl:control";

    /** The vanilla channel the server advertises its channels with. */
    static final String REGISTER_CHANNEL = "minecraft:register";

    private final EventNode<Event> node = EventNode.all("world-downloader");
    private final ClientApiConfig config;

    /**
     * Creates the policy.
     *
     * @param config the client API settings, whose {@code blockWorldDownloader} switches the policy on
     */
    public WorldDownloaderPolicy(ClientApiConfig config) {
        this.config = config;
        this.node.addListener(PlayerSpawnEvent.class, event -> advertise(event.getPlayer()));
        this.node.addListener(PlayerPluginMessageEvent.class, event -> {
            if (INIT_CHANNEL.equals(event.getIdentifier())) {
                event.getPlayer().sendPluginMessage(CONTROL_CHANNEL, denyAllPacket());
            }
        });
    }

    /**
     * Encodes the permission sections that deny all downloading: section 0 with the unknown functions off,
     * then section 1 with general downloading and every save option off.
     *
     * @return the payload to send on {@value #CONTROL_CHANNEL}
     */
    public static byte[] denyAllPacket() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(0);
            out.writeBoolean(false);
            out.writeInt(1);
            out.writeBoolean(false);
            out.writeInt(0);
            out.writeBoolean(false);
            out.writeBoolean(false);
            out.writeBoolean(false);
            out.writeBoolean(false);
        } catch (IOException e) {
            throw new UncheckedIOException("Writing to memory cannot fail", e);
        }
        return bytes.toByteArray();
    }

    private static void advertise(Player player) {
        player.sendPluginMessage(REGISTER_CHANNEL, INIT_CHANNEL.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    @Override
    public boolean enabled() {
        return this.config.blockWorldDownloader();
    }
}
