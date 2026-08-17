package net.onelitefeather.cygnus.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.onelitefeather.cygnus.stats.repository.StatsRepository;

import java.util.UUID;

/**
 * Represents one player's aggregated, long term statistics. Each instance corresponds to exactly one
 * row in the {@code cygnus_player_stats} table.
 *
 * <p>All writes happen through native upsert SQL in {@link StatsRepository}. Hibernate never loads this
 * entity into a session for dirty checking. The class exists so Hibernate can generate the table schema
 * during startup, and so {@code getProfile} has a typed row to read data into.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.7.0
 */
@Entity
@Table(name = "cygnus_player_stats")
public class PlayerStatsEntity {

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "wins_survivor", nullable = false)
    private int winsSurvivor;

    @Column(name = "wins_slender", nullable = false)
    private int winsSlender;

    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    @Column(name = "deaths", nullable = false)
    private int deaths;

    @Column(name = "pages_found", nullable = false)
    private int pagesFound;

    @Column(name = "playtime_seconds", nullable = false)
    private long playtimeSeconds;

    @Column(name = "current_win_streak", nullable = false)
    private int currentWinStreak;

    @Column(name = "longest_win_streak", nullable = false)
    private int longestWinStreak;

    protected PlayerStatsEntity() {
        // Hibernate
    }

    /**
     * Returns the unique id of the player.
     *
     * @return player id
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the number of rounds won while playing as a survivor.
     *
     * @return the number of survivors wins
     */
    public int getWinsSurvivor() {
        return winsSurvivor;
    }

    /**
     * Returns the number of rounds won while playing as the slender.
     *
     * @return number of slender wins
     */
    public int getWinsSlender() {
        return winsSlender;
    }

    /**
     * Returns the total number of rounds played by the player.
     *
     * @return number of rounds played
     */
    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    /**
     * Returns the number of times the player died.
     *
     * @return number of deaths
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * Returns the number of pages found by the player.
     *
     * @return number of pages found
     */
    public int getPagesFound() {
        return pagesFound;
    }

    /**
     * Returns the total playtime of the player, in seconds.
     *
     * @return playtime in seconds
     */
    public long getPlaytimeSeconds() {
        return playtimeSeconds;
    }

    /**
     * Returns the player's current win streak.
     *
     * @return current win streak
     */
    public int getCurrentWinStreak() {
        return currentWinStreak;
    }

    /**
     * Returns the player's longest win streak.
     *
     * @return longest win streak
     */
    public int getLongestWinStreak() {
        return longestWinStreak;
    }
}
