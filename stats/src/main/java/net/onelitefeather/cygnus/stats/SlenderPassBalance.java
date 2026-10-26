package net.onelitefeather.cygnus.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * The slender pass balance of one player. The {@link Version} column is the optimistic lock two servers
 * writing the same balance rely on.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
@Entity
@Table(name = "player_slender_pass")
public class SlenderPassBalance {

    @Id
    @Column(name = "player_uuid")
    private UUID playerUuid;
    @Column(nullable = false)
    private int balance;
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * For Hibernate.
     */
    protected SlenderPassBalance() {
    }

    /**
     * Creates the balance row of a player with no passes.
     *
     * @param playerUuid the player
     */
    public SlenderPassBalance(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    /**
     * Returns the player.
     *
     * @return the player
     */
    public UUID playerUuid() {
        return this.playerUuid;
    }

    /**
     * Returns the balance.
     *
     * @return the number of passes
     */
    public int balance() {
        return this.balance;
    }

    /**
     * Adds a delta to the balance. The caller keeps the balance from going below zero.
     *
     * @param delta the change
     */
    public void add(int delta) {
        this.balance += delta;
    }
}
