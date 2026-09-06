package net.onelitefeather.cygnus.monitoring;

import io.sentry.Sentry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up Sentry error reporting, but only when a DSN says where to report to.
 * <p>
 * The DSN comes from the {@code sentryDsn} entry of the game configuration. Without it there is no
 * project to report to, so no client is created at all and every {@code Sentry} call stays a no-op.
 * That is the expected state for local runs and tests, and it keeps a service that was never meant
 * to report from opening a connection to anywhere.
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

    private SentrySupport() {
    }

    /**
     * Initialises Sentry if a DSN is configured.
     *
     * @param dsn the DSN of the project to report to, or {@code null} to keep reporting off
     * @return {@code true} if Sentry was initialised, {@code false} if it stays off - either
     * because no DSN was given or because Sentry rejected the one it got
     */
    public static boolean init(@Nullable String dsn) {
        if (dsn == null || dsn.isBlank()) {
            LOGGER.info("No Sentry DSN configured - error reporting stays off");
            return false;
        }
        try {
            Sentry.init(options -> options.setDsn(dsn));
        } catch (IllegalArgumentException exception) {
            // Sentry parses the DSN itself and rejects one it cannot make sense of. A typo in the
            // config is not worth taking the service down for - it starts without reporting.
            LOGGER.warn("The configured Sentry DSN was rejected - error reporting stays off", exception);
            return false;
        }
        LOGGER.info("Sentry error reporting enabled");
        return true;
    }
}
