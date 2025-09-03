package net.onelitefeather.cygnus.setup.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The enumeration contains all modes which can be used in the setup process.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public enum SetupMode {

    LOBBY("lobby"),
    GAME("game");

    private static final SetupMode[] VALUES = values();
    private final String name;

    /**
     * Create a new entry for the setup enumeration.
     *
     * @param name the name for the entry
     */
    SetupMode(@NotNull String name) {
        this.name = name;
    }

    /**
     * Returns the name from the mode entry.
     *
     * @return the given name
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * Returns a {@link SetupMode} entry which matches with the given string.
     *
     * @param mode the mode parse
     * @return null when no mode is found otherwise the fetched mode
     */
    public static @Nullable SetupMode parseMode(@NotNull String mode) {
        SetupMode setupMode = null;
        for (int i = 0; i < getValues().length && setupMode == null; i++) {
            if (getValues()[i].getName().equals(mode)) {
                setupMode = getValues()[i];
            }
        }
        return setupMode;
    }

    /**
     * Returns a boolean indicator if the given mode is the same as the requested mode.
     *
     * @param requestedMode the mode to check
     * @param ordinalId     the id to check
     * @return true when the mode is the same otherwise false
     */
    public static boolean isMode(@NotNull SetupMode requestedMode, int ordinalId) {
        if (ordinalId < 0 || ordinalId > getValues().length) return false;
        return requestedMode.ordinal() == ordinalId;
    }

    /**
     * Returns a static method call for the values call.
     *
     * @return the given entries from the enum
     */
    public static @NotNull SetupMode[] getValues() {
        return VALUES;
    }
}
