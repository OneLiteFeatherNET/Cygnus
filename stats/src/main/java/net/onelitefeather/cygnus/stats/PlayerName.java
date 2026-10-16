package net.onelitefeather.cygnus.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * The last name a player was seen with, so a player who is offline can be found by name. Minecraft
 * names are public; the row is deleted together with the player's statistics.
 * <p>
 * The lower-case copy is what lookups compare against. A name can pass to another player after a
 * rename, so a lookup takes the most recently updated row.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
@Entity
@Table(name = "player_names")
public class PlayerName {

    /** The widest name Minecraft allows and the column holds. */
    public static final int MAX_LENGTH = 16;

    @Id
    @Column(name = "player_uuid")
    private UUID playerUuid;
    @Column(nullable = false, length = MAX_LENGTH)
    private String name;
    @Column(name = "name_lower", nullable = false, length = MAX_LENGTH)
    private String nameLower;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * For Hibernate.
     */
    protected PlayerName() {
    }

    /**
     * Creates the row of a player.
     *
     * @param playerUuid the player
     * @param name       the name, cut to {@value #MAX_LENGTH} characters
     * @param updatedAt  when the name was seen
     */
    public PlayerName(UUID playerUuid, String name, Instant updatedAt) {
        this.playerUuid = playerUuid;
        rename(name, updatedAt);
    }

    /**
     * Takes a name seen at the given time, unless the stored one is newer: a round that another server
     * writes late must not bring an old name back.
     *
     * @param name      the name, cut to {@value #MAX_LENGTH} characters
     * @param updatedAt when the name was seen
     */
    public void rename(String name, Instant updatedAt) {
        if (this.updatedAt != null && updatedAt.isBefore(this.updatedAt)) {
            return;
        }
        this.name = name.length() > MAX_LENGTH ? name.substring(0, MAX_LENGTH) : name;
        this.nameLower = this.name.toLowerCase(Locale.ROOT);
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the player and the name as the statistics know them.
     *
     * @return the known player
     */
    public KnownPlayer toKnownPlayer() {
        return new KnownPlayer(this.playerUuid, this.name);
    }
}
