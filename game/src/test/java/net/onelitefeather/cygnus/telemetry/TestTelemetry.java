package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.util.List;

/**
 * A private OpenTelemetry SDK that keeps every finished span in memory.
 * <p>
 * Every test builds its own instance and never registers it globally, so tests stay independent of
 * each other and of the order they run in. {@link SimpleSpanProcessor} exports synchronously, which
 * means a span is visible here the moment it ended - nothing to wait for.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TestTelemetry implements AutoCloseable {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(provider).build();
    private final CygnusTracing tracing = CygnusTracing.of(sdk.getTracer(CygnusAttributes.INSTRUMENTATION_NAME));

    public CygnusTracing tracing() {
        return this.tracing;
    }

    /**
     * All spans that ended so far, in the order they ended.
     */
    public List<SpanData> spans() {
        return this.exporter.getFinishedSpanItems();
    }

    /**
     * The one finished span with the given name.
     *
     * @throws AssertionError if there is none, or more than one
     */
    public SpanData span(String name) {
        List<SpanData> matches = spans().stream().filter(span -> span.getName().equals(name)).toList();
        if (matches.size() != 1) {
            throw new AssertionError("Expected exactly one finished span '" + name + "' but found "
                    + matches.size() + " among " + spans().stream().map(SpanData::getName).toList());
        }
        return matches.getFirst();
    }

    /**
     * The first event with the given name on a span.
     */
    public static EventData event(SpanData span, String name) {
        return span.getEvents().stream()
                .filter(event -> event.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No event '" + name + "' on span '" + span.getName()
                        + "', it has " + span.getEvents().stream().map(EventData::getName).toList()));
    }

    @Override
    public void close() {
        this.sdk.close();
    }
}
