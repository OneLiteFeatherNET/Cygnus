package net.onelitefeather.cygnus.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

/**
 * A player's numbers for one calendar year, summed from their rounds. Several servers may update the
 * same row, so it carries a {@link Version}: a lost race surfaces as an optimistic lock failure and
 * the repository retries.
 * <p>
 * Counters are added, {@code best*} and {@code longest*} keep the maximum, the fastest win the
 * minimum. Derived values such as the win rate are computed when read, never stored.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
@Entity
@Table(name = "player_year_stats")
public class PlayerYearStats {

    /**
     * The key of a year row: the player and the year.
     *
     * @param playerUuid the player
     * @param year       the calendar year
     */
    @Embeddable
    public record Key(@Column(name = "player_uuid") UUID playerUuid, @Column(name = "stats_year") int year)
            implements Serializable {
    }

    @EmbeddedId
    private Key id;
    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;
    @Column(name = "rounds_slender", nullable = false)
    private int roundsSlender;
    @Column(name = "rounds_survivor", nullable = false)
    private int roundsSurvivor;
    @Column(name = "wins_slender", nullable = false)
    private int winsSlender;
    @Column(name = "wins_survivor", nullable = false)
    private int winsSurvivor;
    @Column(name = "losses_slender", nullable = false)
    private int lossesSlender;
    @Column(name = "losses_survivor", nullable = false)
    private int lossesSurvivor;
    @Column(nullable = false)
    private int abandoned;
    @Column(name = "play_time_seconds", nullable = false)
    private long playTimeSeconds;
    @Column(name = "wins_time_over", nullable = false)
    private int winsTimeOver;
    @Column(name = "wins_all_pages_found", nullable = false)
    private int winsAllPagesFound;
    @Column(name = "wins_all_survivor_dead", nullable = false)
    private int winsAllSurvivorDead;
    @Column(name = "wins_slender_left", nullable = false)
    private int winsSlenderLeft;
    @Column(name = "wins_survivor_left", nullable = false)
    private int winsSurvivorLeft;
    @Column(name = "pages_found", nullable = false)
    private int pagesFound;
    @Column(name = "best_pages_round", nullable = false)
    private int bestPagesRound;
    @Column(name = "rounds_survived", nullable = false)
    private int roundsSurvived;
    @Column(nullable = false)
    private int deaths;
    @Column(name = "time_survived_seconds", nullable = false)
    private long timeSurvivedSeconds;
    @Column(name = "longest_survived_seconds", nullable = false)
    private long longestSurvivedSeconds;
    @Column(name = "all_pages_contributions", nullable = false)
    private int allPagesContributions;
    @Column(name = "last_pages_found", nullable = false)
    private int lastPagesFound;
    @Column(name = "damage_taken", nullable = false)
    private double damageTaken;
    @Column(name = "painkillers_used", nullable = false)
    private int painkillersUsed;
    @Column(nullable = false)
    private int kills;
    @Column(name = "best_kills_round", nullable = false)
    private int bestKillsRound;
    @Column(name = "fastest_win_seconds")
    private @Nullable Long fastestWinSeconds;
    @Column(name = "pages_against", nullable = false)
    private int pagesAgainst;
    @Column(nullable = false)
    private int takeovers;
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * For Hibernate.
     */
    protected PlayerYearStats() {
    }

    /**
     * Creates an empty aggregate.
     *
     * @param id the player and the year
     */
    public PlayerYearStats(Key id) {
        this.id = id;
    }

    /**
     * Adds one round to the aggregate.
     *
     * @param stats the participant's numbers of the round
     */
    public void merge(RoundStats stats) {
        boolean slender = stats.role() == RoundStats.Role.SLENDER;
        this.roundsPlayed++;
        if (slender) {
            this.roundsSlender++;
        } else {
            this.roundsSurvivor++;
        }
        mergeOutcome(stats, slender);
        this.playTimeSeconds += stats.playTimeSeconds();
        this.pagesFound += stats.pagesFound();
        this.bestPagesRound = Math.max(this.bestPagesRound, stats.pagesFound());
        if (stats.survived()) {
            this.roundsSurvived++;
        }
        if (stats.died()) {
            this.deaths++;
        }
        this.timeSurvivedSeconds += stats.timeSurvivedSeconds();
        this.longestSurvivedSeconds = Math.max(this.longestSurvivedSeconds, stats.timeSurvivedSeconds());
        if (stats.finisher()) {
            this.lastPagesFound++;
        }
        this.damageTaken += stats.damageTaken();
        this.painkillersUsed += stats.painkillersUsed();
        this.kills += stats.kills();
        this.bestKillsRound = Math.max(this.bestKillsRound, stats.kills());
        this.pagesAgainst += stats.pagesAgainst();
        if (stats.takenOver()) {
            this.takeovers++;
        }
    }

    /**
     * Copies the row into an immutable view.
     *
     * @return the view of this row
     */
    public YearStatsView toView() {
        return new YearStatsView(this.id.playerUuid(), this.id.year(), this.roundsPlayed, this.roundsSlender,
                this.roundsSurvivor, this.winsSlender, this.winsSurvivor, this.lossesSlender, this.lossesSurvivor,
                this.abandoned, this.playTimeSeconds, this.pagesFound, this.bestPagesRound, this.roundsSurvived,
                this.deaths, this.timeSurvivedSeconds, this.longestSurvivedSeconds, this.allPagesContributions,
                this.lastPagesFound, this.damageTaken, this.painkillersUsed, this.kills, this.bestKillsRound,
                this.fastestWinSeconds, this.pagesAgainst, this.takeovers);
    }

    private void mergeOutcome(RoundStats stats, boolean slender) {
        switch (stats.outcome()) {
            case ABANDONED -> this.abandoned++;
            case LOSS -> {
                if (slender) {
                    this.lossesSlender++;
                } else {
                    this.lossesSurvivor++;
                }
            }
            case WIN -> {
                if (slender) {
                    this.winsSlender++;
                    this.fastestWinSeconds = this.fastestWinSeconds == null
                            ? stats.playTimeSeconds()
                            : Math.min(this.fastestWinSeconds, stats.playTimeSeconds());
                } else {
                    this.winsSurvivor++;
                    if (stats.finishReason() == FinishReason.ALL_PAGES_FOUND && stats.pagesFound() > 0) {
                        this.allPagesContributions++;
                    }
                }
                mergeWinReason(stats.finishReason());
            }
        }
    }

    private void mergeWinReason(FinishReason reason) {
        switch (reason) {
            case TIME_OVER -> this.winsTimeOver++;
            case ALL_PAGES_FOUND -> this.winsAllPagesFound++;
            case ALL_SURVIVOR_DEAD -> this.winsAllSurvivorDead++;
            case SLENDER_LEFT -> this.winsSlenderLeft++;
            case SURVIVOR_LEFT -> this.winsSurvivorLeft++;
        }
    }
}
