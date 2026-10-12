package net.onelitefeather.cygnus.clientguard;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.NetworkBuffer;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.ClientGuardConfig;
import net.onelitefeather.cygnus.common.config.ShaderMod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads what a client says about itself and kicks the cheat clients among them.
 * <p>
 * A client sends its brand on {@value #BRAND_CHANNEL} as one string: a VarInt length, then UTF-8. It lists the
 * channels it understands on {@value #REGISTER_CHANNEL} as UTF-8 names separated by a zero byte. Both are sent
 * during the configuration phase or the play phase, depending on the client, so the feature listens to
 * {@link PlayerPluginMessageEvent}, which Minestom fires for both.
 * </p>
 * <p>
 * Every message updates the player's {@link ClientProfile} and checks it again. A blocked client is logged once
 * and then either kicked ({@link ClientGuardConfig.Mode#KICK}) or left alone ({@link ClientGuardConfig.Mode#LOG}).
 * A player's profile is forgotten when they disconnect. The brand and the channel list may arrive in either order,
 * so the whitelist only judges a client once it has named a brand, see {@link ClientInspector}; the blocked
 * channels apply from the first message on. The feature only answers what the client sent, so a modified client
 * that stays silent is not caught.
 * </p>
 * <p>
 * Unless the shader probe is switched off, a player is also asked which shader mods the client has, see
 * {@link ShaderModProbe}. The mods the probe finds are recorded on the profile. An incompatible mod is handled
 * like a blocked brand, a logged mod is only logged. Nothing here is proof of a vanilla client: blocker mods can
 * hide every mod from the probe.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class ClientGuard implements GameFeature {

    /** The channel a client names its brand on. */
    public static final String BRAND_CHANNEL = "minecraft:brand";

    /** The channel a client lists the channels it understands on. */
    public static final String REGISTER_CHANNEL = "minecraft:register";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientGuard.class);

    private final EventNode<Event> node = EventNode.all("client-guard");
    private final ClientGuardConfig config;
    private final Map<UUID, ClientProfile> profiles = new ConcurrentHashMap<>();
    private final Set<UUID> flagged = ConcurrentHashMap.newKeySet();

    /**
     * Creates the guard and hooks it into its node. The shader probe's fallback tokens come from the default random
     * source.
     *
     * @param config the rules and the reaction to a blocked client
     */
    public ClientGuard(ClientGuardConfig config) {
        this(config, RandomGenerator.getDefault());
    }

    /**
     * Creates the guard with a given random source for the shader probe's fallback tokens.
     *
     * @param config the rules and the reaction to a blocked client
     * @param random the source of the probe's fallback tokens. A seeded source makes the probe repeatable.
     */
    public ClientGuard(ClientGuardConfig config, RandomGenerator random) {
        this.config = config;
        this.node.addListener(PlayerPluginMessageEvent.class, this::onPluginMessage);
        this.node.addListener(PlayerDisconnectEvent.class, event -> forget(event.getPlayer().getUuid()));
        if (config.shaderProbe() && config.mode() != ClientGuardConfig.Mode.OFF) {
            this.node.addChild(new ShaderModProbe(config, random, this::onShaderMods).node());
        }
    }

    /**
     * Returns what the client of a player has said about itself so far.
     *
     * @param playerId the player's UUID
     * @return the profile, or empty if the player has not sent a brand or channel list
     */
    public Optional<ClientProfile> profile(UUID playerId) {
        return Optional.ofNullable(this.profiles.get(playerId));
    }

    private void onPluginMessage(PlayerPluginMessageEvent event) {
        String channel = event.getIdentifier();
        boolean brand = BRAND_CHANNEL.equals(channel);
        if (!brand && !REGISTER_CHANNEL.equals(channel)) {
            return;
        }
        Player player = event.getPlayer();
        UUID playerId = player.getUuid();
        ClientProfile current = this.profiles.getOrDefault(playerId, ClientProfile.EMPTY);
        ClientProfile updated;
        if (brand) {
            String name = readBrand(event.getMessage());
            if (name == null) {
                return;
            }
            updated = current.withBrand(name);
        } else {
            updated = current.withChannels(readChannels(event.getMessage()));
        }
        this.profiles.put(playerId, updated);
        this.evaluate(player, updated);
    }

    private void evaluate(Player player, ClientProfile profile) {
        if (!(ClientInspector.inspect(profile, this.config) instanceof Verdict.Blocked blocked)) {
            return;
        }
        if (!this.flagged.add(player.getUuid())) {
            return;
        }
        LOGGER.warn("Blocked client of {} ({}): {}", player.getUsername(), player.getUuid(), blocked.reason());
        if (this.config.mode() == ClientGuardConfig.Mode.KICK) {
            player.kick(Messages.CLIENT_BLOCKED);
        }
    }

    /**
     * Records the shader mods the probe found and reacts to the incompatible ones. An incompatible mod kicks in the
     * kick mode, once per player. A logged mod is only written to the log.
     */
    private void onShaderMods(Player player, Set<ShaderMod> mods) {
        UUID playerId = player.getUuid();
        this.profiles.compute(playerId, (id, current) ->
                (current == null ? ClientProfile.EMPTY : current).withDetectedMods(mods));
        List<ShaderMod> incompatible = mods.stream().filter(this.config.incompatibleMods()::contains).sorted().toList();
        if (!incompatible.isEmpty()) {
            if (this.flagged.add(playerId)) {
                LOGGER.warn("Blocked client of {} ({}): incompatible shader mods {}", player.getUsername(), playerId, incompatible);
                if (this.config.mode() == ClientGuardConfig.Mode.KICK) {
                    player.kick(Messages.INCOMPATIBLE_MODS);
                }
            }
            return;
        }
        List<ShaderMod> logged = mods.stream().filter(this.config.loggedMods()::contains).sorted().toList();
        if (!logged.isEmpty()) {
            LOGGER.info("Client of {} ({}) runs shader mods that only lose cosmetic terrain fog: {}", player.getUsername(), playerId, logged);
        }
    }

    private void forget(UUID playerId) {
        this.profiles.remove(playerId);
        this.flagged.remove(playerId);
    }

    /**
     * Reads the brand: a VarInt length and the UTF-8 name. A payload that does not decode is ignored, the
     * client just has no brand yet.
     *
     * @param payload the message's bytes
     * @return the trimmed brand, or {@code null} if it is unreadable or blank
     */
    private static @Nullable String readBrand(byte[] payload) {
        try {
            String brand = NetworkBuffer.wrap(payload, 0, payload.length).read(NetworkBuffer.STRING).trim();
            return brand.isEmpty() ? null : brand;
        } catch (RuntimeException exception) {
            LOGGER.debug("Ignoring a malformed client brand payload of {} bytes", payload.length, exception);
            return null;
        }
    }

    /**
     * Splits the register message at its zero bytes. Blank names are dropped.
     *
     * @param payload the message's bytes
     * @return the channel ids the client registered
     */
    private static Set<String> readChannels(byte[] payload) {
        Set<String> channels = new HashSet<>();
        int start = 0;
        for (int index = 0; index <= payload.length; index++) {
            if (index == payload.length || payload[index] == 0) {
                String channel = new String(payload, start, index - start, StandardCharsets.UTF_8).trim();
                if (!channel.isEmpty()) {
                    channels.add(channel);
                }
                start = index + 1;
            }
        }
        return channels;
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    @Override
    public boolean enabled() {
        return this.config.mode() != ClientGuardConfig.Mode.OFF;
    }
}
