package net.onelitefeather.cygnus.stats.display;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /stats [player] [year]}: prints a compact overview of a player's year in chat. Everybody may look
 * at everybody, offline players included. The overview carries a click that runs
 * {@code /stats open-menu <player> <year>}, which opens the same numbers as a menu.
 * <p>
 * The year is read as text and checked here, so a typo gets the usage message and not Minestom's generic
 * argument error. No permission is needed.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class StatsCommand extends Command {

    /**
     * The subcommand the overview's click runs. A hyphen cannot be part of a Minecraft name, so no player
     * can be shadowed by it.
     */
    static final String MENU_LITERAL = "open-menu";
    /** The earliest year accepted, well before the game existed. */
    static final int MIN_YEAR = 2000;
    /** The latest year accepted, a plain sanity bound. */
    static final int MAX_YEAR = 9999;

    /**
     * Creates the command.
     *
     * @param service reads and shows the statistics
     */
    public StatsCommand(PlayerStatsService service) {
        super("stats");
        Argument<String> player = ArgumentType.Word("player");
        player.setSuggestionCallback((sender, context, suggestion) ->
                service.onlineNames().forEach(name -> suggestion.addEntry(new SuggestionEntry(name))));
        Argument<String> year = ArgumentType.Word("year");

        setDefaultExecutor((sender, context) -> sender.sendMessage(StatsMessages.usage()));

        addSyntax((sender, context) -> service.showOverview(sender, null, service.currentYear()));

        addSyntax((sender, context) -> service.showOverview(sender, context.get(player), service.currentYear()), player);

        addSyntax((sender, context) -> {
            Integer parsed = parseYear(context.get(year));
            if (parsed == null) {
                sender.sendMessage(StatsMessages.usage());
                return;
            }
            service.showOverview(sender, context.get(player), parsed);
        }, player, year);

        addSyntax((sender, context) -> {
            Integer parsed = parseYear(context.get(year));
            if (parsed == null) {
                sender.sendMessage(StatsMessages.usage());
                return;
            }
            if (!(sender instanceof Player viewer)) {
                sender.sendMessage(StatsMessages.onlyPlayers());
                return;
            }
            service.openMenu(viewer, context.get(player), parsed);
        }, ArgumentType.Literal(MENU_LITERAL), player, year);
    }

    /**
     * Reads a year argument.
     *
     * @param text the argument
     * @return the year, {@code null} when it is not a number between {@value #MIN_YEAR} and {@value #MAX_YEAR}
     */
    static @Nullable Integer parseYear(String text) {
        try {
            int year = Integer.parseInt(text);
            return year >= MIN_YEAR && year <= MAX_YEAR ? year : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
