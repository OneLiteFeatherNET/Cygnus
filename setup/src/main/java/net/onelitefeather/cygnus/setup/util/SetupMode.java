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
     * Returns a static method call for the values call.
     *
     * @return the given entries from the enum
     */
    public static @NotNull SetupMode[] getValues() {
        return VALUES;
    }
}
