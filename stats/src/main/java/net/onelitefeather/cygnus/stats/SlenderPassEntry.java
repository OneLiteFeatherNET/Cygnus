package net.onelitefeather.cygnus.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the append-only slender pass ledger. Rows are written, never changed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
@Entity
@Table(name = "slender_pass_transaction")
public class SlenderPassEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;
    @Column(nullable = false)
    private int delta;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LedgerReason reason;
    @Column(name = "round_id")
    private UUID roundId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * For Hibernate.
     */
    protected SlenderPassEntry() {
    }

    /**
     * Creates a ledger row.
     *
     * @param playerUuid the player
     * @param delta      the change that was written
     * @param reason     why it changed
     * @param roundId    the round for a redemption, {@code null} otherwise
     * @param createdAt  when the row is written
     */
    public SlenderPassEntry(UUID playerUuid, int delta, LedgerReason reason, UUID roundId, Instant createdAt) {
        this.playerUuid = playerUuid;
        this.delta = delta;
        this.reason = reason;
        this.roundId = roundId;
        this.createdAt = createdAt;
    }

    /**
     * Returns the generated id of the row, {@code null} until it is written.
     *
     * @return the id
     */
    public Long id() {
        return this.id;
    }

    /**
     * Returns the change that was written.
     *
     * @return the delta
     */
    public int delta() {
        return this.delta;
    }

    /**
     * Returns the round the row belongs to.
     *
     * @return the round id, {@code null} for an administrator change
     */
    public UUID roundId() {
        return this.roundId;
    }

    /**
     * Returns when the row was written.
     *
     * @return the time from the clock
     */
    public Instant createdAt() {
        return this.createdAt;
    }

    /**
     * Returns the reason of the row.
     *
     * @return the reason
     */
    public LedgerReason reason() {
        return this.reason;
    }
}
