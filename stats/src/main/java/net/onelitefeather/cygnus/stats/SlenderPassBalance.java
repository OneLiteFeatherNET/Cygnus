package net.onelitefeather.cygnus.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.YearMonth;
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
    @Column(name = "rounds_since_slender", nullable = false)
    private int roundsSinceSlender;
    @Convert(converter = YearMonthConverter.class)
    @Column(name = "last_quota_month", length = 7)
    private YearMonth lastQuotaMonth;

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

    /**
     * Returns the Survivor rounds the player has played since the last Slender round or the last earned pass.
     *
     * @return the counter, zero or more
     * @since 2.21.0
     */
    public int roundsSinceSlender() {
        return this.roundsSinceSlender;
    }

    /**
     * Counts one more Survivor round that the player played to the end.
     *
     * @since 2.21.0
     */
    public void countSurvivorRound() {
        this.roundsSinceSlender++;
    }

    /**
     * Sets the Survivor counter back to zero.
     *
     * @since 2.21.0
     */
    public void resetRounds() {
        this.roundsSinceSlender = 0;
    }
}
