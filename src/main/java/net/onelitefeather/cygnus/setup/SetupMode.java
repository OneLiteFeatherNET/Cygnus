package net.onelitefeather.cygnus.setup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SetupMode {

    LOBBY("lobby"),
    GAME("game");

    private static final SetupMode[] VALUES = values();
    final String name;

    SetupMode(@NotNull String name) {
        this.name = name;
    }

    public @NotNull String getName() {
        return this.name;
    }

    public static @Nullable SetupMode parseMode(@NotNull String mode) {
        SetupMode setupMode = null;
        for (int i = 0; i < getValues().length && setupMode == null; i++) {
            if (getValues()[i].getName().equals(mode)) {
                setupMode = getValues()[i];
            }
        }
        return setupMode;
    }

    public static @NotNull SetupMode[] getValues() {
        return VALUES;
    }
}
