package net.onelitefeather.cygnus.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author theEvilReaper
 * @version 1.3.0
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
    public static final Component ONLY_PLAYERS_HAVE_A_VIEW;
    private static final Component PAGE_FOUND_PART;
    private static final Component LEAVE_PART;
    private static final Component JOIN_PART;
    private static final Component SURVIVOR_JOIN_PART_UPPER;
    private static final Component SURVIVOR_JOIN_LOWER_PART;
    public static final Component SLENDER_JOIN_PART;
    private static final Component STATS_HEADER;
    private static final int MAP_ANNOUNCEMENT_MIN_WIDTH = 20;

    static {
        int forceStartTime = GameConfig.FORCE_START_TIME - 1;
        PREFIX = MINI_MESSAGE.deserialize("<gradient:#ff5555:#00:#ff5555:#ff0d00:#00:0.4>Slender </gradient><color:#cc0000>⛧</color>");
        ALREADY_FORCE_STARTED = withPrefix(Component.text("The game has already been force started!", NamedTextColor.RED));
        PHASE_NOT_RUNNING = withPrefix(Component.text("The lobby countdown is not running!", NamedTextColor.RED));
        PHASE_FORCE_STARTED = withMiniPrefix("<gray>The timer has been set to <color:#09ff00><seconds></color> seconds!",
                TagResolver.builder().tag("seconds", (argumentQueue, context) -> Tag.preProcessParsed(String.valueOf(forceStartTime))).build());
        PAGE_FOUND_PART = withPrefix(Component.text("found a page!", NamedTextColor.GRAY));

        VIEW_TIME = withMini("<gradient:#ff5555:#fffffff:#ff5555:#ff0d00:#fffffff:0.6>Time:</gradient>");
        VIEW_PAGES = Component.text("Pages:", NamedTextColor.GRAY);

        SLENDER_WIN_MESSAGE =
                withMini("<gray>has <green>won the game!")
                        .append(Component.newline());

        SURVIVOR_WIN_MESSAGE = withPrefix("<gray>The <green><team>s <gray>has <green>won <gray>the game!",
                TagResolver.builder().tag("team", (argumentQueue, context) ->
                        Tag.preProcessParsed(GameConfig.SURVIVOR_TEAM_NAME)).build());

        LEAVE_PART = Component.text("left the game!", NamedTextColor.GRAY);
        JOIN_PART = Component.text("joined the game!", NamedTextColor.GRAY);
        LIGHT_WENT_OUT = withMiniPrefix("<color:#ff00d4>Your light went out!</color>");
        ONLY_PLAYERS_HAVE_A_VIEW = withMiniPrefix("<red>Only players have a view to lose.");

        SURVIVOR_JOIN_PART_UPPER = withMiniPrefix("<yellow>You are a Survivor! Find various <red>Pages").append(Component.space());

        SURVIVOR_JOIN_LOWER_PART = withMiniPrefix("<yellow>Right-click pages to capture them.")
                .append(Component.newline())
                .append(withMiniPrefix("<yellow>Stay as far away as possible from <color:#5A5A5A>Slenderman</color>"));


        SLENDER_JOIN_PART = withMiniPrefix("<yellow>Find survivors and <red>get as close as you can <yellow>to kill them")
                .append(Component.newline())
                .append(withPrefix(Component.text("Eliminate all survivors to win the game!", NamedTextColor.YELLOW)))
                .append(Component.newline())
                .append(withMiniPrefix("<red>Right-click your <color:#ff00d4>SlenderEye</color> <red>to toggle invisibility!"));

        STATS_HEADER = withMini("<dark_gray>---[<gold>Statistics<dark_gray>]---");
    }

    private Messages() {
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component withPrefix(String component) {
        return PREFIX.append(MINI_MESSAGE.deserialize(component));
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static Component withPrefix(String component, TagResolver... resolvers) {
        return PREFIX.append(Component.space()).append(MINI_MESSAGE.deserialize(component, resolvers));
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component withPrefix(Component component) {
        return PREFIX.append(Component.space()).append(component);
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component withMini(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static Component withMini(String text, TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(text, resolvers);
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component withMiniPrefix(String text) {
        return PREFIX.append(Component.space()).append(MINI_MESSAGE.deserialize(text));
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static Component withMiniPrefix(String text, TagResolver... resolvers) {
        return PREFIX.append(Component.space()).append(MINI_MESSAGE.deserialize(text, resolvers));
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component getPageFoundComponent(Player player) {
        var playerName = Tag.preProcessParsed(player.getUsername());
        var playerTag = TagResolver.builder().tag("player", (argumentQueue, context) -> playerName).build();
        return PREFIX.append(Component.space()).append(withMini("<" + SECONDARY_COLOR + "><player>", playerTag)).append(Component.space()).append(PAGE_FOUND_PART);
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component getDeathComponent(Player player) {
        var playerName = Tag.preProcessParsed(player.getUsername());
        var playerTag = TagResolver.builder().tag("player", (argumentQueue, context) -> playerName).build();
        return PREFIX.append(Component.space()).append(withMini("<red><player> <color:#249D9F>was</color> <color:#ff0000>TAKEN!</color>", playerTag));
    }

    /**
     * Builds the round-end stats box for a survivor.
     *
     * @param player     the survivor the box is about
     * @param pageFounds how many pages the survivor found this round
     * @param died       whether the survivor died this round
     * @return the formatted stats box
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static Component getSurvivorRoundSummaryComponent(Player player, int pageFounds, boolean died) {
        Component deathIcon = died
                ? Component.text("✓", NamedTextColor.GREEN)
                : Component.text("✗", NamedTextColor.RED);
        var resolver = TagResolver.builder()
                .tag("pages", (_, _) -> Tag.preProcessParsed(String.valueOf(pageFounds)))
                .tag("died", Tag.inserting(deathIcon))
                .build();
        return Component.text(player.getUsername(), NamedTextColor.GOLD)
                .append(Component.newline())
                .append(STATS_HEADER)
                .append(Component.newline())
                .append(withMini("<gray>Pages: <green><pages>", resolver))
                .append(Component.newline())
                .append(withMini("<gray>Death: <died>", resolver));
    }

    /**
     * Builds the round-end stats box for the slender.
     *
     * @param player the slender the box is about
     * @param kills  how many survivors the slender caught this round
     * @return the formatted stats box
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static Component getSlenderRoundSummaryComponent(Player player, int kills) {
        var resolver = TagResolver.builder()
                .tag("kills", (_, _) -> Tag.preProcessParsed(String.valueOf(kills)))
                .build();
        return Component.text(player.getUsername(), NamedTextColor.GOLD)
                .append(Component.newline())
                .append(STATS_HEADER)
                .append(Component.newline())
                .append(withMini("<gray>Kills: <green><kills>", resolver));
    }


    @Contract(value = "_, _ -> new", pure = true)
    public static Component getViewComponent(String time, Component pageStatus) {
        return Component.text("Time:", NamedTextColor.GRAY)
                .append(Component.space())
                .append(Component.text(time, NamedTextColor.RED))
                .append(Component.space())
                .append(VIEW_PAGES)
                .append(Component.space())
                .append(pageStatus);
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component getSlenderWinMessage(@Nullable Player player) {
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
    public static Component getJoinMessage(Player player) {
        return PREFIX.append(Component.space()).append(withMini("<color:#249D9F>" + player.getUsername() + "</color>"))
                .append(Component.space()).append(JOIN_PART);
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component getLeaveMessage(Player player) {
        return PREFIX.append(Component.space()).append(withMini("<color:#249D9F>" + player.getUsername() + "</color>"))
                .append(Component.space()).append(LEAVE_PART);
    }

    /**
     * Returns the usage line of the glitch preview command.
     *
     * @param levels how many degrees of tearing there are to pick from
     * @return the created {@link Component} reference
     */
    @Contract(value = "_ -> new", pure = true)
    public static Component getGlitchUsageMessage(int levels) {
        return withMiniPrefix("<gray>Usage: <yellow>/glitch <1-" + levels + "> | off");
    }

    @Contract(value = "_ -> new", pure = true)
    public static Component getSurvivorJoinMessage(String pageCount) {
        return SURVIVOR_JOIN_PART_UPPER.append(withMini("<red>(" + pageCount + " TO WIN)"))
                .append(Component.newline())
                .append(SURVIVOR_JOIN_LOWER_PART);
    }

    /**
     * Returns a {@link Component} which contains information about which map is used for the game.
     *
     * @param mapName  of the map
     * @param builders of the map
     * @return the created {@link Component} reference
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static Component getMapAnnouncementMessage(String mapName, @Nullable List<String> builders) {
        boolean hasBuilders = builders != null && !builders.isEmpty();
        String joinedBuilders = hasBuilders ? String.join(", ", builders) : "";

        int width = Math.max(MAP_ANNOUNCEMENT_MIN_WIDTH, ("Now playing: " + mapName).length());
        if (hasBuilders) {
            width = Math.max(width, ("Built by: " + joinedBuilders).length());
        }
        Component separator = Component.text("─".repeat(width), NamedTextColor.DARK_GRAY);

        Component message = Component.newline().append(separator)
                .append(Component.newline())
                .append(Component.text("Now playing: ", NamedTextColor.GRAY))
                .append(Component.text(mapName, NamedTextColor.GOLD))
                .append(Component.newline());

        if (hasBuilders) {
            message = message
                    .append(Component.text("Built by: ", NamedTextColor.GRAY))
                    .append(Component.text(joinedBuilders, NamedTextColor.AQUA))
                    .append(Component.newline());
        }

        return message.append(separator).append(Component.newline());
    }
}
