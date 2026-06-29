package net.onelitefeather.cygnus.setup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.PreProcess;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class contains method and static constant values to handle specific messages during the setup
 * @author theEvilReaper
 * @version 1.2.1
 * @since 1.0.0
 */
public final class SetupMessages {

    public static final Component EMPTY_NAME;
    public static final Component MISSING_MAP_SELECTION;

    public static final Component DISABLED_PAGE_MODE;
    public static final Component SPACE_SEPARATOR;
    public static final Component NO_SPACE_SEPARATOR;
    public static final Component TELEPORT_CLICK;
    public static final Component DELETE_CLICK;
    public static final Component PAGE_MODE_ENABLED;
    public static final Component PAGE_MODE_DISABLED;
    public static final Component PAGE_MODE_INFORM;

    static {
        SPACE_SEPARATOR = Component.text("» ", NamedTextColor.GRAY);
        NO_SPACE_SEPARATOR = Component.text("»", NamedTextColor.GRAY);

        TELEPORT_CLICK = NO_SPACE_SEPARATOR
                .append(Component.space())
                .append(Component.text("Left", NamedTextColor.GREEN))
                .append(Component.space())
                .append(Component.text("click", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text("->", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text("teleport", NamedTextColor.GREEN));

        DELETE_CLICK = NO_SPACE_SEPARATOR
                .append(Component.space())
                .append(Component.text("Right", NamedTextColor.RED))
                .append(Component.space())
                .append(Component.text("click", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text("->", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text("delete", NamedTextColor.RED));
        EMPTY_NAME = Messages.withPrefix(Component.text("An empty name is not allowed", NamedTextColor.RED));
        MISSING_MAP_SELECTION = Messages.withPrefix(
                Component.text("Please select a map to setup and try the command again", NamedTextColor.RED)
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

    /**
     * Creates a new {@link Component} instance with the content of an invalid face.
     *
     * @param face which should be displayed
     * @return the created component
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getInvalidFace(@NotNull String face) {
        PreProcess facePreProcess = Tag.preProcessParsed(face);
        TagResolver faceTag = TagResolver.builder().tag("face", (_, _) -> facePreProcess).build();
        return Messages.withMini("<red>You are looking in an invalid direction! <gray>(<gold><face><gray>)", faceTag);

    }
}
