package net.onelitefeather.cygnus.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class Messages {

    private static final String SECONDARY_COLOR = "#249D9F";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Component PREFIX;
    public static final Component ALREADY_FORCE_STARTED;
    public static final Component PHASE_NOT_RUNNING;
    public static final Component PHASE_FORCE_STARTED;
    public static final Component VIEW_TIME;
    public static final Component VIEW_PAGES;
    public static final Component SLENDER_WIN_MESSAGE;
    public static final Component SURVIVOR_WIN_MESSAGE;
    public static final Component LIGHT_WENT_OUT;
    private static final Component PAGE_FOUND_PART;
    private static final Component LEAVE_PART;
    private static final Component JOIN_PART;
    private static final Component SURVIVOR_JOIN_PART_UPPER;
    private static final Component SURVIVOR_JOIN_LOWER_PART;
    public static final Component SLENDER_JOIN_PART;

    static {
        int forceStartTime = GameConfig.FORCE_START_TIME - 1;
        PREFIX = MINI_MESSAGE.deserialize("<gradient:#ff5555:#00:#ff5555:#ff0d00:#00:0.4>Slender </gradient><color:#cc0000>⛧</color>");
        ALREADY_FORCE_STARTED = withMiniPrefix("<red>The game has already been force started!");
        PHASE_NOT_RUNNING = withMiniPrefix("<red>The lobby countdown is not running!");
        PHASE_FORCE_STARTED = withMiniPrefix("<gray>The timer has been set to <color:#09ff00><seconds></color> seconds!",
                TagResolver.builder().tag("seconds", (argumentQueue, context) -> Tag.preProcessParsed(String.valueOf(forceStartTime))).build());
        PAGE_FOUND_PART = withMini("<gray>found a page!");

        VIEW_TIME = withMini("<gradient:#ff5555:#fffffff:#ff5555:#ff0d00:#fffffff:0.6>Time:</gradient>");
        VIEW_PAGES = withMini("<gray>Pages:");

        SLENDER_WIN_MESSAGE =
                withMini("<gray>has <green>won the game!")
                        .append(Component.newline());

        SURVIVOR_WIN_MESSAGE = withPrefix("<gray>The <green><team>s <gray>has <green>won <gray>the game!",
                TagResolver.builder().tag("team", (argumentQueue, context) ->
                        Tag.preProcessParsed(GameConfig.SURVIVOR_TEAM_NAME)).build());

        LEAVE_PART = withMini("<gray>left the game!");
        JOIN_PART = withMini("<gray>joined the game!");
        LIGHT_WENT_OUT = withMiniPrefix("<color:#ff00d4>Your light went out!</color>");

        SURVIVOR_JOIN_PART_UPPER = withMiniPrefix("<yellow>You are a Survivor! Find various <red>Pages").append(Component.space());

        SURVIVOR_JOIN_LOWER_PART = withMiniPrefix("<yellow>Right-click pages to capture them.")
                .append(Component.newline())
                .append(withMiniPrefix("<yellow>Stay as far away as possible from <color:#5A5A5A>Slenderman</color>"));


        SLENDER_JOIN_PART = withMiniPrefix("<yellow>Find survivors and <red>get as close as you can <yellow>to kill them")
                .append(Component.newline())
                .append(withMiniPrefix("<yellow>Eliminate all survivors to win the game!"))
                .append(Component.newline())
                .append(withMiniPrefix("<red>Right-click your <color:#ff00d4>SlenderEye</color> <red>to toggle invisibility!"));
    }

    private Messages() {
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component withPrefix(@NotNull String component) {
        return PREFIX.append(MINI_MESSAGE.deserialize(component));
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Component withPrefix(@NotNull String component, @NotNull TagResolver... resolvers) {
        return PREFIX.append(Component.space()).append(MINI_MESSAGE.deserialize(component, resolvers));
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component withPrefix(@NotNull Component component) {
        return PREFIX.append(Component.space()).append(component);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component withMini(@NotNull String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Component withMini(@NotNull String text, @NotNull TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(text, resolvers);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component withMiniPrefix(@NotNull String text) {
        return PREFIX.append(Component.space()).append(MINI_MESSAGE.deserialize(text));
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Component withMiniPrefix(@NotNull String text, @NotNull TagResolver... resolvers) {
        return PREFIX.append(Component.space()).append(MINI_MESSAGE.deserialize(text, resolvers));
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getPageFoundComponent(@NotNull Player player) {
        var playerName = Tag.preProcessParsed(player.getUsername());
        var playerTag = TagResolver.builder().tag("player", (argumentQueue, context) -> playerName).build();
        return PREFIX.append(Component.space()).append(withMini("<" + SECONDARY_COLOR + "><player>", playerTag)).append(Component.space()).append(PAGE_FOUND_PART);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getDeathComponent(@NotNull Player player) {
        var playerName = Tag.preProcessParsed(player.getUsername());
        var playerTag = TagResolver.builder().tag("player", (argumentQueue, context) -> playerName).build();
        return PREFIX.append(Component.space()).append(withMini("<red><player> <color:#249D9F>was</color> <color:#ff0000>TAKEN!</color>", playerTag));
    }


    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Component getViewComponent(@NotNull String time, @NotNull Component pageStatus) {
        return Component.text("Time:", NamedTextColor.GRAY)
                .append(Component.space())
                .append(Component.text(time, NamedTextColor.RED))
                .append(Component.space())
                .append(VIEW_PAGES)
                .append(Component.space())
                .append(pageStatus);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getSlenderWinMessage(@Nullable Player player) {
        if (player == null || player.getDisplayName() == null) {
            return Component.newline()
                    .append(SLENDER_WIN_MESSAGE)
                    .append(Component.newline());
        }
        return Component.newline()
                .append(PREFIX)
                .append(Component.space())
                .append(withMini("<gray>Slenderman (<red>" + player.getUsername() + "<gray>)"))
                .append(Component.space())
                .append(SLENDER_WIN_MESSAGE)
                .append(Component.newline());
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getJoinMessage(@NotNull Player player) {
        return PREFIX.append(Component.space()).append(withMini("<color:#249D9F>" + player.getUsername() + "</color>"))
                .append(Component.space()).append(JOIN_PART);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getLeaveMessage(@NotNull Player player) {
        return PREFIX.append(Component.space()).append(withMini("<color:#249D9F>" + player.getUsername() + "</color>"))
                .append(Component.space()).append(LEAVE_PART);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Component getSurvivorJoinMessage(@NotNull String pageCount) {
        return SURVIVOR_JOIN_PART_UPPER.append(withMini("<red>(" + pageCount + " TO WIN)"))
                .append(Component.newline())
                .append(SURVIVOR_JOIN_LOWER_PART);
    }
}
