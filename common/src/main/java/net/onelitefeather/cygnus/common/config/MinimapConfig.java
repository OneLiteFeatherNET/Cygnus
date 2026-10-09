package net.onelitefeather.cygnus.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for the request Cygnus sends to client minimap mods to switch themselves off.
 * <p>
 * This is opt-in on the side of the mods: Xaero's Minimap, VoxelMap and similar mods read an invisible
 * chat line and obey it, a modified client or a mod that does not know the convention ignores it. It is
 * a courtesy to honest players, not an anti-cheat.
 * </p>
 *
 * @param mode how much of the minimap the mods are asked to give up
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public record MinimapConfig(Mode mode) {

    /**
     * The default settings: ask for the minimap to be off.
     */
    public static final MinimapConfig DEFAULT = new MinimapConfig(Mode.DISABLED);

    private static final Logger LOGGER = LoggerFactory.getLogger(MinimapConfig.class);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if the mode is missing
     */
    public MinimapConfig {
        if (mode == null) {
            throw new IllegalArgumentException("Minimap mode must not be null");
        }
    }

    /**
     * Reads the minimap mode. The section carries the {@code minimap.} prefix. Like every
     * unreadable value, an unknown mode falls back to the default.
     *
     * @param section the {@code minimap.} part of the config
     * @return the minimap settings
     */
    public static MinimapConfig read(ConfigSection section) {
        String value = section.getString("mode");
        if (value == null) {
            return DEFAULT;
        }
        for (Mode mode : Mode.values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return new MinimapConfig(mode);
            }
        }
        LOGGER.warn("'{}' is not a minimap mode (disabled, fair, off): '{}'. Falling back to default: {}",
                section.key("mode"), value, DEFAULT.mode());
        return DEFAULT;
    }

    /**
     * How much of the minimap the mods are asked to give up.
     */
    public enum Mode {

        /** The minimap is asked to switch off wherever the mod supports it, and radar and cave mode with it. */
        DISABLED,

        /** The map stays, but radar and cave mode are asked to switch off ("fair play"). */
        FAIR,

        /** Nothing is sent. */
        OFF
    }
}
