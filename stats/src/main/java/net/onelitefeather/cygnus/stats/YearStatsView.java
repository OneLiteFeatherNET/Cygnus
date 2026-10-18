package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An immutable copy of a player's {@link PlayerYearStats} row, safe to hand to other threads. The
 * derived values (rates, ratios, averages) are computed here when they are read, never stored, and
 * every one of them is {@code 0} instead of a division by zero when there is nothing to divide by.
 *
 * @param playerUuid             the player
 * @param year                   the calendar year
 * @param roundsPlayed           rounds played, abandoned ones included
 * @param roundsSlender          rounds started as the Slender
 * @param roundsSurvivor         rounds started as a survivor
 * @param winsSlender            rounds won as the Slender
 * @param winsSurvivor           rounds won as a survivor
 * @param lossesSlender          rounds lost as the Slender
 * @param lossesSurvivor         rounds lost as a survivor
 * @param abandoned              rounds the player left before they ended
 * @param playTimeSeconds        seconds played
 * @param pagesFound             pages found
 * @param bestPagesRound         most pages found in one round
 * @param roundsSurvived         rounds a survivor was still alive at the end
 * @param deaths                 deaths
 * @param timeSurvivedSeconds    seconds lived as a survivor
 * @param longestSurvivedSeconds longest life in one round, in seconds
 * @param allPagesContributions  winning rounds in which the player found a page and the last one was found
 * @param lastPagesFound         last pages found, the ones that decided a round
 * @param damageTaken            vitality points lost to hits
 * @param painkillersUsed        painkillers taken
 * @param kills                  kills as the Slender
 * @param bestKillsRound         most kills in one round
 * @param fastestWinSeconds      fastest win as the Slender in seconds, {@code null} without one
 * @param pagesAgainst           pages the survivors found in the player's rounds as the Slender
 * @param takeovers              times the player took over the Slender role
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public record YearStatsView(
        UUID playerUuid,
        int year,
        int roundsPlayed,
        int roundsSlender,
        int roundsSurvivor,
        int winsSlender,
        int winsSurvivor,
        int lossesSlender,
        int lossesSurvivor,
        int abandoned,
        long playTimeSeconds,
        int pagesFound,
        int bestPagesRound,
        int roundsSurvived,
        int deaths,
        long timeSurvivedSeconds,
        long longestSurvivedSeconds,
        int allPagesContributions,
        int lastPagesFound,
        double damageTaken,
        int painkillersUsed,
        int kills,
        int bestKillsRound,
        @Nullable Long fastestWinSeconds,
        int pagesAgainst,
        int takeovers
) {

    /**
     * Returns a year in which the player has no rounds yet.
     *
     * @param playerUuid the player
     * @param year       the calendar year
     * @return a view with every counter at zero
     */
    public static YearStatsView empty(UUID playerUuid, int year) {
        return new YearStatsView(playerUuid, year, 0, 0, 0, 0, 0, 0, 0, 0, 0L, 0, 0, 0, 0, 0L, 0L, 0, 0, 0.0D, 0, 0, 0,
                null, 0, 0);
    }

    /**
     * Returns the rounds won on either side.
     *
     * @return wins as the Slender plus wins as a survivor
     */
    public int wins() {
        return this.winsSlender + this.winsSurvivor;
    }

    /**
     * Returns the share of rounds that were won.
     *
     * @return wins over all rounds played, {@code 0} without rounds
     */
    public double winRate() {
        return ratio(wins(), this.roundsPlayed);
    }

    /**
     * Returns the share of survivor rounds that were won.
     *
     * @return survivor wins over survivor rounds, {@code 0} without survivor rounds
     */
    public double survivorWinRate() {
        return ratio(this.winsSurvivor, this.roundsSurvivor);
    }

    /**
     * Returns the share of Slender rounds that were won.
     *
     * @return Slender wins over Slender rounds, {@code 0} without Slender rounds
     */
    public double slenderWinRate() {
        return ratio(this.winsSlender, this.roundsSlender);
    }

    /**
     * Returns kills per death. Without a death the ratio is the kill count itself, as it is usually shown.
     *
     * @return kills over deaths, kills when there are no deaths
     */
    public double killDeathRatio() {
        return this.deaths == 0 ? this.kills : (double) this.kills / this.deaths;
    }

    /**
     * Returns the pages a survivor finds in an average survivor round.
     *
     * @return pages over survivor rounds, {@code 0} without survivor rounds
     */
    public double pagesPerSurvivorRound() {
        return ratio(this.pagesFound, this.roundsSurvivor);
    }

    /**
     * Returns the kills in an average Slender round.
     *
     * @return kills over Slender rounds, {@code 0} without Slender rounds
     */
    public double killsPerSlenderRound() {
        return ratio(this.kills, this.roundsSlender);
    }

    /**
     * Returns how long a survivor lives in an average survivor round.
     *
     * @return seconds survived over survivor rounds, {@code 0} without survivor rounds
     */
    public long averageSurvivedSeconds() {
        return this.roundsSurvivor == 0 ? 0L : this.timeSurvivedSeconds / this.roundsSurvivor;
    }

    /**
     * Returns how long an average round takes for the player.
     *
     * @return seconds played over rounds played, {@code 0} without rounds
     */
    public long averagePlayTimeSeconds() {
        return this.roundsPlayed == 0 ? 0L : this.playTimeSeconds / this.roundsPlayed;
    }

    private static double ratio(int part, int whole) {
        return whole == 0 ? 0.0D : (double) part / whole;
    }
}
