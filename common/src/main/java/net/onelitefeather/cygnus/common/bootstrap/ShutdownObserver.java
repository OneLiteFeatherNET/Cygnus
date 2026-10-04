package net.onelitefeather.cygnus.common.bootstrap;

import org.jetbrains.annotations.Nullable;

/**
 * Watches the one shutdown {@link ServiceShutdown} runs, without {@code common} having to know what
 * the watcher does with it.
 * <p>
 * It exists for tracing: {@code :common} carries no OpenTelemetry dependency, and the game module
 * turns these two calls into a span. Both run on the shutdown path, so an implementation must be
 * quick and must not throw - {@link ServiceShutdown} shields the exit from it either way.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public interface ShutdownObserver {

    /**
     * An observer that observes nothing.
     */
    ShutdownObserver NONE = new ShutdownObserver() {
    };

    /**
     * The shutdown was requested. Called once, on the thread that asked for it.
     */
    default void requested() {
    }

    /**
     * The server stopped, successfully or not. Called once on the shutdown thread, and strictly
     * before the JVM is told to exit: whatever is still open after this call may never reach an
     * exporter, because the exit is what triggers the final flush.
     *
     * @param failure what stopping the server threw, or {@code null} when it stopped cleanly
     */
    default void serverStopped(@Nullable Throwable failure) {
    }
}
