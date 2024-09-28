package net.onelitefeather.cygnus.setup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onelitefeather.cygnus.common.Messages;

public final class SetupMessages {

    public static final Component EMPTY_NAME;
    public static final Component MISSING_MAP_SELECTION;
    public static final Component NOT_ALLOWED_IN_LOBBY;

    public static final Component DISABLED_PAGE_MODE;

    public static final Component PAGE_MODE_ENABLED;
    public static final Component PAGE_MODE_DISABLED;
    public static final Component PAGE_MODE_INFORM;

    static {
        EMPTY_NAME = Messages.withPrefix(Component.text("An empty name is not allowed", NamedTextColor.RED));
        MISSING_MAP_SELECTION = Messages.withPrefix(
                Component.text("Please select a map to setup and try the command again", NamedTextColor.RED)
        );
        NOT_ALLOWED_IN_LOBBY = Messages.withPrefix(
                Component.text("The page mode is not available in the lobby setup mode!", NamedTextColor.RED)
        );
        DISABLED_PAGE_MODE = Messages.withPrefix(
                Component.text("The page mode is now disabled", NamedTextColor.RED)
        );
        PAGE_MODE_ENABLED = Messages.withPrefix(
                Component.text("The page mode has been", NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text("enabled", NamedTextColor.GREEN))
        );
        PAGE_MODE_DISABLED = Messages.withPrefix(
                Component.text("The page mode has been", NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text("disabled", NamedTextColor.RED))
        );
        PAGE_MODE_INFORM = Messages.withPrefix(Component.text("Don't forget to disable this mode", NamedTextColor.GRAY));
    }

    private SetupMessages() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

}
