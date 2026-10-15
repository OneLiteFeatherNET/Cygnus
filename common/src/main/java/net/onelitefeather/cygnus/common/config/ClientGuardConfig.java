package net.onelitefeather.cygnus.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
 * <p>
 * The shader probe asks the client which shader mods it has, see {@link ShaderMod}. A mod in
 * {@code incompatibleMods} gets the client kicked in the kick mode. A mod in {@code loggedMods} is only logged
 * and recorded. A mod in both lists is treated as incompatible.
 * </p>
 *
 * @param mode                   how a blocked client is handled
 * @param blockedBrands          brand fragments that get a client blocked, lower case
 * @param blockedChannels        channel id prefixes that get a client blocked, lower case
 * @param allowedBrands          brands that may join, exact and lower case. Empty means every brand may join.
 * @param shaderProbe            whether the client is asked for its shader mods on the first spawn
 * @param incompatibleMods       shader mods whose presence gets a client kicked in the kick mode
 * @param loggedMods             shader mods whose presence is logged and recorded, but never kicks
 * @param shaderProbeTimeoutTicks how many server ticks the client has to answer one probe sign. A client that
 *                               does not answer in time is not kicked, since the probe is then ambiguous.
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record ClientGuardConfig(Mode mode, List<String> blockedBrands, List<String> blockedChannels,
                                List<String> allowedBrands, boolean shaderProbe, List<ShaderMod> incompatibleMods,
                                List<ShaderMod> loggedMods, int shaderProbeTimeoutTicks) {

    /** The brands of the cheat clients Cygnus knows of. */
    public static final List<String> DEFAULT_BLOCKED_BRANDS = List.of(
            "meteor", "wurst", "aristois", "liquidbounce", "impact", "future", "rusherhack", "inertia", "bleachhack");

    /**
     * The World Downloader Mod registers its {@code wdl:init}, {@code wdl:request} and {@code wdl:control}
     * channels on every server, which makes it visible to the server even when it hides its brand.
     */
    public static final List<String> DEFAULT_BLOCKED_CHANNELS = List.of("wdl:");

    /**
     * The shader mods that break the resource pack's core shaders. Iris is kicked even without an active shader
     * pack, since the server cannot see whether one is active.
     */
    public static final List<ShaderMod> DEFAULT_INCOMPATIBLE_MODS = List.of(
            ShaderMod.IRIS, ShaderMod.OPTIFINE, ShaderMod.CANVAS);

    /** The shader mods that only lose the cosmetic terrain fog, so they are logged and never kicked. */
    public static final List<ShaderMod> DEFAULT_LOGGED_MODS = List.of(
            ShaderMod.SODIUM, ShaderMod.EMBEDDIUM, ShaderMod.NVIDIUM);

    /** The default time a client has to answer one probe sign: 100 ticks, five seconds. */
    public static final int DEFAULT_SHADER_PROBE_TIMEOUT_TICKS = 100;

    /**
     * The defaults: a client with a blocked brand or channel is kicked, there is no brand whitelist, and the
     * shader probe kicks the incompatible mods.
     */
    public static final ClientGuardConfig DEFAULT = new ClientGuardConfig(
            Mode.KICK, DEFAULT_BLOCKED_BRANDS, DEFAULT_BLOCKED_CHANNELS, List.of(),
            true, DEFAULT_INCOMPATIBLE_MODS, DEFAULT_LOGGED_MODS, DEFAULT_SHADER_PROBE_TIMEOUT_TICKS);

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientGuardConfig.class);

    /**
     * Checks the values and normalises the lists. A missing list becomes empty.
     *
     * @throws IllegalArgumentException if the mode is missing or the probe timeout is below one tick
     */
    public ClientGuardConfig {
        if (mode == null) {
            throw new IllegalArgumentException("Client guard mode must not be null");
        }
        if (shaderProbeTimeoutTicks < 1) {
            throw new IllegalArgumentException("Shader probe timeout must be at least 1 tick");
        }
        blockedBrands = normalize(blockedBrands);
        blockedChannels = normalize(blockedChannels);
        allowedBrands = normalize(allowedBrands);
        incompatibleMods = distinct(incompatibleMods);
        loggedMods = distinct(loggedMods);
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
                readList(section, "allowed-brands", List.of()),
                section.getBoolean("shader-probe", DEFAULT.shaderProbe()),
                readMods(section, "incompatible-mods", DEFAULT_INCOMPATIBLE_MODS),
                readMods(section, "logged-mods", DEFAULT_LOGGED_MODS),
                readProbeTimeout(section));
    }

    private static List<ShaderMod> readMods(ConfigSection section, String key, List<ShaderMod> fallback) {
        String value = section.getString(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        List<ShaderMod> mods = new ArrayList<>();
        for (String id : value.split(",")) {
            String trimmed = id.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Optional<ShaderMod> mod = ShaderMod.byId(trimmed);
            if (mod.isPresent()) {
                mods.add(mod.get());
            } else {
                LOGGER.warn("'{}' is not a shader mod the probe knows ({}). Skipping it.", section.key(key), trimmed);
            }
        }
        return mods;
    }

    private static int readProbeTimeout(ConfigSection section) {
        int timeout = section.getInt("shader-probe-timeout-ticks", DEFAULT_SHADER_PROBE_TIMEOUT_TICKS);
        if (timeout < 1) {
            LOGGER.warn("'{}' must be at least 1 tick, got {}. Falling back to default: {}",
                    section.key("shader-probe-timeout-ticks"), timeout, DEFAULT_SHADER_PROBE_TIMEOUT_TICKS);
            return DEFAULT_SHADER_PROBE_TIMEOUT_TICKS;
        }
        return timeout;
    }

    private static <T> List<T> distinct(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().distinct().toList();
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
