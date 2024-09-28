package net.onelitefeather.cygnus.setup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onelitefeather.cygnus.common.Messages;

public final class SetupMessages {

    public static final Component EMPTY_NAME;
    public static final Component MISSING_MAP_SELECTION;
    public static final Component NOT_ALLOWED_IN_LOBBY;

    public static final Component DISABLED_PAGE_MODE;

    static {
        EMPTY_NAME = Messages.withPrefix(Component.text("An empty name is not allowed", NamedTextColor.RED));
        MISSING_MAP_SELECTION = Messages.withPrefix(
                Component.text("Please select a map to setup and try the command again", NamedTextColor.RED)
        );
        NOT_ALLOWED_IN_LOBBY = Messages.withPrefix(
                Component.text("The page mode is not available in the lobby setup mode!", NamedTextColor.RED)
        );
        DISABLED_PAGE_MODE =  Messages.withPrefix(
                Component.text("The page mode is now disabled", NamedTextColor.RED)
        );
    }

    private SetupMessages() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

}
