package net.onelitefeather.cygnus.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Settings for the request Cygnus sends to Lunar Client to switch off mods that could give a survivor an edge.
 * <p>
 * The request goes over Lunar's Apollo channel: a ModSetting override per mod that sets the mod's
 * {@code enabled} option to {@code false}. Only the mods in {@link #KNOWN_MODS} can be named, each of them has
 * an option key that the Apollo documentation lists.
 * </p>
 * <p>
 * This is a client courtesy API, not a cheat-proof measure. Lunar Client applies the override, but a modified
 * client or another client ignores it, and nothing here stops a player from using a mod anyway.
 * </p>
 *
 * @param enabled      whether the request is sent at all
 * @param disabledMods the Lunar mods to switch off, lower case, each one of {@link #KNOWN_MODS}
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record LunarConfig(boolean enabled, List<String> disabledMods) {

    /**
     * The Lunar mods Cygnus can switch off. Each one is an Apollo mod whose {@code enabled} option is listed
     * in the Apollo mod documentation (for example {@code minimap.enabled} at
     * {@code https://lunarclient.dev/apollo/developers/mods/minimap}).
     */
    public static final List<String> KNOWN_MODS = List.of("minimap", "waypoints", "freelook", "zoom", "coordinates", "directionhud");

    /**
     * The default settings: the request is sent and switches off every known mod that gives a survivor an edge
     * on the map, the waypoints, the view and the position readouts.
     */
    public static final LunarConfig DEFAULT = new LunarConfig(true, KNOWN_MODS);

    private static final Logger LOGGER = LoggerFactory.getLogger(LunarConfig.class);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if the list is missing or names a mod outside {@link #KNOWN_MODS}
     */
    public LunarConfig {
        if (disabledMods == null) {
            throw new IllegalArgumentException("Lunar disabled mods must not be null");
        }
        for (String mod : disabledMods) {
            if (!KNOWN_MODS.contains(mod)) {
                throw new IllegalArgumentException("'" + mod + "' is not a Lunar mod Cygnus can switch off, known: " + KNOWN_MODS);
            }
        }
        disabledMods = disabledMods.stream().distinct().toList();
    }

    /**
     * Reads the Lunar settings. The section carries the {@code lunar.} prefix.
     * <p>
     * {@code disabled-mods} takes a comma separated list of {@link #KNOWN_MODS}. An unknown name is logged and
     * skipped, the rest of the list still applies. {@code none} switches off nothing; an empty value counts as
     * absent and gives the default list.
     * </p>
     *
     * @param section the {@code lunar.} part of the config
     * @return the Lunar settings
     */
    public static LunarConfig read(ConfigSection section) {
        boolean enabled = section.getBoolean("enabled", DEFAULT.enabled());
        String value = section.getString("disabled-mods");
        if (value == null) {
            return new LunarConfig(enabled, DEFAULT.disabledMods());
        }
        if ("none".equalsIgnoreCase(value)) {
            return new LunarConfig(enabled, List.of());
        }
        List<String> mods = new ArrayList<>();
        for (String part : value.split(",")) {
            String name = part.trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }
            if (!KNOWN_MODS.contains(name)) {
                LOGGER.warn("'{}' is not a Lunar mod Cygnus can switch off ({}). Ignoring it in '{}'",
                        name, String.join(", ", KNOWN_MODS), section.key("disabled-mods"));
                continue;
            }
            mods.add(name);
        }
        return new LunarConfig(enabled, mods);
    }
}
