package net.onelitefeather.cygnus.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Settings for the requests Cygnus sends to client mods through their own server APIs: WorldDownloader's
 * permission channel and LabyMod's addon moderation packet.
 * <p>
 * These are courtesy APIs. Both mods honour them only because the mod author built the request in, a
 * modified client can ignore every one of them. They raise the cost of cheating for an honest player, they
 * are not an anti-cheat.
 * </p>
 *
 * @param blockWorldDownloader     whether WorldDownloader is asked to refuse all downloading of the world
 * @param labyModDisabledAddons    the LabyMod addon namespaces the client is asked to disable
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record ClientApiConfig(boolean blockWorldDownloader, List<String> labyModDisabledAddons) {

    /**
     * The default settings: WorldDownloader is blocked, no LabyMod addon is disabled. The list is empty
     * because no addon is known to Cygnus to break the round.
     */
    public static final ClientApiConfig DEFAULT = new ClientApiConfig(true, List.of());

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApiConfig.class);

    /** The key of the addon list, relative to the {@code client-api.} section. */
    private static final String ADDONS_KEY = "labymod-disabled-addons";

    /** A namespace in the Minecraft identifier style: lower case letters, digits, dot, dash and underscore. */
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if the addon list is missing or holds a blank namespace
     */
    public ClientApiConfig {
        if (labyModDisabledAddons == null) {
            throw new IllegalArgumentException("LabyMod disabled addons must not be null");
        }
        for (String namespace : labyModDisabledAddons) {
            if (namespace == null || namespace.isBlank()) {
                throw new IllegalArgumentException("LabyMod addon namespace must not be blank");
            }
        }
        labyModDisabledAddons = List.copyOf(labyModDisabledAddons);
    }

    /**
     * Reads the client API settings. The section carries the {@code client-api.} prefix. A missing key takes
     * its default, and a namespace that is not a valid identifier is logged and skipped.
     *
     * @param section the {@code client-api.} part of the config
     * @return the client API settings
     */
    public static ClientApiConfig read(ConfigSection section) {
        boolean block = section.getBoolean("block-world-downloader", DEFAULT.blockWorldDownloader());
        return new ClientApiConfig(block, parseNamespaces(section));
    }

    private static List<String> parseNamespaces(ConfigSection section) {
        String raw = section.getString(ADDONS_KEY);
        if (raw == null) {
            return List.of();
        }
        Set<String> namespaces = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String namespace = part.trim();
            if (namespace.isEmpty()) {
                continue;
            }
            if (NAMESPACE.matcher(namespace).matches()) {
                namespaces.add(namespace);
            } else {
                LOGGER.warn("'{}' is not a LabyMod addon namespace (lower case letters, digits, '.', '-', '_'), "
                        + "ignoring it in '{}'", namespace, section.key(ADDONS_KEY));
            }
        }
        return List.copyOf(namespaces);
    }
}
