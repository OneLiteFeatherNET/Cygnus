package net.onelitefeather.cygnus.stats;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * What one participant did in one finished round. Immutable; the {@link Builder} collects it while the
 * round runs.
 *
 * @param playerUuid         the participant
 * @param role               the role they started the round in
 * @param outcome            how the round ended for them
 * @param finishReason       why the round ended
 * @param pagesFound         pages this participant found
 * @param kills              kills credited to this participant
 * @param died               whether the participant died
 * @param survived           whether a survivor was still alive when the round ended
 * @param finisher           whether this participant found the last page
 * @param playTimeSeconds    seconds from the round start until the round end or the quit
 * @param timeSurvivedSeconds seconds a survivor lived: until the death, the quit or the round end; {@code 0} for the Slender
 * @param damageTaken        vitality points lost to hits
 * @param painkillersUsed    painkillers taken
 * @param pagesAgainst       pages the survivors found in the round, counted for the Slender only, {@code 0} otherwise
 * @param participants       tracked participants of the round (Slender and Survivors at the start), the same on every row of the round
 * @param takenOver          whether this survivor became the Slender after the original one left; {@code role} stays the start role
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record RoundStats(
        UUID playerUuid,
        Role role,
        Outcome outcome,
        FinishReason finishReason,
        int pagesFound,
        int kills,
        boolean died,
        boolean survived,
        boolean finisher,
        long playTimeSeconds,
        long timeSurvivedSeconds,
        double damageTaken,
        int painkillersUsed,
        int pagesAgainst,
        int participants,
        boolean takenOver
) {

    /**
     * The role a participant started the round in.
     */
    public enum Role {
        SLENDER,
        SURVIVOR
    }

    /**
     * How a round ended for a participant.
     */
    public enum Outcome {
        WIN,
        LOSS,
        ABANDONED
    }

    /**
     * Tells which side wins for a finish reason: survivors win on {@code TIME_OVER}, {@code ALL_PAGES_FOUND}
     * and {@code SLENDER_LEFT}, the Slender on {@code ALL_SURVIVOR_DEAD} and {@code SURVIVOR_LEFT}.
     *
     * @param reason why the round ended
     * @param role   a side
     * @return {@code true} if that side won
     */
    public static boolean wins(FinishReason reason, Role role) {
        boolean slenderWon = switch (reason) {
            case ALL_SURVIVOR_DEAD, SURVIVOR_LEFT -> true;
            case TIME_OVER, ALL_PAGES_FOUND, SLENDER_LEFT -> false;
        };
        return (role == Role.SLENDER) == slenderWon;
    }

    /**
     * Collects the numbers of one participant while the round runs. Not thread-safe: the collector
     * guards it.
     */
    public static final class Builder {

        private final UUID playerUuid;
        private final Role role;
        private final Instant started;
        private int pagesFound;
        private int kills;
        private boolean finisher;
        private boolean takenOver;
        private double damageTaken;
        private int painkillersUsed;
        private @Nullable Instant died;
        private @Nullable Instant left;

        /**
         * Starts tracking a participant.
         *
         * @param playerUuid the participant
         * @param role       the role they start in
         * @param started    when the round started
         */
        public Builder(UUID playerUuid, Role role, Instant started) {
            this.playerUuid = playerUuid;
            this.role = role;
            this.started = started;
        }

        /**
         * Returns the participant's role.
         *
         * @return the role
         */
        public Role role() {
            return this.role;
        }

        /**
         * Returns the pages found so far.
         *
         * @return the page count
         */
        public int pagesFound() {
            return this.pagesFound;
        }

        /**
         * Tells whether the participant already left, so it counts nothing more.
         *
         * @return {@code true} if the participant is out
         */
        public boolean frozen() {
            return this.left != null;
        }

        /**
         * Counts a found page.
         *
         * @param last whether it was the page that completed the round
         */
        public void pageFound(boolean last) {
            this.pagesFound++;
            this.finisher |= last;
        }

        /**
         * Marks that this survivor became the Slender after the original one left.
         */
        public void takenOver() {
            this.takenOver = true;
        }

        /**
         * Counts a kill credit.
         */
        public void kill() {
            this.kills++;
        }

        /**
         * Marks the death. Only the first one counts.
         *
         * @param at when it happened
         */
        public void died(Instant at) {
            if (this.died == null) {
                this.died = at;
            }
        }

        /**
         * Adds damage.
         *
         * @param points the vitality points lost
         */
        public void damage(double points) {
            this.damageTaken += points;
        }

        /**
         * Counts a painkiller.
         */
        public void painkiller() {
            this.painkillersUsed++;
        }

        /**
         * Marks the quit; it ends the play time and makes the outcome {@link Outcome#ABANDONED}.
         *
         * @param at when the participant left
         */
        public void left(Instant at) {
            if (this.left == null) {
                this.left = at;
            }
        }

        /**
         * Builds the snapshot.
         *
         * @param reason       why the round ended
         * @param finished     when it ended
         * @param pagesAgainst pages all survivors found, kept for the Slender
         * @param participants how many participants the round had at its start
         * @return the snapshot
         */
        public RoundStats build(FinishReason reason, Instant finished, int pagesAgainst, int participants) {
            Instant end = this.left != null ? this.left : finished;
            Outcome outcome = this.left != null ? Outcome.ABANDONED
                    : wins(reason, this.role) ? Outcome.WIN : Outcome.LOSS;
            boolean survivor = this.role == Role.SURVIVOR;
            Instant lifeEnd = this.died != null ? this.died : end;
            return new RoundStats(
                    this.playerUuid,
                    this.role,
                    outcome,
                    reason,
                    this.pagesFound,
                    this.kills,
                    this.died != null,
                    survivor && this.died == null && this.left == null,
                    this.finisher,
                    seconds(this.started, end),
                    survivor ? seconds(this.started, lifeEnd) : 0L,
                    this.damageTaken,
                    this.painkillersUsed,
                    this.role == Role.SLENDER ? pagesAgainst : 0,
                    participants,
                    this.takenOver);
        }

        private static long seconds(Instant from, Instant to) {
            return Math.max(0L, Duration.between(from, to).toSeconds());
        }
    }
}
