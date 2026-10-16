package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.onelitefeather.cygnus.stats.YearStatsView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the chat overview and the lines of the menu items: the stored counters, the values derived
 * when they are read, an empty year, and the click that opens the menu.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class StatsOverviewTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    /** Twelve rounds: eight as survivor (four wins), four as Slender (two wins), one abandoned. */
    private static YearStatsView busyYear() {
        return new YearStatsView(PLAYER, 2026, 12, 4, 8, 2, 4, 1, 3, 1, 3_900L, 12, 5, 3, 4, 1_200L, 600L, 2, 1, 55.0D, 3,
                9, 4, 480L, 14, 1);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static List<ClickEvent> clicks(Component component) {
        List<ClickEvent> found = new ArrayList<>();
        collect(component, found);
        return found;
    }

    private static void collect(Component component, List<ClickEvent> found) {
        if (component.clickEvent() != null) {
            found.add(component.clickEvent());
        }
        component.children().forEach(child -> collect(child, found));
    }

    @Test
    @DisplayName("The overview names the player and the year")
    void overviewNamesPlayerAndYear() {
        String text = plain(StatsOverview.chat("Alice", busyYear()));

        assertTrue(text.contains("Alice"), "the player's name belongs in the header: " + text);
        assertTrue(text.contains("2026"), "the year belongs in the header: " + text);
    }

    @Test
    @DisplayName("The overview has a general, a survivor and a slender section")
    void overviewHasThreeSections() {
        String text = plain(StatsOverview.chat("Alice", busyYear()));

        assertTrue(text.contains("General"), text);
        assertTrue(text.contains("Survivor"), text);
        assertTrue(text.contains("Slender"), text);
    }

    @Test
    @DisplayName("The general section shows rounds, wins and the win rate")
    void generalSection() {
        List<StatsOverview.Line> lines = StatsOverview.general(busyYear());

        assertTrue(lines.contains(new StatsOverview.Line("Rounds played", "12")), lines.toString());
        assertTrue(lines.contains(new StatsOverview.Line("Wins", "6 (50.0%)")), "six wins in twelve rounds: " + lines);
        assertTrue(lines.contains(new StatsOverview.Line("Abandoned", "1")), lines.toString());
        assertTrue(lines.contains(new StatsOverview.Line("Play time", "1h 5m")), "3900 seconds: " + lines);
    }

    @Test
    @DisplayName("The survivor section shows the derived rate and average")
    void survivorSection() {
        List<StatsOverview.Line> lines = StatsOverview.survivor(busyYear());

        assertTrue(lines.contains(new StatsOverview.Line("Wins", "4 (50.0%)")), "four wins in eight rounds: " + lines);
        assertTrue(lines.contains(new StatsOverview.Line("Pages found", "12 (1.5 per round)")), lines.toString());
        assertTrue(lines.contains(new StatsOverview.Line("Average survival", "2m 30s")), "1200 seconds over eight rounds: " + lines);
        assertTrue(lines.contains(new StatsOverview.Line("Deaths", "4")), lines.toString());
    }

    @Test
    @DisplayName("The slender section shows the kill/death ratio and the fastest win")
    void slenderSection() {
        List<StatsOverview.Line> lines = StatsOverview.slender(busyYear());

        assertTrue(lines.contains(new StatsOverview.Line("Kills", "9 (K/D 2.25)")), "nine kills over four deaths: " + lines);
        assertTrue(lines.contains(new StatsOverview.Line("Kills per round", "2.3")), "nine kills in four rounds: " + lines);
        assertTrue(lines.contains(new StatsOverview.Line("Fastest win", "8m 0s")), "480 seconds: " + lines);
    }

    @Test
    @DisplayName("An empty year shows zeros and dashes instead of dividing by zero")
    void emptyYearDividesByNothing() {
        YearStatsView empty = YearStatsView.empty(PLAYER, 2026);

        String text = assertDoesNotThrow(() -> plain(StatsOverview.chat("Alice", empty)),
                "an empty year must be formattable");

        assertTrue(text.contains("0.0%"), "no rounds, no win rate: " + text);
        assertTrue(StatsOverview.slender(empty).contains(new StatsOverview.Line("Fastest win", "-")),
                "no win, no fastest win");
        assertTrue(StatsOverview.slender(empty).contains(new StatsOverview.Line("Kills", "0 (K/D 0.00)")));
    }

    @Test
    @DisplayName("The overview carries one click that opens the menu of the same player and year")
    void overviewOpensTheMenuOnClick() {
        List<ClickEvent> clicks = clicks(StatsOverview.chat("Alice", busyYear()));

        assertEquals(1, clicks.size(), "exactly one clickable part");
        assertEquals(ClickEvent.Action.RUN_COMMAND, clicks.getFirst().action());
        assertEquals("/stats open-menu Alice 2026", ((ClickEvent.Payload.Text) clicks.getFirst().payload()).value());
    }

    @Test
    @DisplayName("Durations are shown in the two largest units")
    void durations() {
        assertEquals("0s", StatsOverview.duration(0), "zero");
        assertEquals("59s", StatsOverview.duration(59), "seconds only");
        assertEquals("1m 5s", StatsOverview.duration(65), "minutes and seconds");
        assertEquals("1h 0m", StatsOverview.duration(3_600), "hours and minutes");
    }
}
