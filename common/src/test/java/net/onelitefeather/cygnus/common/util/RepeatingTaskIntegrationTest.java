package net.onelitefeather.cygnus.common.util;

import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link RepeatingTask} owns exactly one scheduler task no matter how many times
 * start and stop are called.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
@ExtendWith(MicrotusExtension.class)
class RepeatingTaskIntegrationTest {

    @Test
    void notRunningBeforeStart() {
        RepeatingTask task = new RepeatingTask(() -> {
        });

        assertFalse(task.isRunning());
    }

    @Test
    void runsOnceStarted(Env env) {
        AtomicInteger ticks = new AtomicInteger();
        RepeatingTask task = new RepeatingTask(ticks::incrementAndGet);

        task.start(50, ChronoUnit.MILLIS);
        assertTrue(task.isRunning());
        for (int i = 0; i < 10; i++) {
            env.tick();
        }

        assertTrue(ticks.get() > 0, "the action should have run at least once by now");
    }

    @Test
    void startIsIdempotent(Env env) {
        AtomicInteger ticks = new AtomicInteger();
        RepeatingTask task = new RepeatingTask(ticks::incrementAndGet);

        task.start(50, ChronoUnit.MILLIS);
        task.start(50, ChronoUnit.MILLIS);
        for (int i = 0; i < 10; i++) {
            env.tick();
        }
        int afterFirstBatch = ticks.get();

        // Stopping cancels the single task this class is meant to own. If start() had scheduled a
        // second task on the repeated call, stop() would only ever reach one of them and the other
        // would keep running forever, still incrementing the counter below.
        task.stop();
        for (int i = 0; i < 10; i++) {
            env.tick();
        }

        assertEquals(afterFirstBatch, ticks.get(), "a leaked second task would still be ticking");
    }

    @Test
    void stopStopsTheTask(Env env) {
        AtomicInteger ticks = new AtomicInteger();
        RepeatingTask task = new RepeatingTask(ticks::incrementAndGet);
        task.start(50, ChronoUnit.MILLIS);
        for (int i = 0; i < 10; i++) {
            env.tick();
        }

        task.stop();
        assertFalse(task.isRunning());
        int afterStop = ticks.get();
        for (int i = 0; i < 10; i++) {
            env.tick();
        }

        assertEquals(afterStop, ticks.get(), "no more runs should happen after stop()");
    }

    @Test
    void stopIsIdempotent() {
        RepeatingTask task = new RepeatingTask(() -> {
        });

        task.stop();

        assertFalse(task.isRunning(), "stopping a task that never ran must not throw");
    }
}
