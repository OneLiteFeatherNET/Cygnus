package net.onelitefeather.cygnus.common.permission;

import net.kyori.adventure.util.TriState;
import net.luckperms.api.util.Tristate;

/**
 * Converts between LuckPerms' and Adventure's tri-state types, which model the same three values
 * under two unrelated types.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 **/
public final class TriStates {

    private TriStates() {
    }

    /**
     * Converts a LuckPerms tri-state into its Adventure counterpart.
     *
     * @param tristate the LuckPerms value to convert
     * @return the matching Adventure value, where {@code UNDEFINED} maps to {@code NOT_SET}
     */
    public static TriState fromLuckPerms(Tristate tristate) {
        return switch (tristate) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.NOT_SET;
        };
    }
}
