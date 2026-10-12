package net.onelitefeather.cygnus.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Settings for the client check: which client brands and mod channels Cygnus turns away.
 * <p>
 * A client names itself on the {@code minecraft:brand} channel and lists the channels it understands on
 * {@code minecraft:register}. Both are sent by the client itself, so a modified client can lie about them.
 * The check catches the clients that do not bother, which is the point: it is not a substitute for
 * anti-cheat.
 * </p>
 * <p>
 * Brands are matched as fragments: a brand containing a blocked fragment is blocked, so {@code meteor}
 * also catches {@code meteor-client}. Channels are matched as prefixes of the channel id. Everything is
 * compared case-insensitively. A blank list value keeps the default; {@code allowed-brands} is the only
 * list that defaults to empty.
 * </p>
 *
 * @param mode           how a blocked client is handled
 * @param blockedBrands  brand fragments that get a client blocked, lower case
 * @param blockedChannels channel id prefixes that get a client blocked, lower case
 * @param allowedBrands  brands that may join, exact and lower case. Empty means every brand may join.
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record ClientGuardConfig(Mode mode, List<String> blockedBrands, List<String> blockedChannels,
                                List<String> allowedBrands) {

    /** The brands of the cheat clients Cygnus knows of. */
    public static final List<String> DEFAULT_BLOCKED_BRANDS = List.of(
            "meteor", "wurst", "aristois", "liquidbounce", "impact", "future", "rusherhack", "inertia", "bleachhack");

    /**
     * The World Downloader Mod registers its {@code wdl:init}, {@code wdl:request} and {@code wdl:control}
     * channels on every server, which makes it visible to the server even when it hides its brand.
     */
    public static final List<String> DEFAULT_BLOCKED_CHANNELS = List.of("wdl:");

    /** The defaults: a client with a blocked brand or channel is kicked, and there is no brand whitelist. */
    public static final ClientGuardConfig DEFAULT = new ClientGuardConfig(
            Mode.KICK, DEFAULT_BLOCKED_BRANDS, DEFAULT_BLOCKED_CHANNELS, List.of());

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientGuardConfig.class);

    /**
     * Checks the values and normalises the lists. A missing list becomes empty.
     *
     * @throws IllegalArgumentException if the mode is missing
     */
    public ClientGuardConfig {
        if (mode == null) {
            throw new IllegalArgumentException("Client guard mode must not be null");
        }
        blockedBrands = normalize(blockedBrands);
        blockedChannels = normalize(blockedChannels);
        allowedBrands = normalize(allowedBrands);
    }

    /**
     * Reads the client check settings. The section carries the {@code client-guard.} prefix. An unknown mode
     * falls back to the default with a warning, and so does a list that is present but blank.
     *
     * @param section the {@code client-guard.} part of the config
     * @return the client check settings
     */
    public static ClientGuardConfig read(ConfigSection section) {
        return new ClientGuardConfig(
                readMode(section),
                readList(section, "blocked-brands", DEFAULT_BLOCKED_BRANDS),
                readList(section, "blocked-channels", DEFAULT_BLOCKED_CHANNELS),
                readList(section, "allowed-brands", List.of()));
    }

    private static Mode readMode(ConfigSection section) {
        String value = section.getString("mode");
        if (value == null) {
            return DEFAULT.mode();
        }
        for (Mode mode : Mode.values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        LOGGER.warn("'{}' is not a client guard mode (off, log, kick): '{}'. Falling back to default: {}",
                section.key("mode"), value, DEFAULT.mode());
        return DEFAULT.mode();
    }

    private static List<String> readList(ConfigSection section, String key, List<String> fallback) {
        String value = section.getString(key);
        if (value == null) {
            return fallback;
        }
        return Arrays.asList(value.split(","));
    }

    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * What happens to a client that is blocked.
     */
    public enum Mode {

        /** Nothing is checked: the feature is not hooked into the event tree. */
        OFF,

        /** A blocked client is logged and allowed to stay. */
        LOG,

        /** A blocked client is logged and kicked. */
        KICK
    }
}
