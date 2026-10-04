package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One span with a guaranteed end, used with try-with-resources.
 * <p>
 * Cygnus never makes a span <em>current</em>: the work a span covers is spread over the tick thread,
 * the scheduler and the configuration threads, so a thread-local context would attach children to the
 * wrong parent or leak. Parents are passed explicitly instead, through {@link #child(String)}.
 * </p>
 * <p>
 * Ending is idempotent. A step that failed is marked {@code ERROR} and carries the exception, so the
 * code that creates it can stay a plain {@code try (TraceStep step = ...)} plus one {@code catch}.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TraceStep implements AutoCloseable {

    private final Tracer tracer;
    private final Span span;
    private final AtomicBoolean ended = new AtomicBoolean(false);

    private TraceStep(Tracer tracer, Span span) {
        this.tracer = tracer;
        this.span = span;
    }

    /**
     * Starts a step without a parent, which makes it the root of its own trace.
     *
     * @param tracer the tracer to create the span with
     * @param name   the span name
     * @return the started step
     */
    static TraceStep root(Tracer tracer, String name) {
        SpanBuilder builder = tracer.spanBuilder(name).setNoParent();
        return new TraceStep(tracer, builder.startSpan());
    }

    /**
     * Starts a step below the given context, or a root when there is none.
     *
     * @param tracer the tracer to create the span with
     * @param name   the span name
     * @param parent the context of the parent span, or {@code null} for a root
     * @return the started step
     */
    static TraceStep start(Tracer tracer, String name, @Nullable Context parent) {
        if (parent == null) {
            return root(tracer, name);
        }
        return new TraceStep(tracer, tracer.spanBuilder(name).setParent(parent).startSpan());
    }

    /**
     * Starts a step below this one.
     *
     * @param name the span name
     * @return the started child
     */
    public TraceStep child(String name) {
        Span child = this.tracer.spanBuilder(name)
                .setParent(Context.root().with(this.span))
                .startSpan();
        return new TraceStep(this.tracer, child);
    }

    /**
     * Sets an attribute on the span.
     *
     * @param key   the attribute key
     * @param value the value
     * @param <T>   the value type
     * @return this step
     */
    public <T> TraceStep set(AttributeKey<T> key, T value) {
        this.span.setAttribute(key, value);
        return this;
    }

    /**
     * Marks the step as failed. The step still has to be closed.
     *
     * @param throwable what went wrong
     */
    public void fail(Throwable throwable) {
        this.span.recordException(throwable);
        this.span.setStatus(StatusCode.ERROR, String.valueOf(throwable.getMessage()));
    }

    /**
     * Returns the underlying span, for the rare caller that needs to add an event.
     *
     * @return the span
     */
    public Span span() {
        return this.span;
    }

    /**
     * Ends the span. Does nothing when it already ended.
     */
    @Override
    public void close() {
        if (this.ended.compareAndSet(false, true)) {
            this.span.end();
        }
    }
}
