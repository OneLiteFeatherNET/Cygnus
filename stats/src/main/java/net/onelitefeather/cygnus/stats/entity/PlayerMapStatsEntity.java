package net.onelitefeather.cygnus.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import net.onelitefeather.cygnus.stats.repository.StatsRepository;

/**
 * Represents how often, and how successfully, one player has played on one specific map. Each instance
 * corresponds to exactly one row in the {@code cygnus_player_map_stats} table, keyed by player and map
 * name. These per map counts are what a "favorite map" is derived from.
 *
 * <p>All writes happen through native upsert SQL in {@link StatsRepository}.
 *
 * <p>The {@code player} field links this row back to that player's aggregated stats using a derived
 * identity association ({@code @MapsId}). The {@code player_id} column serves two purposes at once. It
 * is part of the composite id in {@link PlayerMapStatsId}, and it is the foreign key to
 * {@link PlayerStatsEntity}. Because of this, Hibernate's schema generation creates a real foreign key
 * constraint between the two tables instead of leaving them connected only by convention.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.7.0
 */
@Entity
@Table(name = "cygnus_player_map_stats")
public class PlayerMapStatsEntity {

    @EmbeddedId
    private PlayerMapStatsId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private PlayerStatsEntity player;

    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    @Column(name = "wins", nullable = false)
    private int wins;

    protected PlayerMapStatsEntity() {
        // Hibernate
    }

    /**
     * Returns the composite primary key of this stats entry.
     *
     * @return composite primary key
     */
    public PlayerMapStatsId getId() {
        return id;
    }

    /**
     * Returns the player this stats entry belongs to.
     *
     * @return player
     */
    public PlayerStatsEntity getPlayer() {
        return player;
    }

    /**
     * Returns the number of rounds played by a player on a map
     *
     * @return number of rounds played
     */
    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    /**
     * Return the number of wins that a player has
     *
     * @return number of wins
     */
    public int getWins() {
        return wins;
    }
}
