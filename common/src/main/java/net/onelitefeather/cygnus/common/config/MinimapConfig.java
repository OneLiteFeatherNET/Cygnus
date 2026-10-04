package net.onelitefeather.cygnus.common.config;

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
