package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.onelitefeather.cygnus.stats.YearStatsView;

import java.util.List;
import java.util.Locale;

/**
 * Turns a year of statistics into the lines the chat overview and the menu items show: the main stored
 * counters plus the values derived from them. The derived values come from {@link YearStatsView}, which
 * answers {@code 0} where there is nothing to divide by; this class only formats them.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
final class StatsOverview {

    /**
     * One line of a section.
     *
     * @param label what is counted
     * @param value the formatted number
     */
    record Line(String label, String value) {
    }

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3_600;

    private StatsOverview() {
    }

    /**
     * Returns the lines of the general section.
     *
     * @param view the year
     * @return the lines
     */
    static List<Line> general(YearStatsView view) {
        return List.of(
                new Line("Rounds played", String.valueOf(view.roundsPlayed())),
                new Line("Wins", view.wins() + " (" + percent(view.winRate()) + ")"),
                new Line("Abandoned", String.valueOf(view.abandoned())),
                new Line("Play time", duration(view.playTimeSeconds())),
                new Line("Average round", duration(view.averagePlayTimeSeconds())));
    }

    /**
     * Returns the lines of the survivor section.
     *
     * @param view the year
     * @return the lines
     */
    static List<Line> survivor(YearStatsView view) {
        return List.of(
                new Line("Rounds", String.valueOf(view.roundsSurvivor())),
                new Line("Wins", view.winsSurvivor() + " (" + percent(view.survivorWinRate()) + ")"),
                new Line("Pages found", view.pagesFound() + " (" + decimal(view.pagesPerSurvivorRound(), 1) + " per round)"),
                new Line("Best round", view.bestPagesRound() + " pages"),
                new Line("Deaths", String.valueOf(view.deaths())),
                new Line("Rounds survived", String.valueOf(view.roundsSurvived())),
                new Line("Average survival", duration(view.averageSurvivedSeconds())));
    }

    /**
     * Returns the lines of the Slender section.
     *
     * @param view the year
     * @return the lines
     */
    static List<Line> slender(YearStatsView view) {
        Long fastest = view.fastestWinSeconds();
        return List.of(
                new Line("Rounds", String.valueOf(view.roundsSlender())),
                new Line("Wins", view.winsSlender() + " (" + percent(view.slenderWinRate()) + ")"),
                new Line("Kills", view.kills() + " (K/D " + decimal(view.killDeathRatio(), 2) + ")"),
                new Line("Kills per round", decimal(view.killsPerSlenderRound(), 1)),
                new Line("Best round", view.bestKillsRound() + " kills"),
                new Line("Fastest win", fastest == null ? "-" : duration(fastest)));
    }

    /**
     * Builds the chat overview: a header, the three sections and a click that opens the menu of the same
     * player and year.
     *
     * @param playerName the name to show and to open the menu for
     * @param view       the year
     * @return the message
     */
    static Component chat(String playerName, YearStatsView view) {
        Component message = Component.text()
                .append(Component.text("---[ ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Statistics of " + playerName + " (" + view.year() + ")", NamedTextColor.GOLD))
                .append(Component.text(" ]---", NamedTextColor.DARK_GRAY))
                .build();
        message = message.append(section("General", general(view)))
                .append(section("Survivor", survivor(view)))
                .append(section("Slender", slender(view)));
        Component open = Component.text("[Open the menu]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Shows the same statistics in a menu", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/stats " + StatsCommand.MENU_LITERAL + " " + playerName + " " + view.year()));
        return message.append(Component.newline()).append(open);
    }

    private static Component section(String title, List<Line> lines) {
        Component section = Component.newline().append(Component.text(title, NamedTextColor.YELLOW));
        for (Line line : lines) {
            section = section.append(Component.newline())
                    .append(Component.text(" " + line.label() + ": ", NamedTextColor.GRAY))
                    .append(Component.text(line.value(), NamedTextColor.GREEN));
        }
        return section;
    }

    /**
     * Formats a share as a percentage with one decimal.
     *
     * @param rate the share, {@code 0.5} for half
     * @return for example {@code 50.0%}
     */
    static String percent(double rate) {
        return decimal(rate * 100.0D, 1) + "%";
    }

    private static String decimal(double value, int digits) {
        return String.format(Locale.ROOT, "%." + digits + "f", value);
    }

    /**
     * Formats seconds in the two largest units, for example {@code 1h 5m}, {@code 2m 30s} or {@code 45s}.
     *
     * @param seconds the duration
     * @return the text
     */
    static String duration(long seconds) {
        if (seconds >= SECONDS_PER_HOUR) {
            return seconds / SECONDS_PER_HOUR + "h " + seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE + "m";
        }
        if (seconds >= SECONDS_PER_MINUTE) {
            return seconds / SECONDS_PER_MINUTE + "m " + seconds % SECONDS_PER_MINUTE + "s";
        }
        return seconds + "s";
    }
}
