package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
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
 * <h2>Ordering against the telemetry flush</h2>
 * <p>
 * A {@link ShutdownObserver} sees the shutdown before and after the server stopped, and the
 * {@code serverStopped} call is made before {@link Runtime#exit(int)}. That order is the one
 * guarantee tracing needs: the OpenTelemetry javaagent flushes its span processor from a JVM
 * shutdown hook, and that hook only runs once the exit started - a span ended before it is
 * flushed, a span ended after is lost. The watchdog's {@link Runtime#halt(int)} skips the hooks,
 * so on that path the shutdown span is lost along with the tail of every other export.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.1.0
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
    private volatile ShutdownObserver observer = ShutdownObserver.NONE;

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
     * Hands the shutdown to an observer, replacing the previous one.
     * <p>
     * Static because the shutdown is: there is one process to end. The default does nothing.
     * </p>
     *
     * @param observer who is told about the shutdown
     */
    public static void observe(@NotNull ShutdownObserver observer) {
        INSTANCE.observer = observer;
    }

    /**
     * Replaces the observer of this instance. Package-private for tests, which never touch the
     * shared instance.
     *
     * @param observer who is told about the shutdown
     */
    void setObserver(@NotNull ShutdownObserver observer) {
        this.observer = observer;
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
        // After the watchdog, before the shutdown thread: whatever the observer throws is caught, and
        // the thread cannot report the stop before the observer heard about the request.
        startWatchdog();
        notifyObserver(ShutdownObserver::requested);
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
        Throwable failure = null;
        try {
            stopServer.run();
        } catch (RuntimeException | Error exception) {
            failure = exception;
            LOGGER.error("Stopping the server failed - exiting anyway", exception);
        }
        // Before the exit on purpose, see the class comment: the exit is what flushes the exporter.
        Throwable stopFailure = failure;
        notifyObserver(watcher -> watcher.serverStopped(stopFailure));
        LOGGER.info("Exiting the process");
        exit.accept(EXIT_CODE);
    }

    /**
     * Calls the observer without letting it hold up the shutdown: a broken tracer must not be the
     * reason the service never exits.
     */
    private void notifyObserver(Consumer<ShutdownObserver> call) {
        try {
            call.accept(observer);
        } catch (Throwable exception) {
            LOGGER.warn("The shutdown observer failed - continuing the shutdown", exception);
        }
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
