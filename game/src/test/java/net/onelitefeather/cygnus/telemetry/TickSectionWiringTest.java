package net.onelitefeather.cygnus.telemetry;

import io.opentelemetry.sdk.trace.data.SpanData;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.TelemetryConfig;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.phase.task.LobbyTimeTransitionTask;
import net.onelitefeather.cygnus.phase.task.LobbyWaitingTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that the lobby's real tasks report to {@link TickSections} when the scheduler runs them.
 * The clock moves a millisecond with every read, so one run is exactly one millisecond of clock
 * and nothing depends on the machine.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
@ExtendWith(MicrotusExtension.class)
class TickSectionWiringTest {

    private static final long MILLI = 1_000_000L;

    private TestTelemetry telemetry;
    private TickSections sections;
    private SlowTickTracer tracer;

    @BeforeEach
    void setUp() {
        this.telemetry = new TestTelemetry();
        AtomicLong clock = new AtomicLong();
        this.sections = TickSections.measuring(() -> clock.addAndGet(MILLI));
        this.tracer = new SlowTickTracer(this.telemetry.tracing(), new TelemetryConfig(50),
                Clock.fixed(Instant.parse("2026-01-01T00:00:10Z"), ZoneOffset.UTC), this.sections);
    }

    @AfterEach
    void tearDown() {
        this.telemetry.close();
    }

    private SpanData section(String name) {
        return this.telemetry.spans().stream()
                .filter(span -> span.getName().equals(CygnusAttributes.SPAN_TICK_SECTION))
                .filter(span -> name.equals(span.getAttributes().get(CygnusAttributes.TICK_SECTION_NAME)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no section '" + name + "' among " + this.telemetry.spans()));
    }

    @Test
    @DisplayName("The lobby's waiting display shows up in a slow tick")
    void waitingDisplayIsMeasured(Env env) {
        LobbyWaitingTask display = new LobbyWaitingTask(2, this.sections);

        env.tick();
        this.tracer.onTick(80.0D, 0.0D);
        display.stop();

        assertEquals(1.0D, section(TickSectionNames.LOBBY_WAITING)
                .getAttributes().get(CygnusAttributes.TICK_SECTION_DURATION_MS));
    }

    @Test
    @DisplayName("The lobby's time transition shows up in a slow tick")
    void timeTransitionIsMeasured(Env env) {
        Instance instance = env.createFlatInstance();
        LobbyTimeTransitionTask task = new LobbyTimeTransitionTask(() -> instance, Helper.MIDNIGHT_TIME, 1, this.sections);
        task.start();

        env.tick();
        this.tracer.onTick(80.0D, 0.0D);
        task.stop();

        assertEquals(1.0D, section(TickSectionNames.LOBBY_TIME)
                .getAttributes().get(CygnusAttributes.TICK_SECTION_DURATION_MS));
        env.destroyInstance(instance, true);
    }
}
