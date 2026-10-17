package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.stats.YearStatsView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The messages of the statistics display. Kept next to the display code so {@code common} stays free of
 * anything statistics. Names are inserted as plain text, never parsed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
final class StatsMessages {

    /** The text of the loading message, also what the tests look for. */
    static final String LOADING = "Loading statistics…";

    private StatsMessages() {
    }

    /**
     * Statistics are off or the database could not be opened.
     */
    static Component notAvailable() {
        return Messages.withMiniPrefix("<red>Statistics are not available on this server.");
    }

    /**
     * Nobody was ever seen with the name.
     *
     * @param name the name as it was typed
     */
    static Component unknownPlayer(String name) {
        return Messages.withMiniPrefix("<red>No statistics found for <name>.", Placeholder.unparsed("name", name));
    }

    /**
     * The player is known but has no rounds in the year.
     *
     * @param name  the player's name
     * @param year  the year that was asked for
     * @param years the years that have statistics, newest first
     */
    static Component noStatisticsInYear(String name, int year, List<Integer> years) {
        Component message = Messages.withMiniPrefix("<red><name> has no statistics for <year>.",
                Placeholder.unparsed("name", name), Placeholder.unparsed("year", String.valueOf(year)));
        return message.append(yearHint(years));
    }

    /**
     * The sender looked at their own year and has not played in it.
     *
     * @param year  the year that was asked for
     * @param years the years that have statistics, newest first
     */
    static Component selfWithoutRounds(int year, List<Integer> years) {
        Component message = Messages.withMiniPrefix("<red>You have not played a round in <year> yet.",
                Placeholder.unparsed("year", String.valueOf(year)));
        return message.append(yearHint(years));
    }

    private static Component yearHint(List<Integer> years) {
        if (years.isEmpty()) {
            return Component.empty();
        }
        String list = years.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return Messages.withMini(" <gray>Years with statistics: <yellow><years>", Placeholder.unparsed("years", list));
    }

    /**
     * The read failed. The cause is in the log, not in the message.
     */
    static Component loadFailed() {
        return Messages.withMiniPrefix("<red>Could not load statistics, please try again later.");
    }

    /**
     * The year argument is not a year, or an argument is too many.
     */
    static Component usage() {
        return Messages.withMiniPrefix("<gray>Usage: <yellow>/stats [player] [year]");
    }

    /**
     * The console has no statistics of its own.
     */
    static Component onlyPlayers() {
        return Messages.withMiniPrefix("<red>Only players can look at their own statistics. Usage: /stats \\<player> [year]");
    }

    /**
     * The menu is being loaded.
     */
    static Component loading() {
        return Messages.withMiniPrefix("<gray>" + LOADING);
    }

    /**
     * The extra line of the round summary: the totals of the year after the round.
     *
     * @param view the committed year row of the player
     */
    static Component roundTotals(YearStatsView view) {
        return Messages.withMini("<gray>Your <year> totals: <green><rounds></green> rounds, <green><wins></green> wins, "
                        + "<green><pages></green> pages, <green><kills></green> kills",
                Placeholder.unparsed("year", String.valueOf(view.year())),
                Placeholder.unparsed("rounds", String.valueOf(view.roundsPlayed())),
                Placeholder.unparsed("wins", String.valueOf(view.wins())),
                Placeholder.unparsed("pages", String.valueOf(view.pagesFound())),
                Placeholder.unparsed("kills", String.valueOf(view.kills())));
    }
}
