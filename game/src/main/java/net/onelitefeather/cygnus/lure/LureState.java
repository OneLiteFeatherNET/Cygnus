package net.onelitefeather.cygnus.lure;

import org.jetbrains.annotations.Nullable;

/**
 * One survivor's lure in the current round.
 * <p>
 * It starts idle and is rolled for now and then while the survivor is alone. Once the figure
 * stands, it waits for the survivor to look at it. A figure nobody noticed goes again and the lure
 * is idle once more. A figure that struck uses up the lure for the round, and its follow-up comes
 * once the phantom has dissolved.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
final class LureState {

    /**
     * Where the lure stands.
     */
    enum Phase {
        /** Waiting for the next roll. */
        IDLE,
        /** The figure stands and waits to be looked at. */
        WAITING,
        /** The figure has struck. Nothing more happens this round. */
        USED
    }

    private Phase phase = Phase.IDLE;
    private long nextRollAt;
    private @Nullable LureFigure figure;
    private long waitUntil;
    private long seenSince = -1L;
    private long followUpAt = -1L;

    Phase phase() {
        return this.phase;
    }

    /**
     * Tells whether the survivor is due for a roll, and if so, sets the next one.
     *
     * @param now   the current time in milliseconds
     * @param every how long until the next roll, in milliseconds
     * @return {@code true} if a roll is due now
     */
    boolean rollDue(long now, long every) {
        if (now < this.nextRollAt) return false;
        this.nextRollAt = now + every;
        return true;
    }

    @Nullable LureFigure figure() {
        return this.figure;
    }

    void waiting(LureFigure figure, long until) {
        this.phase = Phase.WAITING;
        this.figure = figure;
        this.waitUntil = until;
        this.seenSince = -1L;
    }

    boolean waitedOut(long now) {
        return now >= this.waitUntil;
    }

    /**
     * Notes whether the survivor looks at the figure in this step.
     *
     * @param looking whether they look at it
     * @param now     the current time in milliseconds
     * @return how long they have looked at it without a break, in milliseconds, or -1 if they do not
     */
    long gaze(boolean looking, long now) {
        if (!looking) {
            this.seenSince = -1L;
            return -1L;
        }
        if (this.seenSince < 0) this.seenSince = now;
        return now - this.seenSince;
    }

    /**
     * Takes the figure away again without the lure being used up.
     */
    void idle() {
        this.phase = Phase.IDLE;
        this.figure = null;
        this.seenSince = -1L;
    }

    void used(long followUpAt) {
        this.phase = Phase.USED;
        this.figure = null;
        this.followUpAt = followUpAt;
    }

    /**
     * Tells whether the follow-up is due, and clears it if so.
     *
     * @param now the current time in milliseconds
     * @return {@code true} once, when the follow-up is due
     */
    boolean followUpDue(long now) {
        if (this.followUpAt < 0 || now < this.followUpAt) return false;
        this.followUpAt = -1L;
        return true;
    }
}
