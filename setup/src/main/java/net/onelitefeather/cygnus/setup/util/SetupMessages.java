package net.onelitefeather.cygnus.setup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.PreProcess;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The class contains method and static constant values to handle specific messages during the setup
 *
 * @author theEvilReaper
 * @version 1.3.0
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
    public static final Component SURVIVOR_MODE_ENABLED;
    public static final Component SURVIVOR_MODE_DISABLED;
    public static final Component DUPLICATE_PAGE;
    public static final Component CREEK_MODE_ENABLED = Messages.withPrefix(
            Component.text("The creek route mode has been", NamedTextColor.GRAY)
                    .append(Component.space())
                    .append(Component.text("enabled", NamedTextColor.GREEN)));
    public static final Component CREEK_MODE_DISABLED = Messages.withPrefix(
            Component.text("The creek route mode has been", NamedTextColor.GRAY)
                    .append(Component.space())
                    .append(Component.text("disabled", NamedTextColor.RED)));
    public static final Component NO_ACTIVE_CREEK_ROUTE = Messages.withPrefix(
            Component.text("Create or select a creek route first", NamedTextColor.RED));
    public static final Component NO_CREEK_POINT_TO_REMOVE = Messages.withPrefix(
            Component.text("The active creek route has no point to remove", NamedTextColor.RED));

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
        SURVIVOR_MODE_ENABLED = Messages.withPrefix(
                Component.text("The survivor mode has been", NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text("enabled", NamedTextColor.GREEN))
        );
        SURVIVOR_MODE_DISABLED = Messages.withPrefix(
                Component.text("The survivor mode has been", NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text("disabled", NamedTextColor.RED))
        );
        DUPLICATE_PAGE = Messages.withPrefix(
                Component.text("A page with this direction already exists at this position", NamedTextColor.RED)
        );
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

    /**
     * Creates a new {@link Component} instance which informs the player to disable the given mode.
     *
     * @param mode which should be displayed
     * @return the created component
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getModeInform(@NotNull String mode) {
        PreProcess modePreProcess = Tag.preProcessParsed(mode);
        TagResolver modeTag = TagResolver.builder().tag("mode", (_, _) -> modePreProcess).build();
        return Messages.withMiniPrefix("<gray>Don't forget to disable <mode> mode", modeTag);
    }

    /**
     * Creates the message shown after a creek route was created.
     *
     * @param name the route's name
     * @return the created component
     */
    public static Component getCreekRouteCreated(String name) {
        return Messages.withPrefix(Component.text("Created creek route ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.GREEN)));
    }

    /**
     * Creates the message shown when a creek route name is already taken.
     *
     * @param name the route's name
     * @return the created component
     */
    public static Component getDuplicateCreekRoute(String name) {
        return Messages.withPrefix(Component.text("A creek route named ", NamedTextColor.RED)
                .append(Component.text(name, NamedTextColor.GOLD))
                .append(Component.text(" already exists", NamedTextColor.RED)));
    }

    /**
     * Creates the message shown after a point was added to a creek route.
     *
     * @param name  the route's name
     * @param count the number of the new point
     * @return the created component
     */
    public static Component getCreekPointAdded(String name, int count) {
        return Messages.withPrefix(Component.text("Added point " + count + " to ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.GREEN)));
    }

    /**
     * Creates the message shown after the last point of a creek route was removed.
     *
     * @param name  the route's name
     * @param count the number of points left
     * @return the created component
     */
    public static Component getCreekPointRemoved(String name, int count) {
        return Messages.withPrefix(Component.text("Removed a point, ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.GREEN))
                .append(Component.text(" now has " + count, NamedTextColor.GRAY)));
    }

    /**
     * Creates the message shown after a creek route was selected.
     *
     * @param name the route's name
     * @return the created component
     */
    public static Component getCreekRouteSelected(String name) {
        return Messages.withPrefix(Component.text("Now editing creek route ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.GREEN)));
    }

    /**
     * Creates the message shown after a creek route was deleted.
     *
     * @param name the route's name
     * @return the created component
     */
    public static Component getCreekRouteDeleted(String name) {
        return Messages.withPrefix(Component.text("Deleted creek route ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.RED)));
    }

    /**
     * Returns a list that contains the given components which some additional empty components
     *
     * @param components to add
     * @return the list with the {@link Component}s
     */
    @Contract(value = "_ -> new", pure = true)
    public static List<Component> getLore(@NotNull Component... components) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.addAll(Arrays.asList(components));
        lore.add(Component.empty());
        return lore;
    }
}
