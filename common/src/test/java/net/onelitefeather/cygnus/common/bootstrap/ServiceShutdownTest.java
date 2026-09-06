package net.onelitefeather.cygnus.common.bootstrap;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the shutdown path every stop request goes through.
 * <p>
 * All of these drive a {@link ServiceShutdown} built with fakes instead of the shared instance -
 * the real one exits the JVM, which would take the test run with it.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.11.0
 **/
class ServiceShutdownTest {

    private static final Duration NEVER = Duration.ofDays(1);
    private static final long AWAIT_SECONDS = 10;

    @Test
    void testServerIsStoppedAndTheProcessExits() throws InterruptedException {
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger stops = new AtomicInteger();
        AtomicInteger exitCode = new AtomicInteger(-1);

        ServiceShutdown shutdown = new ServiceShutdown(
                stops::incrementAndGet,
                status -> {
                    exitCode.set(status);
                    exited.countDown();
                },
                _ -> fail("the watchdog must not fire while the shutdown makes progress"),
                NEVER
        );

        assertTrue(shutdown.requestShutdown(), "the first request starts the shutdown");
        assertTrue(exited.await(AWAIT_SECONDS, TimeUnit.SECONDS), "the process has to exit");
        assertEquals(1, stops.get(), "the server is stopped exactly once");
        assertEquals(ServiceShutdown.EXIT_CODE, exitCode.get());
    }

    @Test
    void testTheProcessExitsEvenWhenStoppingTheServerFails() throws InterruptedException {
        CountDownLatch exited = new CountDownLatch(1);

        ServiceShutdown shutdown = new ServiceShutdown(
                () -> {
                    throw new IllegalStateException("the server refuses to stop");
                },
                _ -> exited.countDown(),
                _ -> fail("the watchdog must not fire while the shutdown makes progress"),
                NEVER
        );

        shutdown.requestShutdown();

        assertTrue(exited.await(AWAIT_SECONDS, TimeUnit.SECONDS),
                "a server that fails to stop must not keep the process alive");
    }

    @Test
    void testASecondRequestIsIgnored() throws InterruptedException {
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger stops = new AtomicInteger();

        ServiceShutdown shutdown = new ServiceShutdown(
                stops::incrementAndGet,
                _ -> exited.countDown(),
                _ -> fail("the watchdog must not fire while the shutdown makes progress"),
                NEVER
        );

        assertTrue(shutdown.requestShutdown());
        assertTrue(exited.await(AWAIT_SECONDS, TimeUnit.SECONDS));

        assertFalse(shutdown.requestShutdown(), "a round ending and a /stop must not stop twice");
        assertEquals(1, stops.get());
    }

    @Test
    void testTheWatchdogHaltsAJvmThatRefusesToExit() throws InterruptedException {
        CountDownLatch halted = new CountDownLatch(1);
        CountDownLatch releaseExit = new CountDownLatch(1);
        AtomicInteger haltCode = new AtomicInteger(-1);

        ServiceShutdown shutdown = new ServiceShutdown(
                () -> {
                },
                // Stands in for a shutdown hook that blocks - System.exit never returns then, and
                // without the watchdog the service would hang exactly there.
                _ -> awaitQuietly(releaseExit),
                status -> {
                    haltCode.set(status);
                    halted.countDown();
                },
                Duration.ofMillis(50)
        );

        try {
            shutdown.requestShutdown();

            assertTrue(halted.await(AWAIT_SECONDS, TimeUnit.SECONDS), "the watchdog has to halt the JVM");
            assertEquals(ServiceShutdown.EXIT_CODE, haltCode.get());
        } finally {
            // The fake exit blocks a non-daemon thread; leaving it parked would outlive the test.
            releaseExit.countDown();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
