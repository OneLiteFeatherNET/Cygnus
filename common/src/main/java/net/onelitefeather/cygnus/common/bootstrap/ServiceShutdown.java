package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

/**
 * The single way Cygnus ends its own process.
 * <p>
 * Every caller that wants the service gone - the {@link StopCommand} CloudNet reaches through its
 * stdin {@code stop} line, and the restart phase at the end of a round - goes through here, so a
 * finished round and an operator stopping the server leave the process in the same state: exited.
 * </p>
 *
 * <h2>Why {@code MinecraftServer.stopCleanly()} alone is not enough</h2>
 * <p>
 * {@code stopCleanly()} only tears down what Minestom itself owns. Libraries bootstrapped next to
 * it keep their own threads, and at least one of them is a non-daemon thread that outlives the
 * server: LuckPerms runs its housekeeping on {@code luckperms-scheduler}, created through
 * {@code Executors.defaultThreadFactory()} - which explicitly clears the daemon flag - and shut
 * down only from the JVM shutdown hook {@code MinestomLoader#registerShutdownHook()} installs. A
 * JVM shutdown hook runs when the JVM starts exiting, and the JVM starts exiting when the last
 * non-daemon thread ends. That is a cycle: the thread waits for the hook, the hook waits for the
 * thread, and the process lingers. CloudNet then keeps reporting the service as running until its
 * own timeout kills it. Calling {@link System#exit(int)} breaks the cycle by starting the shutdown
 * from the outside.
 * </p>
 *
 * <h2>Why a watchdog</h2>
 * <p>
 * {@link System#exit(int)} hands control to the shutdown hooks, and a hook that blocks - a
 * storage backend waiting on a socket, say - hangs the exit just as thoroughly as a stray thread
 * would. A daemon watchdog started alongside the shutdown gives the orderly path
 * {@value #WATCHDOG_TIMEOUT_SECONDS} seconds and then calls {@link Runtime#halt(int)}, which skips
 * the hooks and ends the process. Losing the tail of a flush beats never handing the service slot
 * back.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.11.0
 **/
public final class ServiceShutdown {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceShutdown.class);

    /**
     * How long the orderly shutdown may take before the watchdog halts the JVM.
     */
    static final long WATCHDOG_TIMEOUT_SECONDS = 30;

    static final String SHUTDOWN_THREAD_NAME = "cygnus-shutdown";
    static final String WATCHDOG_THREAD_NAME = "cygnus-shutdown-watchdog";
    static final int EXIT_CODE = 0;

    private static final ServiceShutdown INSTANCE = new ServiceShutdown(
            MinecraftServer::stopCleanly,
            status -> Runtime.getRuntime().exit(status),
            status -> Runtime.getRuntime().halt(status),
            Duration.ofSeconds(WATCHDOG_TIMEOUT_SECONDS)
    );

    private final Runnable stopServer;
    private final IntConsumer exit;
    private final IntConsumer halt;
    private final Duration watchdogTimeout;
    private final AtomicBoolean requested = new AtomicBoolean(false);

    /**
     * Creates a shutdown with explicit collaborators. Package-private so tests can drive it without
     * ending the test JVM; production code uses {@link #request()}.
     *
     * @param stopServer      what stops the server, normally {@code MinecraftServer::stopCleanly}
     * @param exit            what starts the JVM shutdown, normally {@link Runtime#exit(int)}
     * @param halt            what ends the JVM without running hooks, normally {@link Runtime#halt(int)}
     * @param watchdogTimeout how long the orderly path gets before {@code halt} is used
     */
    ServiceShutdown(@NotNull Runnable stopServer, @NotNull IntConsumer exit, @NotNull IntConsumer halt,
                    @NotNull Duration watchdogTimeout) {
        this.stopServer = stopServer;
        this.exit = exit;
        this.halt = halt;
        this.watchdogTimeout = watchdogTimeout;
    }

    /**
     * Stops the server and ends the process.
     * <p>
     * Returns immediately: the work happens on a separate thread, because
     * {@code MinecraftServer.stopCleanly()} tears down the very threads the callers run on - the
     * console reader feeding CloudNet's {@code stop} line into the command manager, and the tick
     * thread the restart phase finishes on. Repeated calls do nothing, so a round ending while an
     * operator types {@code /stop} still shuts the service down exactly once.
     * </p>
     *
     * @return {@code true} if this call started the shutdown, {@code false} if one was already
     * running
     */
    public static boolean request() {
        return INSTANCE.requestShutdown();
    }

    /**
     * Starts the shutdown on this instance.
     *
     * @return {@code true} if this call started the shutdown, {@code false} if one was already
     * running
     */
    boolean requestShutdown() {
        if (!requested.compareAndSet(false, true)) {
            LOGGER.debug("Shutdown already in progress - ignoring the additional request");
            return false;
        }
        startWatchdog();
        Thread.ofPlatform().name(SHUTDOWN_THREAD_NAME).start(this::runShutdown);
        return true;
    }

    /**
     * Stops the server and then starts the JVM shutdown.
     * <p>
     * A server that fails to stop must not keep the process alive, so the exit happens either way -
     * everything Minestom did not manage to release is torn down by the JVM anyway.
     * </p>
     */
    private void runShutdown() {
        try {
            stopServer.run();
        } catch (RuntimeException | Error exception) {
            LOGGER.error("Stopping the server failed - exiting anyway", exception);
        }
        LOGGER.info("Exiting the process");
        exit.accept(EXIT_CODE);
    }

    /**
     * Starts the daemon thread that halts the JVM if the orderly shutdown does not finish in time.
     * <p>
     * Daemon on purpose: it must never be the reason the process stays alive, and daemon threads
     * keep running while the shutdown hooks do, which is exactly the window it has to cover.
     * </p>
     */
    private void startWatchdog() {
        Thread.ofPlatform().name(WATCHDOG_THREAD_NAME).daemon().start(() -> {
            try {
                Thread.sleep(watchdogTimeout);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return;
            }
            LOGGER.warn("The service did not exit within {} - halting the JVM. Something is still "
                    + "holding the process; check the shutdown hooks and any non-daemon thread.", watchdogTimeout);
            halt.accept(EXIT_CODE);
        });
    }
}
