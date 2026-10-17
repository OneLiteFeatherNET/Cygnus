package net.onelitefeather.cygnus.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * One participant's numbers for one finished round: the source of truth the yearly aggregate can be
 * rebuilt from. Written once, never updated.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
@Entity
@Table(name = "player_round_stats")
public class PlayerRoundStats {

    /**
     * The key of a round row: the round and the player.
     *
     * @param roundId    the round
     * @param playerUuid the player
     */
    @Embeddable
    public record Key(@Column(name = "round_id") UUID roundId, @Column(name = "player_uuid") UUID playerUuid)
            implements Serializable {
    }

    @EmbeddedId
    private Key id;
    @Column(name = "stats_year", nullable = false)
    private int year;
    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStats.Role role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStats.Outcome outcome;
    @Enumerated(EnumType.STRING)
    @Column(name = "finish_reason", nullable = false)
    private FinishReason finishReason;
    @Column(name = "pages_found", nullable = false)
    private int pagesFound;
    @Column(nullable = false)
    private int kills;
    @Column(nullable = false)
    private boolean died;
    @Column(nullable = false)
    private boolean survived;
    @Column(nullable = false)
    private boolean finisher;
    @Column(name = "play_time_seconds", nullable = false)
    private long playTimeSeconds;
    @Column(name = "time_survived_seconds", nullable = false)
    private long timeSurvivedSeconds;
    @Column(name = "damage_taken", nullable = false)
    private double damageTaken;
    @Column(name = "painkillers_used", nullable = false)
    private int painkillersUsed;
    @Column(name = "pages_against", nullable = false)
    private int pagesAgainst;
    @Column(nullable = false)
    private int participants;
    @Column(name = "taken_over", nullable = false)
    private boolean takenOver;

    /**
     * For Hibernate.
     */
    protected PlayerRoundStats() {
    }

    /**
     * Creates the row for one participant.
     *
     * @param round the finished round
     * @param stats the participant's numbers
     */
    public PlayerRoundStats(FinishedRound round, RoundStats stats) {
        this.id = new Key(round.roundId(), stats.playerUuid());
        this.year = round.year();
        this.finishedAt = round.finishedAt();
        this.role = stats.role();
        this.outcome = stats.outcome();
        this.finishReason = stats.finishReason();
        this.pagesFound = stats.pagesFound();
        this.kills = stats.kills();
        this.died = stats.died();
        this.survived = stats.survived();
        this.finisher = stats.finisher();
        this.playTimeSeconds = stats.playTimeSeconds();
        this.timeSurvivedSeconds = stats.timeSurvivedSeconds();
        this.damageTaken = stats.damageTaken();
        this.painkillersUsed = stats.painkillersUsed();
        this.pagesAgainst = stats.pagesAgainst();
        this.participants = stats.participants();
        this.takenOver = stats.takenOver();
    }
}
