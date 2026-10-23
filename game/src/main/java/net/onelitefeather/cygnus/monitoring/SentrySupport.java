package net.onelitefeather.cygnus.monitoring;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import net.minestom.server.exception.ExceptionHandler;
import net.minestom.server.exception.ExceptionManager;
import net.onelitefeather.cygnus.common.bootstrap.ServiceName;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Sets up Sentry error reporting, but only when a DSN says where to report to.
 * <p>
 * The DSN comes from the {@code sentryDsn} entry of the game configuration. Without it there is no
 * project to report to, so no client is created at all and every {@code Sentry} call stays a no-op.
 * That is the expected state for local runs and tests, and it keeps a service that was never meant
 * to report from opening a connection to anywhere.
 * </p>
 * <p>
 * Minestom catches the exceptions of listeners, ticks and packet handling and only hands them to its
 * {@link ExceptionManager}, so they never reach Sentry on their own. With a DSN the manager's handler
 * is wrapped to forward every throwable first and then still pass it on to the handler that was there
 * before, so nothing disappears from the logs. Every event carries the release and the CloudNet
 * service it came from.
 * </p>
 * <p>
 * Sentry installs its own shutdown hook to flush pending events, so nothing has to be closed here.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.11.0
 */
public final class SentrySupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySupport.class);
    private static final String RELEASE_PREFIX = "cygnus-game@";
    private static final String UNKNOWN_VERSION = "unknown";

    private SentrySupport() {
    }

    /**
     * Initialises Sentry if a DSN is configured and makes Minestom report its caught exceptions.
     *
     * @param dsn              the DSN of the project to report to, or {@code null} to keep reporting off
     * @param version          the version of this build, used as the release; {@code unknown} sets none
     * @param serviceName      the name of the CloudNet service, or {@code null} to keep Sentry's default
     * @param exceptionManager the manager whose handler forwards to Sentry once reporting is on
     * @return {@code true} if Sentry was initialised, {@code false} if it stays off - either
     * because no DSN was given or because Sentry rejected the one it got
     * @since 2.18.3
     */
    public static boolean init(@Nullable String dsn, String version, @Nullable String serviceName,
                               ExceptionManager exceptionManager) {
        if (dsn == null || dsn.isBlank()) {
            LOGGER.info("No Sentry DSN configured - error reporting stays off");
            return false;
        }
        try {
            Sentry.init(options -> configure(options, dsn, version, serviceName));
        } catch (IllegalArgumentException exception) {
            // Sentry parses the DSN itself and rejects one it cannot make sense of. A typo in the
            // config is not worth taking the service down for - it starts without reporting.
            LOGGER.warn("The configured Sentry DSN was rejected - error reporting stays off", exception);
            return false;
        }
        exceptionManager.setExceptionHandler(
                forwarding(exceptionManager.getExceptionHandler(), Sentry::captureException));
        LOGGER.info("Sentry error reporting enabled");
        return true;
    }

    /**
     * Fills the Sentry options.
     *
     * @param options     the options to fill
     * @param dsn         the DSN of the project to report to
     * @param version     the version of this build; {@code unknown} sets no release
     * @param serviceName the name of the CloudNet service, or {@code null} to keep Sentry's default
     * @since 2.18.3
     */
    static void configure(SentryOptions options, String dsn, String version, @Nullable String serviceName) {
        options.setDsn(dsn);
        if (!UNKNOWN_VERSION.equals(version)) {
            options.setRelease(RELEASE_PREFIX + version);
        }
        if (serviceName != null) {
            options.setServerName(serviceName);
        }
    }

    /**
     * Wraps an exception handler so every throwable is also handed to {@code capture}.
     *
     * @param delegate the handler that keeps logging the throwable, as before
     * @param capture  receives the throwable, normally {@code Sentry::captureException}
     * @return the combined handler
     * @since 2.18.3
     */
    static ExceptionHandler forwarding(ExceptionHandler delegate, Consumer<Throwable> capture) {
        return throwable -> {
            try {
                capture.accept(throwable);
            } catch (RuntimeException failure) {
                // Reporting must never be the reason an error is not logged.
                throwable.addSuppressed(failure);
            }
            delegate.handleException(throwable);
        };
    }

    /**
     * Derives the CloudNet service name from the service directory.
     * <p>
     * CloudNet starts a temporary service in {@code temp/services/<name>_<unique id>} and does not hand
     * the name to the JVM in any system property, so the directory is the one place it shows up.
     * </p>
     *
     * @param workingDirectory the service working directory
     * @return the service name, or {@code null} when the directory does not look like one of CloudNet's
     * (standalone runs, static services)
     * @since 2.18.3
     */
    public static @Nullable String serviceName(Path workingDirectory) {
        return ServiceName.of(workingDirectory);
    }
}
