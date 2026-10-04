package net.onelitefeather.cygnus.telemetry;

import net.onelitefeather.cygnus.common.bootstrap.ShutdownObserver;
import org.jetbrains.annotations.Nullable;

/**
 * Turns the shutdown of the service into a span.
 * <p>
 * The span runs from the request to the moment the server stopped, and it is ended <em>before</em>
 * {@code ServiceShutdown} tells the JVM to exit. That order matters: the javaagent flushes its
 * exporter from a JVM shutdown hook, which only runs once the exit started, so a span ended earlier
 * is part of that flush and a span ended later is lost. The watchdog's {@code Runtime.halt} skips the
 * hooks, so a shutdown that hangs loses this span too - there is nothing to be done about that from
 * in here.
 * </p>
 * <p>
 * A round still running when the service is stopped is ended as well, with the end reason
 * {@code shutdown}; otherwise a stop in the middle of a round would leave the whole round trace
 * without its root.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class ShutdownTracer implements ShutdownObserver {

    /** The end reason of a round the shutdown cut short. */
    public static final String END_REASON_SHUTDOWN = "shutdown";

    private final CygnusTracing tracing;
    private final RoundTracer rounds;
    private volatile @Nullable TraceStep step;

    /**
     * Creates the tracer.
     *
     * @param tracing where the spans go
     * @param rounds  the round to end if one is running
     */
    public ShutdownTracer(CygnusTracing tracing, RoundTracer rounds) {
        this.tracing = tracing;
        this.rounds = rounds;
    }

    @Override
    public void requested() {
        this.step = this.tracing.root(CygnusAttributes.SPAN_SHUTDOWN);
    }

    @Override
    public void serverStopped(@Nullable Throwable failure) {
        this.rounds.abort(END_REASON_SHUTDOWN);
        TraceStep current = this.step;
        this.step = null;
        if (current == null) {
            return;
        }
        if (failure != null) {
            current.fail(failure);
        }
        current.close();
    }
}
