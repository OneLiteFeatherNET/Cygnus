package net.onelitefeather.cygnus.telemetry;

import net.theevilreaper.xerus.api.phase.LinearPhaseSeries;
import net.theevilreaper.xerus.api.phase.Phase;

/**
 * A {@link LinearPhaseSeries} that reports its lifecycle to a {@link RoundTracer}.
 * <p>
 * The series is the one place every phase passes through, so hooking it traces all of them without
 * a line of tracing in {@code LobbyPhase}, {@code GamePhase} and the rest: the series starting is the
 * round starting, a phase becoming current is a phase span opening, and the series finishing - which
 * only the last phase's callback can cause - is the round ending.
 * </p>
 *
 * @param <T> the phase type of the series
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TracedPhaseSeries<T extends Phase> extends LinearPhaseSeries<T> {

    private final RoundTracer rounds;

    /**
     * Creates the series.
     *
     * @param name   the name of the series
     * @param rounds receives the lifecycle of the series
     */
    public TracedPhaseSeries(String name, RoundTracer rounds) {
        super(name);
        this.rounds = rounds;
    }

    /**
     * Opens the round, then starts the first phase.
     */
    @Override
    public void onStart() {
        this.rounds.roundStarted();
        super.onStart();
    }

    /**
     * Ends the phase that just finished before the series moves on - unless the series is paused,
     * in which case {@code advance} does nothing and the phase is still the current one.
     */
    @Override
    public void advance() {
        if (isRunning() && !isPaused()) {
            this.rounds.phaseEnded();
        }
        super.advance();
    }

    /**
     * Opens the span of the phase that becomes current. A phase that throws while starting gets its
     * span ended with the exception on it, because nothing else would ever end it.
     */
    @Override
    public void startCurrentPhase() {
        this.rounds.phaseStarted(this.currentPhase.getName());
        try {
            super.startCurrentPhase();
        } catch (RuntimeException | Error throwable) {
            this.rounds.phaseFailed(throwable);
            throw throwable;
        }
    }

    /**
     * Ends the round once the last phase is done.
     */
    @Override
    public void finish() {
        try {
            super.finish();
        } finally {
            this.rounds.roundEnded();
        }
    }
}
