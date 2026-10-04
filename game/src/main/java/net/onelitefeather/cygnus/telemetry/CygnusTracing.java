package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.jetbrains.annotations.Nullable;

/**
 * The tracer of a Cygnus service, handed to everything that creates spans.
 * <p>
 * Services never look at {@link GlobalOpenTelemetry} themselves: the composition root builds one of
 * these through {@link #fromGlobal(String)} and passes it down. That keeps the global singleton out
 * of the logic - a test hands in a tracer of its own private SDK and never registers anything
 * globally.
 * </p>
 * <p>
 * Without the OpenTelemetry javaagent attached, {@code GlobalOpenTelemetry} is a no-op and so is every
 * span created here.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CygnusTracing {

    private final Tracer tracer;

    private CygnusTracing(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Builds the tracing from the global OpenTelemetry instance. Called once, by the composition root.
     *
     * @param version the version of the running service, reported with the instrumentation scope
     * @return the tracing
     */
    public static CygnusTracing fromGlobal(String version) {
        return new CygnusTracing(GlobalOpenTelemetry.get()
                .getTracer(CygnusAttributes.INSTRUMENTATION_NAME, version));
    }

    /**
     * Wraps a tracer, for tests and for callers that already hold one.
     *
     * @param tracer the tracer
     * @return the tracing
     */
    public static CygnusTracing of(Tracer tracer) {
        return new CygnusTracing(tracer);
    }

    /**
     * A tracing whose spans go nowhere.
     *
     * @return the no-op tracing
     */
    public static CygnusTracing noop() {
        return new CygnusTracing(OpenTelemetry.noop().getTracer(CygnusAttributes.INSTRUMENTATION_NAME));
    }

    /**
     * Returns the tracer.
     *
     * @return the tracer
     */
    public Tracer tracer() {
        return this.tracer;
    }

    /**
     * Starts a step that is the root of its own trace.
     *
     * @param name the span name
     * @return the started step
     */
    public TraceStep root(String name) {
        return TraceStep.root(this.tracer, name);
    }

    /**
     * Starts a step below the given context. Without one it is the root of its own trace.
     *
     * @param name   the span name
     * @param parent the context of the parent span, or {@code null}
     * @return the started step
     */
    public TraceStep step(String name, @Nullable Context parent) {
        return TraceStep.start(this.tracer, name, parent);
    }
}
