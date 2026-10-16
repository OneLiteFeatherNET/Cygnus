package net.onelitefeather.cygnus.stats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks the values derived from the stored counters, most of all that an empty year divides by nothing.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class YearStatsViewTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final double DELTA = 1.0E-9;

    @Test
    @DisplayName("An empty year has no rounds and every derived value is zero")
    void emptyYearDividesByNothing() {
        YearStatsView empty = YearStatsView.empty(PLAYER, 2026);

        assertEquals(0, empty.roundsPlayed(), "no rounds");
        assertEquals(0.0, empty.winRate(), DELTA, "no rounds, no win rate");
        assertEquals(0.0, empty.survivorWinRate(), DELTA, "no survivor rounds");
        assertEquals(0.0, empty.slenderWinRate(), DELTA, "no slender rounds");
        assertEquals(0.0, empty.killDeathRatio(), DELTA, "no kills and no deaths");
        assertEquals(0.0, empty.pagesPerSurvivorRound(), DELTA, "no survivor rounds");
        assertEquals(0.0, empty.killsPerSlenderRound(), DELTA, "no slender rounds");
        assertEquals(0L, empty.averageSurvivedSeconds(), "no survivor rounds");
        assertEquals(0L, empty.averagePlayTimeSeconds(), "no rounds");
        assertNull(empty.fastestWinSeconds(), "no win");
    }

    @Test
    @DisplayName("The win rate is wins over all rounds, abandoned ones included")
    void winRateCountsAbandonedRounds() {
        YearStatsView view = YearStatsViewBuilder.of().rounds(10).survivorRounds(6).slenderRounds(4)
                .wins(3, 2).abandoned(1).build();

        assertEquals(5, view.wins(), "three survivor and two slender wins");
        assertEquals(0.5, view.winRate(), DELTA, "five wins in ten rounds");
    }

    @Test
    @DisplayName("Each side's win rate is measured against the rounds played on that side")
    void sideWinRates() {
        YearStatsView view = YearStatsViewBuilder.of().rounds(10).survivorRounds(8).slenderRounds(2)
                .wins(4, 1).build();

        assertEquals(0.5, view.survivorWinRate(), DELTA, "four of eight survivor rounds");
        assertEquals(0.5, view.slenderWinRate(), DELTA, "one of two slender rounds");
    }

    @Test
    @DisplayName("Without a death the kill/death ratio is the kill count")
    void killDeathRatioWithoutDeaths() {
        YearStatsView view = YearStatsViewBuilder.of().kills(7).deaths(0).build();

        assertEquals(7.0, view.killDeathRatio(), DELTA, "seven kills over no death");
    }

    @Test
    @DisplayName("The kill/death ratio divides kills by deaths")
    void killDeathRatio() {
        YearStatsView view = YearStatsViewBuilder.of().kills(9).deaths(4).build();

        assertEquals(2.25, view.killDeathRatio(), DELTA, "nine kills over four deaths");
    }

    @Test
    @DisplayName("Averages are per round of the side they belong to")
    void averages() {
        YearStatsView view = YearStatsViewBuilder.of().rounds(5).survivorRounds(4).slenderRounds(1)
                .pages(10).kills(3).survived(400L).playTime(1000L).build();

        assertEquals(2.5, view.pagesPerSurvivorRound(), DELTA, "ten pages in four survivor rounds");
        assertEquals(3.0, view.killsPerSlenderRound(), DELTA, "three kills in one slender round");
        assertEquals(100L, view.averageSurvivedSeconds(), "400 seconds over four survivor rounds");
        assertEquals(200L, view.averagePlayTimeSeconds(), "1000 seconds over five rounds");
    }
}
