package net.onelitefeather.cygnus.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identifies exactly one row in the {@code cygnus_player_map_stats} table. A player has one such row
 * for every map they have played, so the primary key needs both the player and the map to be unique.
 * This class combines those two values, the player's id and the map's name, into a single embeddable
 * id used by {@link PlayerMapStatsEntity}.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.7.0
 */
@Embeddable
public class PlayerMapStatsId implements Serializable {

    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "map_name")
    private String mapName;

    protected PlayerMapStatsId() {
        // Hibernate
    }

    /**
     * Creates a new instance of the {@link PlayerMapStatsId} class.
     *
     * @param playerId id of the player
     * @param mapName  name of the map
     */
    public PlayerMapStatsId(UUID playerId, String mapName) {
        this.playerId = playerId;
        this.mapName = mapName;
    }

    /**
     * Returns the id of the player.
     *
     * @return player id
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the name of the map.
     *
     * @return map name
     */
    public String getMapName() {
        return mapName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerMapStatsId that)) return false;
        return Objects.equals(playerId, that.playerId) && Objects.equals(mapName, that.mapName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, mapName);
    }
}
