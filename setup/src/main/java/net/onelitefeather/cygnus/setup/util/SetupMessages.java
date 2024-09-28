package net.onelitefeather.cygnus.setup.util;

import net.kyori.adventure.text.Component;
import net.onelitefeather.cygnus.common.Messages;

public final class SetupMessages {

    public static final Component MISSING_MAP_SELECTION;

    static {
        MISSING_MAP_SELECTION = Messages.withMini("<red>Please select a map to setup and try the command again");
    }

    private SetupMessages() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

}
